/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DiffCounter;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffTreeWalk;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * A faster alternative to count the number of diffs between two trees than walking a
 * {@link DiffTreeWalk} iterator.
 * 
 * @see DiffCounter
 */
public class DiffCount extends AbstractGeoGitOp<Long> {

    private StagingDatabase index;

    private String pathFilter;

    private String oldRefSpec;

    private String newRefSpec;

    @Inject
    public DiffCount(StagingDatabase index) {
        this.index = index;
    }

    public DiffCount setOldVersion(@Nullable String refSpec) {
        this.oldRefSpec = refSpec;
        return this;
    }

    public DiffCount setNewVersion(@Nullable String refSpec) {
        this.newRefSpec = refSpec;
        return this;
    }

    /**
     * @param path the path filter to use during the diff operation
     * @return {@code this}
     */
    public DiffCount setFilter(@Nullable String path) {
        pathFilter = path;
        return this;
    }

    @Override
    public Long call() {
        checkState(oldRefSpec != null, "old ref spec not provided");
        checkState(newRefSpec != null, "new ref spec not provided");

        final RevTree oldTree = getTree(oldRefSpec);
        final RevTree newTree = getTree(newRefSpec);

        Long diffCount;
        if (null == pathFilter) {
            DiffCounter counter = new DiffCounter(index, oldTree, newTree);
            diffCount = counter.get();
        } else {
            DiffTreeWalk treeWalk = new DiffTreeWalk(index, oldTree, newTree);
            treeWalk.setFilter(pathFilter);
            Iterator<DiffEntry> iterator = treeWalk.get();
            long count = 0;
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
            diffCount = Long.valueOf(count);
        }
        return diffCount;
    }

    /**
     * @return the tree referenced by the old ref, or the head of the index.
     */
    private RevTree getTree(String refSpec) {

        Optional<ObjectId> resolved = command(ResolveTreeish.class).setTreeish(refSpec).call();
        if (!resolved.isPresent()) {
            return RevTree.EMPTY;
        }
        ObjectId headTreeId = resolved.get();
        final RevTree headTree;
        if (headTreeId.isNull()) {
            headTree = RevTree.EMPTY;
        } else {
            headTree = command(RevObjectParse.class).setObjectId(headTreeId).call(RevTree.class)
                    .get();
        }

        return headTree;
    }

}
