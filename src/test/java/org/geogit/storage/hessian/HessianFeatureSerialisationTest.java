package org.geogit.storage.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


import org.geogit.api.ObjectId;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;


public class HessianFeatureSerialisationTest extends SerialisationTestCase {
    public void testSerialise() throws Exception {

        HessianFeatureWriter writer = new HessianFeatureWriter(feature1_1);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(output);

        byte[] data = output.toByteArray();
        assertTrue(data.length > 0);

        HessianFeatureReader reader = new HessianFeatureReader(featureType1, feature1_1
                .getIdentifier().getID(), null);
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        Feature feat = reader.read(ObjectId.forString(feature1_1.getIdentifier().getID()), input);

        assertNotNull(feat);
        assertTrue(feat instanceof SimpleFeature);

        assertEquals(feature1_1, feat);

    }
}
