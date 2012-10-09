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

import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;

/**
 * This class provides an implementation of the {@link MetadataReader} interface which reads from an already
 * materialised class (rather than a resource or stream).
 */
public class SimpleMetadataReader implements MetadataReader {

    /**
     * Stores the metadata for the class supplied in the constructor.
     */
    private final transient StandardAnnotationMetadata standardAnnotationMetadata;

    /**
     * Creates a new instance, based on the supplied class.
     *
     * @param clazz the class to base this instance on.
     */
    public SimpleMetadataReader(final Class<?> clazz) {
        standardAnnotationMetadata = new StandardAnnotationMetadata(clazz);
    }

    /**
     * {@inheritDoc}
     *
     * @return always returns {@code null}.
     */
    @Override
    public final Resource getResource() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return returns {@link #standardAnnotationMetadata}
     */
    @Override
    public final ClassMetadata getClassMetadata() {
        return standardAnnotationMetadata;
    }

    /**
     * {@inheritDoc}
     *
     * @return returns {@link #standardAnnotationMetadata}
     */
    @Override
    public final AnnotationMetadata getAnnotationMetadata() {
        return standardAnnotationMetadata;
    }

}
