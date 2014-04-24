/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.RevTreeBuilder;
import org.geogit.api.plumbing.diff.MutableTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;

/**
 * Creates a new {@link RevTreeBuilder} backed by the specified object database (the repository's by
 * default, or the staging area object database if so indicated)
 */
public class CreateTree extends AbstractGeoGitOp<RevTreeBuilder> {

    private boolean index;

    /**
     * @param toIndexDb if {@code true}, the returned tree is backed by the {@link StagingDatabase},
     *        otherwise by the repository's {@link ObjectDatabase}
     * @return {@code this}
     */
    public CreateTree setIndex(boolean toIndexDb) {
        index = toIndexDb;
        return this;
    }

    /**
     * Executes the create tree operation and returns a new mutable tree.
     * 
     * @return the {@link MutableTree} that was created by the operation
     */
    @Override
    protected  RevTreeBuilder _call() {
        ObjectDatabase storage = index ? stagingDatabase() : objectDatabase();
        return new RevTreeBuilder(storage);
    }

}
