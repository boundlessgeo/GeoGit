package org.geogit.storage.hessian;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.io.ParseException;

import junit.framework.TestCase;

public abstract class SerialisationTestCase extends TestCase {

    private String namespace1 = "http://geoserver.org/test";
    private String typeNameString1 = "TestType";
    private String typeSpec1 = "str:String," + "bool:Boolean," + "byte:java.lang.Byte,"
                + "doub:Double," + "bdec:java.math.BigDecimal," + "flt:Float," + "int:Integer,"
                + "bint:java.math.BigInteger," + "pp:Point:srid=4326," + "lng:java.lang.Long";
    protected SimpleFeatureType featureType1;
    protected Feature feature1_1;
    protected Name typeName1;
    
    public SerialisationTestCase() {
        super();
    }

    public SerialisationTestCase(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        /* now we will setup our feature types and test features. */
        typeName1 = new NameImpl(namespace1, typeNameString1);
        featureType1 = DataUtilities.createType(namespace1, typeNameString1, typeSpec1);
        feature1_1 = feature(featureType1, "TestType.feature.1", "StringProp1_1", Boolean.TRUE,
                Byte.valueOf("18"), new Double(100.01), new BigDecimal("1.89e1021"),
                new Float(12.5), new Integer(1000), new BigInteger("90000000"), "POINT(1 1)",
                new Long(800000));
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