package org.geogit.storage;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.repository.RepositoryConnectionException;

import com.google.inject.Provider;

public class ForwardingObjectDatabase implements ObjectDatabase {

    protected final Provider<? extends ObjectDatabase> subject;

    public ForwardingObjectDatabase(Provider<? extends ObjectDatabase> odb) {
        this.subject = odb;
    }

    @Override
    public void open() {
        subject.get().open();
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        subject.get().configure();
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        subject.get().checkConfig();
    }

    @Override
    public boolean isOpen() {
        return subject.get().isOpen();
    }

    @Override
    public void close() {
        subject.get().close();
    }

    @Override
    public boolean exists(ObjectId id) {
        return subject.get().exists(id);
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        return subject.get().lookUp(partialId);
    }

    @Override
    public RevObject get(ObjectId id) throws IllegalArgumentException {
        return subject.get().get(id);
    }

    @Override
    public <T extends RevObject> T get(ObjectId id, Class<T> type) throws IllegalArgumentException {
        return subject.get().get(id, type);
    }

    @Override
    public RevObject getIfPresent(ObjectId id) {
        return subject.get().getIfPresent(id);
    }

    @Override
    public <T extends RevObject> T getIfPresent(ObjectId id, Class<T> type)
            throws IllegalArgumentException {
        return subject.get().getIfPresent(id, type);
    }

    @Override
    public RevTree getTree(ObjectId id) {
        return subject.get().getTree(id);
    }

    @Override
    public RevFeature getFeature(ObjectId id) {
        return subject.get().getFeature(id);
    }

    @Override
    public RevFeatureType getFeatureType(ObjectId id) {
        return subject.get().getFeatureType(id);
    }

    @Override
    public RevCommit getCommit(ObjectId id) {
        return subject.get().getCommit(id);
    }

    @Override
    public RevTag getTag(ObjectId id) {
        return subject.get().getTag(id);
    }

    @Override
    public boolean put(RevObject object) {
        return subject.get().put(object);
    }

    @Override
    @Deprecated
    public ObjectInserter newObjectInserter() {
        return new ObjectInserter(this);
    }

    @Override
    public boolean delete(ObjectId objectId) {
        return subject.get().delete(objectId);
    }

    @Override
    public Iterator<RevObject> getAll(Iterable<ObjectId> ids) {
        return subject.get().getAll(ids);
    }

    @Override
    public Iterator<RevObject> getAll(Iterable<ObjectId> ids, BulkOpListener listener) {
        return subject.get().getAll(ids, listener);
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects) {
        subject.get().putAll(objects);
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects, BulkOpListener listener) {
        subject.get().putAll(objects, listener);
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids) {
        return deleteAll(ids);
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids, BulkOpListener listener) {
        return subject.get().deleteAll(ids, listener);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), subject);
    }
}
