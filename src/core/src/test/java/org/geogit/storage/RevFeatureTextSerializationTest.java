package org.geogit.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.storage.text.TextSerializationFactory;
import org.junit.Test;
import org.opengis.feature.Feature;

public class RevFeatureTextSerializationTest extends RevFeatureSerializationTest {

    ObjectSerialisingFactory factory = new TextSerializationFactory();

    @Override
    protected ObjectSerialisingFactory getFactory() {
        return factory;
    }

    @Test
    public void testNonAsciiCharacters() throws Exception {

        Feature feature = feature(featureType1, "TestType.feature.1", "геогит", Boolean.TRUE,
                Byte.valueOf("18"), new Double(100.01), new BigDecimal("1.89e1021"),
                new Float(12.5), new Integer(1000), new BigInteger("90000000"), "POINT(1 1)",
                new Long(800000), UUID.fromString("bd882d24-0fe9-11e1-a736-03b3c0d0d06d"));

        RevFeature revFeature = new RevFeatureBuilder().build(feature);
        testFeatureReadWrite(revFeature);

    }

    @Test
    public void testMalformedSerializedObject() throws Exception {

        // a wrong value
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("id\t" + ObjectId.forString("ID_STRING") + "\n");
        writer.write(Float.class.getName() + "\tNUMBER" + "\n");
        writer.flush();

        ObjectReader<RevFeature> reader = factory.createFeatureReader();
        try {
            reader.read(ObjectId.forString("ID_STRING"),
                    new ByteArrayInputStream(out.toByteArray()));
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Wrong attribute value"));
        }

        // an unrecognized class
        out = new ByteArrayOutputStream();
        writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("id\t" + ObjectId.forString("ID_STRING") + "\n");
        writer.write(this.getClass().getName() + "\tvalue" + "\n");
        writer.flush();

        try {
            reader.read(ObjectId.forString("ID_STRING"),
                    new ByteArrayInputStream(out.toByteArray()));
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Cannot deserialize attribute. Unknown type"));
        }

    }

}
