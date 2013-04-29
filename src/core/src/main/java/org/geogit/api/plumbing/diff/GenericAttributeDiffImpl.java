package org.geogit.api.plumbing.diff;

import org.geogit.storage.text.TextValueSerializer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class GenericAttributeDiffImpl implements AttributeDiff {

    private Optional<?> newValue;

    private Optional<?> oldValue;

    public GenericAttributeDiffImpl(Optional<?> oldValue, Optional<?> newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public TYPE getType() {
        TYPE type;
        if ((newValue == null || !newValue.isPresent())
                && (oldValue == null || !oldValue.isPresent())) {
            type = TYPE.NO_CHANGE;
        } else if (newValue == null || !newValue.isPresent()) {
            type = TYPE.REMOVED;
        } else if (oldValue == null || !oldValue.isPresent()) {
            type = TYPE.ADDED;
        } else if (oldValue.equals(newValue)) {
            type = TYPE.NO_CHANGE;
        } else {
            type = TYPE.MODIFIED;
        }
        return type;
    }

    @Override
    public Optional<?> getOldValue() {
        return oldValue;
    }

    @Override
    public Optional<?> getNewValue() {
        return newValue;
    }

    public String toString() {
        if (getType().equals(TYPE.MODIFIED)) {
            return attributeValueAsString(oldValue) + " -> " + attributeValueAsString(newValue);
        } else if (getType().equals(TYPE.ADDED)) {
            return "[MISSING] -> " + attributeValueAsString(newValue);
        } else if (getType().equals(TYPE.REMOVED)) {
            return attributeValueAsString(oldValue) + " -> [MISSING]";
        } else {
            return "[NO CHANGE] -> " + attributeValueAsString(oldValue);
        }
    }

    private CharSequence attributeValueAsString(Optional<?> value) {
        return TextValueSerializer.asString(Optional.fromNullable((Object) value.orNull()));
    }

    @Override
    public AttributeDiff reversed() {
        return new GenericAttributeDiffImpl(newValue, oldValue);
    }

    @Override
    public Optional<?> applyOn(Optional<?> obj) {
        Preconditions.checkState(canBeAppliedOn(obj));
        return newValue;
    }

    @Override
    public boolean canBeAppliedOn(Optional<?> obj) {
        if (obj == null) {
            return oldValue == null;
        }
        return obj.equals(oldValue);
    }

    @Override
    public String asText() {
        if (getType().equals(TYPE.MODIFIED)) {
            return getType().name().toCharArray()[0] + "\t" + attributeValueAsString(oldValue)
                    + "\t" + attributeValueAsString(newValue);
        } else if (getType().equals(TYPE.ADDED)) {
            return getType().name().toCharArray()[0] + "\t" + attributeValueAsString(newValue);
        } else {
            return getType().name().toCharArray()[0] + "\t" + attributeValueAsString(oldValue);
        }
    }

    @Override
    public boolean equals(Object o) {
        // TODO: this is a temporary simple comparison. Should be more elaborate
        if (!(o instanceof GenericAttributeDiffImpl)) {
            return false;
        }
        GenericAttributeDiffImpl d = (GenericAttributeDiffImpl) o;
        return d.oldValue.equals(oldValue) && d.newValue.equals(newValue);
    }

    @Override
    public boolean conflicts(AttributeDiff ad) {
        if (ad instanceof GenericAttributeDiffImpl) {
            GenericAttributeDiffImpl gad = (GenericAttributeDiffImpl) ad;
            return !gad.newValue.equals(newValue);
        } else {
            return true;
        }
    }

}
