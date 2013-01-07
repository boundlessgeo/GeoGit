package org.geogit.api.plumbing;

public interface AttributeDiff<T> {

    public T getOldValue();

    public T getNewValue();

}
