package au.com.permeance.liferay.spring;

import com.liferay.portal.kernel.bean.BeanLocator;
import com.liferay.portal.kernel.bean.BeanLocatorException;

import java.io.IOException;
import java.util.List;

import org.mockito.Mock;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.util.Collections.singleton;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class BeanLocatorDefinitionCopierTest {

    @Mock
    private BeanLocator beanLocator;

    @Mock
    private ConfigurableApplicationContext configurableApplicationContext;

    @Mock
    private MetadataReader metadataReader;

    @Mock
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Mock
    private TypeFilter typeFilter;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testAddIncludeFilter() throws Exception {
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        copier.addIncludeFilter(typeFilter);
        @SuppressWarnings("unchecked")
        final List<TypeFilter> includeFilters = (List<TypeFilter>) ReflectionTestUtils.getField(copier, "includeFilters");
        final boolean result = includeFilters.contains(typeFilter);

        assertTrue(result);
    }

    @Test
    public void testAddExcludeFilter() throws Exception {
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        copier.addExcludeFilter(typeFilter);
        @SuppressWarnings("unchecked")
        final List<TypeFilter> excludeFilters = (List<TypeFilter>) ReflectionTestUtils.getField(copier, "excludeFilters");
        final boolean result = excludeFilters.contains(typeFilter);

        assertTrue(result);
    }

    @Test
    public void testRequiredContextClass() throws Exception {
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final Class result = copier.requiredContextClass();

        assertEquals(result, ConfigurableApplicationContext.class);
    }

    @Test
    public void testInitApplicationContext() throws Exception {
        final Object bean = new Object();
        when(configurableApplicationContext.getBeanFactory()).thenReturn(configurableListableBeanFactory);
        when(beanLocator.getNames()).thenReturn(new String[]{"invalid.class.Name",
                                                             "java.lang.String",
                                                             "java.lang.Object"});
        when(beanLocator.locate("java.lang.String")).thenReturn(null);
        when(beanLocator.locate("java.lang.Object")).thenReturn(bean);

        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);
        copier.initApplicationContext(configurableApplicationContext);

        verify(configurableListableBeanFactory).registerSingleton("java.lang.Object", bean);
    }

    @Test
    public void testSafeLocateWithBeanLocatorException() throws Exception {
        when(beanLocator.locate(anyString())).thenThrow(new BeanLocatorException());
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final Object result = copier.safeLocate("test");

        assertNull(result);
    }

    @Test
    public void testSafeLocate() throws Exception {
        final Object bean = new Object();
        when(beanLocator.locate(anyString())).thenReturn(bean);
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final Object result = copier.safeLocate("test");

        assertEquals(result, bean);
    }

    @Test
    public void testIsAcceptableWithUnknownType() throws Exception {
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final boolean result = copier.isAcceptable("invalid.class.Name");

        assertFalse(result);
    }

    @Test
    public void testIsAcceptableWithExclusion() throws Exception {
        when(typeFilter.match(any(MetadataReader.class), any(MetadataReaderFactory.class))).thenReturn(true);
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);
        copier.addExcludeFilter(typeFilter);

        final boolean result = copier.isAcceptable("java.lang.Object");

        assertFalse(result);
    }

    @Test
    public void testIsAcceptableIncludeFilters() throws Exception {
        when(typeFilter.match(any(MetadataReader.class), any(MetadataReaderFactory.class))).thenReturn(true);
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);
        copier.addIncludeFilter(typeFilter);

        final boolean result = copier.isAcceptable("java.lang.Object");

        assertTrue(result);
    }

    @Test
    public void testIsAcceptableWithUnmatchedIncludeFilter() throws Exception {
        when(typeFilter.match(any(MetadataReader.class), any(MetadataReaderFactory.class))).thenReturn(false);
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);
        copier.addIncludeFilter(typeFilter);

        final boolean result = copier.isAcceptable("java.lang.Object");

        assertFalse(result);
    }

    @Test
    public void testIsAcceptable() throws Exception {
        when(typeFilter.match(any(MetadataReader.class), any(MetadataReaderFactory.class))).thenReturn(false);
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final boolean result = copier.isAcceptable("java.lang.Object");

        assertTrue(result);
    }

    @Test
    public void testMatchesWithoutMatch() throws Exception {
        when(typeFilter.match(any(MetadataReader.class), any(MetadataReaderFactory.class))).thenReturn(false);
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final boolean result = copier.matches(singleton(typeFilter), Object.class);

        assertFalse(result);
    }

    @Test
    public void testMatches() throws Exception {
        when(typeFilter.match(any(MetadataReader.class), any(MetadataReaderFactory.class))).thenReturn(true);
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final boolean result = copier.matches(singleton(typeFilter), Object.class);

        assertTrue(result);
    }

    @Test
    public void testSafeMatchWithIOException() throws Exception {
        when(typeFilter.match(any(MetadataReader.class), any(MetadataReaderFactory.class))).thenThrow(new IOException());
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final boolean result = copier.safeMatch(typeFilter, metadataReader);

        assertFalse(result);
    }

    @Test
    public void testSafeMatch() throws Exception {
        when(typeFilter.match(any(MetadataReader.class), any(MetadataReaderFactory.class))).thenReturn(true);
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final boolean result = copier.safeMatch(typeFilter, metadataReader);

        assertTrue(result);
    }

    @Test
    public void testSafeForNameWithClassNotFoundException() throws Exception {
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final Class<?> clazz = copier.safeForName("invalid.class.Name");

        assertNull(clazz);
    }

    @Test
    public void testSafeForName() throws Exception {
        final BeanLocatorDefinitionCopier copier = new BeanLocatorDefinitionCopier(beanLocator);

        final Class<?> clazz = copier.safeForName("java.lang.Object");

        assertEquals(clazz, Object.class);
    }

}
