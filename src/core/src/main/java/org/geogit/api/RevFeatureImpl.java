/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.api.plumbing.HashObject;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * A binary representation of the values of a Feature.
 * 
 */
public class RevFeatureImpl extends AbstractRevObject implements RevFeature {

    private final ImmutableList<Optional<Object>> values;

    public static RevFeatureImpl build(ImmutableList<Optional<Object>> values) {
        RevFeatureImpl unnamed = new RevFeatureImpl(values);
        ObjectId id = new HashObject().setObject(unnamed).call();
        return new RevFeatureImpl(id, values);
    }

    /**
     * Constructs a new {@code RevFeature} with the provided set of values.
     * 
     * @param values a list of values, with {@link Optional#absent()} representing a null value
     */
    private RevFeatureImpl(ImmutableList<Optional<Object>> values) {
        this(ObjectId.NULL, values);
    }

    /**
     * Constructs a new {@code RevFeature} with the provided {@link ObjectId} and set of values
     * 
     * @param id the {@link ObjectId} to use for this feature
     * @param values a list of values, with {@link Optional#absent()} representing a null value
     */
    public RevFeatureImpl(ObjectId id, ImmutableList<Optional<Object>> values) {
        super(id);
        this.values = values;
    }

    @Override
	public ImmutableList<Optional<Object>> getValues() {
        return values;
    }

    @Override
    public TYPE getType() {
        return TYPE.FEATURE;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Feature[");
        builder.append(getId().toString());
        builder.append("; ");
        boolean first = true;
        for (Optional<Object> value : getValues()) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }

            String valueString = String.valueOf(value.orNull());
            builder.append(valueString.substring(0, Math.min(10, valueString.length())));
        }
        builder.append(']');
        return builder.toString();
    }
}
