package org.geogit.storage.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectWriter;
import org.opengis.feature.simple.SimpleFeatureType;

public class SimpleFeatureTypeSerialisationTest extends SerialisationTestCase {

    public void testSimpleFeatureTypeRoundTripping() throws Exception {
        ObjectWriter<SimpleFeatureType> writer = new HessianSimpleFeatureTypeWriter(featureType1);
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(output);
        
        byte[] data = output.toByteArray();
        assertTrue(data.length > 0);
        
        ObjectReader<SimpleFeatureType> reader = new HessianSimpleFeatureTypeReader(typeName1);
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        SimpleFeatureType type = reader.read(null, input);
        
        assertNotNull(type);
        assertEquals(featureType1, type);
    }
}
