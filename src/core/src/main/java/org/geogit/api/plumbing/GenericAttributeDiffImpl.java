package org.geogit.api.plumbing;


public class GenericAttributeDiffImpl<T> implements AttributeDiff<T> {

    private T newValue;

    private T oldValue;

    public GenericAttributeDiffImpl(T oldValue, T newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public T getOldValue() {
        return oldValue;
    }

    @Override
    public T getNewValue() {
        return newValue;
    }

}
