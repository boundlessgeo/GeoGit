/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.inject.Inject;

/**
 * Compares two features in the repository and returns a {code FeatureDiff} object representing it.
 * No checking is performed to ensure that the old and new features actually correspond to two
 * versions of the same feature and not to two unrelated features. This class behaves as if it was
 * unaware of the actual location of compared features, and merely compares them ignoring additional
 * information about them.
 * 
 */
public class DiffFeature extends AbstractGeoGitOp<FeatureDiff> {

    private ObjectDatabase objectDb;

    private WorkingTree workTree;

    private NodeRef oldNodeRef;

    private NodeRef newNodeRef;

    /**
     * Constructs a new instance of the {@code DiffFeature} operation with the given parameters.
     * 
     * @param objectDb the repository object database
     * @param serialFactory the serialization factory
     */
    @Inject
    public DiffFeature(WorkingTree workTree, ObjectDatabase objectDb) {
        this.workTree = workTree;
        this.objectDb = objectDb;
    }

    /**
     * @param oldNodeRef the ref that points to the "old" version of the feature to compare
     * @return {@code this}
     */
    public DiffFeature setOldVersion(Supplier<NodeRef> oldNodeRef) {
        this.oldNodeRef = oldNodeRef.get();
        return this;
    }

    /**
     * @param oldNodeRef the ref that points to the "old" version of the feature to compare
     * @return {@code this}
     */
    public DiffFeature setNewVersion(Supplier<NodeRef> newNodeRef) {
        this.newNodeRef = newNodeRef.get();
        return this;
    }

    /**
     * Finds differences between the two specified trees.
     * 
     * @return a FeatureDiff object with the differences between the specified features
     * @see FeatureDiff
     */
    @Override
    public FeatureDiff call() throws IllegalArgumentException {
        checkNotNull(oldNodeRef, "old version not specified");
        checkNotNull(newNodeRef, "new version not specified");

        Optional<RevFeature> oldFeature = command(RevObjectParse.class).setObjectId(
                oldNodeRef.getObjectId()).call(RevFeature.class);
        checkArgument(oldFeature.isPresent(), "Invalid reference: %s", oldNodeRef);

        Optional<RevFeature> newFeature = command(RevObjectParse.class).setObjectId(
                newNodeRef.getObjectId()).call(RevFeature.class);
        checkArgument(newFeature.isPresent(), "Invalid reference: %s", newNodeRef);
        
        Optional<RevFeatureType> oldFeatureType = command(RevObjectParse.class).setObjectId(
                oldNodeRef.getMetadataId()).call(RevFeatureType.class);
        checkArgument(oldFeatureType.isPresent(), "Invalid reference: %s", oldNodeRef);
        
        Optional<RevFeatureType> newFeatureType = command(RevObjectParse.class).setObjectId(
                newNodeRef.getMetadataId()).call(RevFeatureType.class);
        checkArgument(newFeatureType.isPresent(), "Invalid reference: %s", newNodeRef);

        return compare(oldFeature.get(), newFeature.get(), oldFeatureType.get(),
                newFeatureType.get());
        
    }

    private FeatureDiff compare(RevFeature oldRevFeature, RevFeature newRevFeature,
            RevFeatureType oldRevFeatureType, RevFeatureType newRevFeatureType) {

        return new FeatureDiff(newRevFeature, oldRevFeature, newRevFeatureType,
                oldRevFeatureType);
    }


}
