package org.geogit.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.api.RevObject.TYPE;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.io.ParseException;

public abstract class RevFeatureSerializationTest extends Assert {
    private String namespace1 = "http://geoserver.org/test";
    private String typeName1 = "TestType";
    private String typeSpec1 = "str:String," + "bool:Boolean," + "byte:java.lang.Byte,"
                + "doub:Double," + "bdec:java.math.BigDecimal," + "flt:Float," + "int:Integer,"
                + "bint:java.math.BigInteger," + "pp:Point:srid=4326," + "lng:java.lang.Long,"
                + "uuid:java.util.UUID";
    private SimpleFeatureType featureType1;
    private Feature feature1_1;
    private ObjectSerializingFactory factory = getObjectSerializingFactory();

    protected abstract ObjectSerializingFactory getObjectSerializingFactory();

    @Before
    public void initializeFeatureAndFeatureType() throws Exception {
        /* now we will setup our feature types and test features. */
        featureType1 = DataUtilities.createType(namespace1, typeName1, typeSpec1);
        feature1_1 = feature(featureType1, "TestType.feature.1", "StringProp1_1", Boolean.TRUE,
                Byte.valueOf("18"), new Double(100.01), new BigDecimal("1.89e1021"),
                new Float(12.5), new Integer(1000), new BigInteger("90000000"), "POINT(1 1)",
                new Long(800000), UUID.fromString("bd882d24-0fe9-11e1-a736-03b3c0d0d06d"));
    }

    @Test
    public void testSerialize() throws Exception {
    
        RevFeatureBuilder builder = new RevFeatureBuilder();
        RevFeature newFeature = builder.build(feature1_1);
        ObjectWriter<RevFeature> writer = factory.<RevFeature>createObjectWriter(TYPE.FEATURE);
    
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(newFeature, output);
    
        byte[] data = output.toByteArray();
        assertTrue(data.length > 0);
    
        ObjectReader<RevFeature> reader = factory.<RevFeature>createObjectReader(TYPE.FEATURE);
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        RevFeature feat = reader
                .read(ObjectId.forString(feature1_1.getIdentifier().getID()), input);
    
        assertNotNull(feat);
        assertEquals(newFeature.getValues().size(), feat.getValues().size());
    
        for (int i = 0; i < newFeature.getValues().size(); i++) {
            assertEquals(newFeature.getValues().get(i).orNull(), feat.getValues().get(i).orNull());
        }
    
    }

    protected Feature feature(SimpleFeatureType type, String id, Object... values)
            throws ParseException {
                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
                for (int i = 0; i < values.length; i++) {
                    Object value = values[i];
                    if (type.getDescriptor(i) instanceof GeometryDescriptor) {
                        if (value instanceof String) {
                            value = new WKTReader2().read((String) value);
                        }
                    }
                    builder.set(i, value);
                }
                return builder.buildFeature(id);
            }
}
