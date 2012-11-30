/* Copyright (c) 2011-2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.InjectorBuilder;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RefDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * A repository is a collection of commits, each of which is an archive of what the project's
 * working tree looked like at a past date, whether on your machine or someone else's.
 * <p>
 * It also defines HEAD (see below), which identifies the branch or commit the current working tree
 * stemmed from. Lastly, it contains a set of branches and tags, to identify certain commits by
 * name.
 * </p>
 * 
 * @author Gabriel Roldan
 * @see WorkingTree
 */
public class Repository {

    @Inject
    private StagingArea index;

    @Inject
    private WorkingTree workingTree;

    @Inject
    private ObjectSerialisingFactory serialFactory;

    @Inject
    private Injector injector;

    @Inject
    private ConfigDatabase configDatabase;

    @Inject
    private RefDatabase refDatabase;

    @Inject
    private ObjectDatabase objectDatabase;

    private InjectorBuilder injectorBuilder = new InjectorBuilder();

    /**
     * Creates the repository.
     */
    public Repository() {
    }

    public void open() {
        refDatabase.create();
        objectDatabase.open();
        index.getDatabase().open();
    }

    /**
     * @return the {@link ConfigDatabase} for this repository
     */
    public ConfigDatabase getConfigDatabase() {
        return configDatabase;
    }

    /**
     * @return the {@link RefDatabase} for this repository
     */
    public RefDatabase getRefDatabase() {
        return refDatabase;
    }

    /**
     * @return the {@link ObjectDatabase} for this repository
     */
    public ObjectDatabase getObjectDatabase() {
        return objectDatabase;
    }

    /**
     * @return the {@link StagingArea} for this repository
     */
    public StagingArea getIndex() {
        return index;
    }

    /**
     * Closes the repository.
     */
    public void close() {
        refDatabase.close();
        objectDatabase.close();
        index.getDatabase().close();
    }

    /**
     * Finds and returns an instance of a command of the specified class.
     * 
     * @param commandClass the kind of command to locate and instantiate
     * @return a new instance of the requested command class, with its dependencies resolved
     */
    public <T extends AbstractGeoGitOp<?>> T command(Class<T> commandClass) {
        return injector.getInstance(commandClass);
    }

    /**
     * @return the {@link WorkingTree} for this repository
     */
    public WorkingTree getWorkingTree() {
        return workingTree;
    }

    /**
     * @param oid the {@link ObjectId} of the object to get
     * @return the raw {@link InputStream} for the object data
     * @throws IOException
     */
    public InputStream getRawObject(final ObjectId oid) throws IOException {
        return getObjectDatabase().getRaw(oid);
    }

    /**
     * Test if a blob exists in the object database
     * 
     * @param id the ID of the blob in the object database
     * @return true if the blob exists with the parameter ID, false otherwise
     */
    public boolean blobExists(final ObjectId id) {
        return getObjectDatabase().exists(id);
    }

    /**
     * @param revStr the string to parse
     * @return the parsed {@link Ref}, or {@link Optional#absent()} if it did not parse.
     */
    public Optional<Ref> getRef(final String revStr) {
        Optional<Ref> ref = command(RefParse.class).setName(revStr).call();
        return ref;
    }

    /**
     * @return the {@link Ref} pointed to by HEAD, or {@link Optional#absent()} if it could not be
     *         resolved.
     */
    public Optional<Ref> getHead() {
        return getRef(Ref.HEAD);
    }

