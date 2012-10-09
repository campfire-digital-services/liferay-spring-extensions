/*
This file is part of liferay-spring-extensions.

liferay-spring-extensions is free software: you can redistribute it and/or
modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or (at your option)
any later version.

liferay-spring-extensions is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
more details.

You should have received a copy of the GNU General Public License along with
liferay-spring-extensions. If not, see <http://www.gnu.org/licenses />.
*/
package au.com.permeance.liferay.spring;

import com.liferay.portal.kernel.bean.BeanLocator;
import com.liferay.portal.kernel.bean.BeanLocatorException;
import com.liferay.portal.kernel.log.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.TypeFilter;

import static com.liferay.portal.kernel.log.LogFactoryUtil.getLog;

import static java.lang.Class.forName;
import static java.lang.String.format;

/**
 * This class provides a bean which registers existing liferay beans obtained from liferay's {@link BeanLocator} in the
 * Spring {@link ApplicationContext} in which the instance of this class is defined. Bean inclusions and exclusions can
 * be specified using the {@link #addIncludeFilter(TypeFilter)} and {@link #addExcludeFilter(TypeFilter)} methods.
 * Exclusions take priority of inclusions.
 */
public class BeanLocatorDefinitionCopier extends ApplicationObjectSupport {

    /**
     * Logger for this class.
     */
    private static final Log LOG = getLog(BeanLocatorDefinitionCopier.class);

    /**
     * Stores the list of bean exclusions.
     */
    private final transient List<TypeFilter> excludeFilters = new LinkedList<TypeFilter>();

    /**
     * Stores the list of bean inclusions.
     */
    private final transient List<TypeFilter> includeFilters = new LinkedList<TypeFilter>();

    /**
     * Stores the liferay bean locator to copy beans from.
     */
    private final transient BeanLocator beanLocator;

    /**
     * Creates a new instance based on the supplied bean locator.
     *
     * @param beanLocator the liferay bean locator to copy beans from.
     */
    public BeanLocatorDefinitionCopier(final BeanLocator beanLocator) {
        super();
        LOG.debug(format("Storing bean locator: %s", beanLocator));
        this.beanLocator = beanLocator;
    }

    /**
     * Adds an inclusion filter.
     *
     * @param filter the filter which beans must match to be copied.
     */
    public final void addIncludeFilter(final TypeFilter filter) {
        includeFilters.add(filter);
    }

    /**
     * Adds an exclusion filter.
     *
     * @param filter the filter which beans must not match to be copied.
     */
    public final void addExcludeFilter(final TypeFilter filter) {
        excludeFilters.add(filter);
    }

    /**
     * {@inheritDoc}
     *
     * @return always returns {@link ConfigurableApplicationContext}.
     */
    @Override
    protected final Class requiredContextClass() {
        return ConfigurableApplicationContext.class;
    }

    /**
     * Initialises the supplied application context based on the {@link BeanLocator} associated with this instance by
     * iterating the liferay registered beans (via {@link BeanLocator#getNames()}).
     *
     * @param context the {@link ConfigurableApplicationContext} in which beans should be registered (by calling
     *                {@link ConfigurableApplicationContext#getBeanFactory()}).
     */
    @Override
    protected final void initApplicationContext(final ApplicationContext context) {
        LOG.info(format("Copying bean definitions from %s to %s", beanLocator, context));

        final ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) context;
        @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
        final SingletonBeanRegistry singletonBeanRegistry = configurableApplicationContext.getBeanFactory();

