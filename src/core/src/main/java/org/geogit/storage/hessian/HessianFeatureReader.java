/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevObject;
import org.geogit.storage.EntityType;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;
import com.vividsolutions.jts.io.InStream;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Reads features from a binary encoded stream. Refer to HessianFeatureWriter for encoding details.
 * 
 */
class HessianFeatureReader extends HessianRevReader<RevFeature> implements ObjectReader<RevFeature> {

    private static final GeometryFactory DEFAULT_GF_FACTORY = new GeometryFactory(
            new PackedCoordinateSequenceFactory());

    private final GeometryFactory geometryFactory;

    /**
     * Constructs a new {@code HessianFeatureReader} with the provided hints.
     * 
     * @param hints
     */
    public HessianFeatureReader(final Map<String, Serializable> hints) {
        GeometryFactory gf = DEFAULT_GF_FACTORY;
        if (hints != null) {
            GeometryFactory provided = (GeometryFactory) hints
                    .get(ObjectReader.JTS_GEOMETRY_FACTORY);
            if (provided != null) {
                gf = provided;
            }
        }
        this.geometryFactory = gf;
    }

    /**
     * Reads a {@link RevFeature} from the given input stream and assigns it the provided
     * {@link ObjectId id}.
     * 
     * @param id the id to use for the feature
     * @param rawData the input stream of the feature
     * @return the final feature
     * @throws IllegalArgumentException if the provided stream does not represent a
     *         {@code RevFeature}
     */
    @Override
    protected RevFeature read(ObjectId id, Hessian2Input hin, RevObject.TYPE type)
            throws IOException {
        Preconditions.checkArgument(RevObject.TYPE.FEATURE.equals(type));

        ImmutableList.Builder<Optional<Object>> valuesBuilder = new ImmutableList.Builder<Optional<Object>>();

        int attrCount = hin.readInt();
        for (int i = 0; i < attrCount; i++) {
            Object obj = readValue(hin);
            valuesBuilder.add(Optional.fromNullable(obj));
        }
        return new RevFeature(id, valuesBuilder.build());
    }

    /**
     * Reads an object from the input stream.
     * 
     * @param in the input stream
     * @return the object that was read
     * @throws IOException
     */
    private Object readValue(final Hessian2Input in) throws IOException {
        EntityType type = EntityType.fromValue(in.readInt());
        if (type == null)
            throw new IOException("Illegal format in data stream");
        switch (type) {
        case STRING:
            String str = in.readString();
            return str;
        case BOOLEAN:
            Boolean bool = Boolean.valueOf(in.readBoolean());
            return bool;
        case BYTE:
            byte[] bts = in.readBytes();
            if (bts.length == 1)
                return Byte.valueOf(bts[0]);
            else
                return null;
        case DOUBLE:
            Double doub = Double.valueOf(in.readDouble());
            return doub;
        case FLOAT:
            Float flt = Float.intBitsToFloat(in.readInt());
            return flt;
        case INT:
            Integer intg = Integer.valueOf(in.readInt());
            return intg;
        case LONG:
            Long lng = Long.valueOf(in.readLong());
            return lng;
        case BYTE_ARRAY:
            return in.readBytes();
        case BOOLEAN_ARRAY:
            int boolLength = in.readInt();
            boolean[] bools = new boolean[boolLength];
            for (int i = 0; i < boolLength; i++) {
                bools[i] = in.readBoolean();
            }
            return bools;
        case CHAR_ARRAY:
            String charstring = in.readString();
            return charstring.toCharArray();
        case DOUBLE_ARRAY:
            in.readNull();
            break;
        case FLOAT_ARRAY:
            in.readNull();
            break;
        case INT_ARRAY:
            in.readNull();
            break;
        case LONG_ARRAY:
            in.readNull();
            break;
        case BIGDECIMAL:
            String bdString = in.readString();
            return new BigDecimal(bdString);
        case BIGINT:
            byte[] biBytes = in.readBytes();
            return new BigInteger(biBytes);
        case UUID:
            long most = in.readLong();
            long least = in.readLong();
            return new UUID(most, least);
        case GEOMETRY:
            String srs = in.readString();
            WKBReader wkbReader = new WKBReader(geometryFactory);
            Geometry geom;
            try {
                geom = wkbReader.read(new InStream() {

                    public void read(byte[] buf) throws IOException {
                        int length = buf.length;
                        int returned = in.readBytes(buf, 0, length);
                    }
                });
            } catch (ParseException ex) {
                throw (IOException) new IOException(ex.getMessage()).initCause(ex);
            }
            in.readBytes(new byte[1], 0, 1);
            return geom;
        case NULL:
            in.readNull();
            return null;
        case UNKNOWN_SERIALISABLE:
            return in.readObject();
        case UNKNOWN:
            String classname = in.readString();
            String value = in.readString();
            return classname + value;
        }
        return null;
    }
}
