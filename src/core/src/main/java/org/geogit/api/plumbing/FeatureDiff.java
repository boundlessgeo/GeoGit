package org.geogit.api.plumbing;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class FeatureDiff {

    private Map<PropertyDescriptor, AttributeDiff<?>> diffs;

    public FeatureDiff(RevFeature newRevFeature, RevFeature oldRevFeature,
            RevFeatureType newRevFeatureType, RevFeatureType oldRevFeatureType) {

        diffs = new HashMap<PropertyDescriptor, AttributeDiff<?>>();
        ImmutableList<PropertyDescriptor> oldAttributes = oldRevFeatureType.sortedDescriptors();
        ImmutableList<PropertyDescriptor> newAttributes = newRevFeatureType.sortedDescriptors();
        ImmutableList<Optional<Object>> oldValues = oldRevFeature.getValues();
        ImmutableList<Optional<Object>> newValues = newRevFeature.getValues();
        BitSet updatedAttributes = new BitSet(newValues.size());
        for (int i = 0; i < oldAttributes.size(); i++) {
            Optional<Object> oldValue = oldValues.get(i);
            int idx = newAttributes.indexOf(oldAttributes.get(i));
            if (idx != -1) {
                Optional<Object> newValue = newValues.get(idx);
                if (!oldValue.equals(newValue)) {
                    diffs.put(oldAttributes.get(i),
                            new GenericAttributeDiffImpl(oldValue, newValue));
                }
                updatedAttributes.set(idx);
            } else {
                diffs.put(oldAttributes.get(i), new GenericAttributeDiffImpl(oldValue, null));
            }
        }
        updatedAttributes.flip(0, updatedAttributes.length());
        for (int i = updatedAttributes.nextSetBit(0); i >= 0; i = updatedAttributes
                .nextSetBit(i + 1)) {
            diffs.put(newAttributes.get(i), new GenericAttributeDiffImpl(null, newValues.get(i)));
        }

    }

    public boolean hasDifferences() {
        return diffs.size() != 0;
    }

    public Map<PropertyDescriptor, AttributeDiff<?>> getDiffs() {
        return diffs;
    }

    public String toString() {

        StringBuilder sb = new StringBuilder();
        Set<Entry<PropertyDescriptor, AttributeDiff<?>>> entries = diffs.entrySet();
        Iterator<Entry<PropertyDescriptor, AttributeDiff<?>>> iter = entries.iterator();
        while (iter.hasNext()) {
            Entry<PropertyDescriptor, AttributeDiff<?>> entry = iter.next();
            PropertyDescriptor pd = entry.getKey();
            AttributeDiff<?> ad = entry.getValue();
            String oldValue = ad.getNewValue() == null ? "[MISSING]" : ad.getNewValue().toString();
            String newValue = ad.getOldValue() == null ? "[MISSING]" : ad.getOldValue().toString();
            sb.append(pd.getName() + "<" + pd.getType().toString() + ">: " + oldValue + " ---> "
                    + newValue + "\n");
        }
        return sb.toString();

    }

}