        final String[] names = beanLocator.getNames();
        for (String name : names) {
            LOG.debug(format("Processing bean locator bean named: %s", name));

            if (!isAcceptable(name)) {
                LOG.debug(format("Skipping bean %s", name));
                continue;
            }

            final Object bean = safeLocate(name);
            if (bean == null) {
                LOG.warn(format("Skipping bean %s (bean locator couldn't acquire a valid instance)", name));
                continue;
            }

            LOG.info(format("Copying bean definition %s", name));
            singletonBeanRegistry.registerSingleton(name, bean);
        }
    }

    /**
     * Calls {@link BeanLocator#locate(String)} on the {@link #beanLocator} associated with this instance, catching and
     * logging any {@link BeanLocatorException} which is thrown.
     *
     * @param name the name of the bean to locate.
     *
     * @return the instance of the specified bean if it exists, or {@code null} if a {@link BeanLocatorException} is
     *         thrown.
     */
    protected final Object safeLocate(final String name) {
        LOG.debug(format("Fetching bean from bean locator: %s", name));
        try {
            return beanLocator.locate(name);
        }
        catch (final BeanLocatorException e) {
            LOG.warn(format("Error fetching bean from bean locator: %s - skipping", name), e);
        }
        return null;
    }

    /**
     * Determines whether the class identified by the supplied string is acceptable for copying by this instance.
     * Acceptance is based on (in order):
     * <ol>
     * <li>The string identifying a known type (if the type is unknown, it is unacceptable)</li>
     * <li>Which does not match an exclude filter (if the type is excluded, it is unacceptable)</li>
     * <li>Which matches any defined include filters (if the type is included, it is acceptable)</li>
     * <li>Or if no include filter is defined, it is acceptable</li>
     * </ol>
     *
     * @param type the type of class to match.
     *
     * @return {@code true} if the type matches the acceptance criteria for this class, {@code false} otherwise.
     */
    protected final boolean isAcceptable(final String type) {
        final Class<?> clazz = safeForName(type);

        if (clazz == null) {
            LOG.debug(format("Skipping unknown type %s", type));
            return false;
        }

        if (matches(excludeFilters, clazz)) {
            LOG.debug(format("Refusing excluded type %s", type));
            return false;
        }

        if (matches(includeFilters, clazz)) {
            LOG.debug(format("Accepting included type %s", type));
            return true;
        }

        return includeFilters.isEmpty();
    }

    /**
     * Checks whether the supplied class matches any of the supplied type filters, returning {@code true} if it does,
     * {@code false} otherwise.
     *
     * @param filters the filters to match against.
     * @param clazz   the class to match.
     *
     * @return {@code true} if any of the filters match, {@code false} otherwise.
     */
    protected final boolean matches(final Collection<? extends TypeFilter> filters, final Class<?> clazz) {
        @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
        final MetadataReader metadataReader = new SimpleMetadataReader(clazz);
        for (TypeFilter filter : filters) {
            final boolean match = safeMatch(filter, metadataReader);
            if (match) {
                return true;
            }
        }
        return false;
    }


    /**
     * Wraps a call to
     * {@link TypeFilter#match(MetadataReader, org.springframework.core.type.classreading.MetadataReaderFactory)}
     * so that any {@link IOException} is logged rather than propagating up the call stack. Returns the result of the
     * match if successful, {@code false} otherwise.
     *
     * @param filter         the filter to match against.
     * @param metadataReader the metadata reader to match with.
     *
     * @return true if
     *         {@link TypeFilter#match(MetadataReader, org.springframework.core.type.classreading.MetadataReaderFactory)}
     *         returns it, {@code false} otherwise.
     */
    protected final boolean safeMatch(final TypeFilter filter, final MetadataReader metadataReader) {
        try {
            return filter.match(metadataReader, null);
        }
        catch (final IOException e) {
            LOG.warn(format("Error checking for filter %s match against reader %s", filter, metadataReader));
        }
        return false;
    }

    /**
     * Wraps a call to {@link Class#forName(String)} so that any {@link ClassNotFoundException} is logged rather than
     * propagating back up the call stack. Returns the specified class if successful, or {@code null} otherwise.
     *
     * @param className the name of the class to resolve.
     *
     * @return the requested class, if available, or {@code null}.
     */
    protected final Class<?> safeForName(final String className) {
        try {
            return forName(className);
        }
        catch (final ClassNotFoundException e) {
            LOG.debug(format("Unknown class %s", className));
        }
        return null;
    }

}
