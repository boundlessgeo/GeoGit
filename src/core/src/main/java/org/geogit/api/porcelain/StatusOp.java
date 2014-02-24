/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.plumbing.DiffIndex;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.api.plumbing.merge.ConflictsReadOp;
import org.geogit.di.CanRunDuringConflict;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;

import com.google.inject.Inject;
import com.sun.xml.internal.xsom.impl.scd.Iterators;

@CanRunDuringConflict
public class StatusOp extends AbstractGeoGitOp<StatusSummary> {

    @Inject
    public StatusOp() {

    }

    @Override
    public StatusSummary call() {
        WorkingTree workTree = getWorkTree();
        StagingArea index = getIndex();
        long countStaged = index.countStaged(null).getCount();
        int countConflicted = index.countConflicted(null);
        long countUnstaged = workTree.countUnstaged(null).getCount();

        Iterator<DiffEntry> stagedEntries = Iterators.empty();
        Iterator<DiffEntry> unstagedEntries = Iterators.empty();
        List<Conflict> conflicts = new ArrayList<Conflict>();

        if (countStaged > 0) {
            stagedEntries = command(DiffIndex.class).setReportTrees(true).call();
        }
        if (countConflicted > 0) {
            conflicts = command(ConflictsReadOp.class).call();
        }
        if (countUnstaged > 0) {
            unstagedEntries = command(DiffWorkTree.class).setReportTrees(true).call();
        }
        StatusSummary summary = new StatusSummary();
        summary.setCountStaged(countStaged);
        summary.setCountUnstaged(countUnstaged);
        summary.setCountConflicts(countConflicted);
        summary.setStaged(stagedEntries);
        summary.setUnstaged(unstagedEntries);
        summary.setConflicts(conflicts);
        return summary;
    }
}
