package au.com.permeance.liferay.spring;

import org.springframework.core.io.Resource;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class SimpleMetadataReaderTest {

    @Test
    public void testGetResource() throws Exception {
        final SimpleMetadataReader reader = new SimpleMetadataReader(getClass());

        final Resource resource = reader.getResource();

        assertNull(resource);
    }

    @Test
    public void testGetClassMetadata() throws Exception {
        final SimpleMetadataReader reader = new SimpleMetadataReader(getClass());

        final StandardAnnotationMetadata metadata = reader.getClassMetadata();
        final String className = metadata.getClassName();

        assertEquals(className, SimpleMetadataReaderTest.class.getName());
    }

    @Test
    public void testGetAnnotationMetadata() throws Exception {
        final SimpleMetadataReader reader = new SimpleMetadataReader(getClass());

        final StandardAnnotationMetadata metadata = reader.getAnnotationMetadata();
        final String className = metadata.getClassName();

        assertEquals(className, SimpleMetadataReaderTest.class.getName());
    }

}