    /**
     * Determines if a commit with the given {@link ObjectId} exists in the object database.
     * 
     * @param id the id to look for
     * @return true if the object was found, false otherwise
     */
    public boolean commitExists(final ObjectId id) {
        try {
            getObjectDatabase().get(id, newCommitReader());
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    /**
     * Gets the {@link RevCommit} with the given {@link ObjectId} from the object database.
     * 
     * @param commitId the {@code ObjectId} for the commit
     * @return the {@code RevCommit}
     */
    public RevCommit getCommit(final ObjectId commitId) {
        RevCommit commit = getObjectDatabase().get(commitId, newCommitReader());

        return commit;
    }

    /**
     * Test if a tree exists in the object database
     * 
     * @param id the ID of the tree in the object database
     * @return true if the tree exists with the parameter ID, false otherwise
     */
    public boolean treeExists(final ObjectId id) {
        try {
            getObjectDatabase().get(id, newRevTreeReader());
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    /**
     * @return the {@link ObjectId} of the root tree
     */
    public ObjectId getRootTreeId() {
        // find the root tree
        ObjectId commitId = command(RevParse.class).setRefSpec(Ref.HEAD).call().get();
        if (commitId.isNull()) {
            return commitId;
        }
        RevCommit commit = command(RevObjectParse.class).setRefSpec(commitId.toString())
                .call(RevCommit.class).get();
        ObjectId treeId = commit.getTreeId();
        return treeId;
    }

    /**
     * @return an {@link ObjectInserter} to insert objects into the object database
     */
    public ObjectInserter newObjectInserter() {
        return getObjectDatabase().newObjectInserter();
    }

    /**
     * @param contentId the {@link ObjectId} of the feature to get
     * @return the {@link RevFeature} that was found in the object database
     */
    public RevFeature getFeature(final ObjectId contentId) {
        ObjectReader<RevFeature> reader = newFeatureReader();

        RevFeature revFeature = getObjectDatabase().get(contentId, reader);

        return revFeature;
    }

    /**
     * @param commit commit to write
     * @return a new ObjectWriter for the given commit
     */
    public ObjectWriter<RevCommit> newCommitWriter(RevCommit commit) {
        return serialFactory.createCommitWriter(commit);
    }

    public ObjectReader<RevTree> newRevTreeReader() {
        return serialFactory.createRevTreeReader();
    }

    /**
     * @param tree the {@link RevTree} to write
     * @return a newly constructed {@link ObjectWriter} for the {@code RevTree}
     */
    public ObjectWriter<RevTree> newRevTreeWriter(RevTree tree) {
        return serialFactory.createRevTreeWriter(tree);
    }

    /**
     * @return a newly constructed {@link ObjectReader} for {@link RevCommit}s
     */
    public ObjectReader<RevCommit> newCommitReader() {
        return serialFactory.createCommitReader();
    }

    /**
     * @return a newly constructed {@link ObjectReader} for {@link RevFeature}s
     */
    public ObjectReader<RevFeature> newFeatureReader() {
        ObjectReader<RevFeature> reader = serialFactory.createFeatureReader();
        return reader;
    }

    /**
     * @param hints hints for feature reading
     * @return a newly constructed {@link ObjectReader} for {@link RevFeature}s
     */
    public ObjectReader<RevFeature> newFeatureReader(final Map<String, Serializable> hints) {
        return serialFactory.createFeatureReader(hints);
    }

    /**
     * @param feature the {@link RevFeature} to write
     * @return a newly constructed {@link ObjectWriter} for the given {@code RevFeature}
     */
    public ObjectWriter<RevFeature> newFeatureWriter(RevFeature feature) {
        return serialFactory.createFeatureWriter(feature);
    }

    /**
     * @param type the {@link RevFeatureType} to write
     * @return a newly constructed {@link ObjectWriter} for the given {@code RevFeatureType}
     */
    public ObjectWriter<RevFeatureType> newFeatureTypeWriter(RevFeatureType type) {
        return serialFactory.createFeatureTypeWriter(type);
    }

    /**
     * @return a newly constructed {@link ObjectReader} for {@link RevFeatureType}s
     */
    public ObjectReader<RevFeatureType> newFeatureTypeReader() {
        return serialFactory.createFeatureTypeReader();
    }

    /**
     * @return the {@link ObjectSerialisingFactory} for this repository
     */
    public ObjectSerialisingFactory getSerializationFactory() {
        return serialFactory;
    }

    /**
     * @return the existing {@link RevTree} pointed to by HEAD, or a new {@code RevTree} if it did
     *         not exist
     */
    public RevTree getOrCreateHeadTree() {
        Optional<ObjectId> headTreeId = command(ResolveTreeish.class).setTreeish(Ref.HEAD).call();
        if (!headTreeId.isPresent() || headTreeId.get().isNull()) {
            return RevTree.EMPTY;
        }
        return getTree(headTreeId.get());
    }

    /**
     * @param treeId the tree to retrieve
     * @return the {@link RevTree} referred to by the given {@link ObjectId}
     */
    public RevTree getTree(ObjectId treeId) {
        return command(RevObjectParse.class).setObjectId(treeId).call(RevTree.class).get();
    }

    /**
     * @param path the path to search for
     * @return an {@link Optional} of the {@link Node} for the child, or {@link Optional#absent()}
     *         if it wasn't found
     */
    public Optional<Node> getRootTreeChild(String path) {
        return command(FindTreeChild.class).setChildPath(path).call();
    }

    /**
     * Search the given tree for the child path.
     * 
     * @param tree the tree to search
     * @param childPath the path to search for
     * @return an {@link Optional} of the {@link Node} for the child path, or
     *         {@link Optional#absent()} if it wasn't found
     */
    public Optional<Node> getTreeChild(RevTree tree, String childPath) {
        return command(FindTreeChild.class).setParent(tree).setChildPath(childPath).call();
    }

}
