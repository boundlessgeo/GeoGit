/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DepthTreeIterator;
import org.geogit.api.plumbing.diff.DepthTreeIterator.Strategy;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @see RevParse
 * @see FindTreeChild
 */
public class ResolveFeatureType extends AbstractGeoGitOp<Optional<RevFeatureType>> {

    private String treeIshRefSpec;

    private ObjectDatabase objectDb;

    @Inject
    public ResolveFeatureType(ObjectDatabase objectDb) {
        this.objectDb = objectDb;
    }

    /**
     * @param treeIshRefSpec a ref spec that resolves to the tree or feature type holding the
     *        {@link Node#getMetadataId() metadataId} of the {@link RevFeatureType} to parse
     * @return
     */
    public ResolveFeatureType setFeatureType(String treeIshRefSpec) {
        this.treeIshRefSpec = treeIshRefSpec;
        return this;
    }

    @Override
    public Optional<RevFeatureType> call() {
        Preconditions.checkState(treeIshRefSpec != null, "ref spec has not been set.");
        final String refspec;
        if (treeIshRefSpec.contains(":")) {
            refspec = treeIshRefSpec;
        } else {
            refspec = "WORK_HEAD:" + treeIshRefSpec;
        }
        final String spec = refspec.substring(0, refspec.indexOf(':'));
        final String treePath = refspec.substring(refspec.indexOf(':') + 1);

        Optional<ObjectId> treeId = command(ResolveTreeish.class).setTreeish(refspec).call();
        Preconditions
                .checkArgument(treeId.isPresent(), "spec '%s' did not resolve to a tree", spec);

        RevTree tree = command(RevObjectParse.class).setObjectId(treeId.get()).call(RevTree.class)
                .get();

        DepthTreeIterator iter = new DepthTreeIterator(treePath, ObjectId.NULL, tree, objectDb,
                Strategy.FEATURES_ONLY);
        Preconditions.checkArgument(iter.hasNext(), "Wrong feature type spec: " + spec);

        ObjectId metadataID = iter.next().getMetadataId();

        Optional<RevFeatureType> ft = command(RevObjectParse.class).setObjectId(metadataID).call(
                RevFeatureType.class);
        return ft;
    }
}
