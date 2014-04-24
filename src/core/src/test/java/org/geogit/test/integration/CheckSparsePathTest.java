/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.CheckSparsePath;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.MergeOp;
import org.geogit.storage.GraphDatabase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Suppliers;

public class CheckSparsePathTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        // These values should be used during a commit to set author/committer
        // TODO: author/committer roles need to be defined better, but for
        // now they are the same thing.
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.name")
                .setValue("groldan").call();
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.email")
                .setValue("groldan@opengeo.org").call();
    }

    @Test
    public void testCheckSparsePath() throws Exception {
        // Create the following revision graph
        // o - commit1
        // |\
        // | o - commit2
        // | |
        // | o - commit3
        // | |\
        // | | o - commit4
        // | | |\
        // | | | o - commit5 (sparse)
        // | | | |
        // | | o | - commit6
        // | | |/
        // | | o - commit7
        // | |
        // o | - commit8
        // | |
        // | o - commit9
        // |/
        // o - commit10
        insertAndAdd(points1);
        RevCommit commit1 = geogit.command(CommitOp.class).setMessage("commit1").call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        RevCommit commit2 = geogit.command(CommitOp.class).setMessage("commit2").call();
        insertAndAdd(points3);
        RevCommit commit3 = geogit.command(CommitOp.class).setMessage("commit3").call();
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch2").call();
        insertAndAdd(lines1);
        RevCommit commit4 = geogit.command(CommitOp.class).setMessage("commit4").call();
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch3").call();
        insertAndAdd(poly1);
        RevCommit commit5 = geogit.command(CommitOp.class).setMessage("commit5").call();
        geogit.getRepository().graphDatabase()
                .setProperty(commit5.getId(), GraphDatabase.SPARSE_FLAG, "true");
        geogit.command(CheckoutOp.class).setSource("branch2").call();
        insertAndAdd(poly2);
        RevCommit commit6 = geogit.command(CommitOp.class).setMessage("commit6").call();
        RevCommit commit7 = geogit.command(MergeOp.class).setMessage("commit7")
                .addCommit(Suppliers.ofInstance(commit5.getId())).call().getMergeCommit();

        geogit.command(CheckoutOp.class).setSource("branch1").call();
        insertAndAdd(lines3);
        RevCommit commit9 = geogit.command(CommitOp.class).setMessage("commit9").call();

        // checkout master
        geogit.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(lines2);
        RevCommit commit8 = geogit.command(CommitOp.class).setMessage("commit8").call();

        RevCommit commit10 = geogit.command(MergeOp.class).setMessage("commit10")
                .addCommit(Suppliers.ofInstance(commit9.getId())).call().getMergeCommit();

        CheckSparsePath command = geogit.command(CheckSparsePath.class);

        assertTrue(command.setStart(commit7.getId()).setEnd(commit1.getId()).call());
        assertFalse(command.setStart(commit6.getId()).setEnd(commit1.getId()).call());
        assertTrue(command.setStart(commit5.getId()).setEnd(commit2.getId()).call());
        assertFalse(command.setStart(commit10.getId()).setEnd(commit1.getId()).call());
        assertFalse(command.setStart(commit10.getId()).setEnd(commit3.getId()).call());
        assertFalse(command.setStart(commit8.getId()).setEnd(commit1.getId()).call());
        assertFalse(command.setStart(commit4.getId()).setEnd(commit2.getId()).call());
        assertFalse(command.setStart(commit7.getId()).setEnd(commit5.getId()).call());

    }

    @Test
    public void testCheckSparsePath2() throws Exception {
        // Create the following revision graph
        // o - commit1
        // |\
        // | o - commit2 (sparse)
        // | |
        // | o - commit3
        // | |\
        // | | o - commit4
        // | | |\
        // | | | o - commit5 (sparse)
        // | | | |
        // | | o | - commit6
        // | | |/
        // | | o - commit7
        // | |
        // o | - commit8
        // | |
        // | o - commit9
        // |/
        // o - commit10
        insertAndAdd(points1);
        RevCommit commit1 = geogit.command(CommitOp.class).setMessage("commit1").call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        RevCommit commit2 = geogit.command(CommitOp.class).setMessage("commit2").call();
        geogit.getRepository().graphDatabase()
                .setProperty(commit2.getId(), GraphDatabase.SPARSE_FLAG, "true");
        insertAndAdd(points3);
        RevCommit commit3 = geogit.command(CommitOp.class).setMessage("commit3").call();
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch2").call();
        insertAndAdd(lines1);
        RevCommit commit4 = geogit.command(CommitOp.class).setMessage("commit4").call();
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch3").call();
        insertAndAdd(poly1);
        RevCommit commit5 = geogit.command(CommitOp.class).setMessage("commit5").call();
        geogit.getRepository().graphDatabase()
                .setProperty(commit5.getId(), GraphDatabase.SPARSE_FLAG, "true");
        geogit.command(CheckoutOp.class).setSource("branch2").call();
        insertAndAdd(poly2);
        RevCommit commit6 = geogit.command(CommitOp.class).setMessage("commit6").call();
        RevCommit commit7 = geogit.command(MergeOp.class).setMessage("commit7")
                .addCommit(Suppliers.ofInstance(commit5.getId())).call().getMergeCommit();

        geogit.command(CheckoutOp.class).setSource("branch1").call();
        insertAndAdd(lines3);
        RevCommit commit9 = geogit.command(CommitOp.class).setMessage("commit9").call();

        // checkout master
        geogit.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(lines2);
        RevCommit commit8 = geogit.command(CommitOp.class).setMessage("commit8").call();

        RevCommit commit10 = geogit.command(MergeOp.class).setMessage("commit10")
                .addCommit(Suppliers.ofInstance(commit9.getId())).call().getMergeCommit();

        CheckSparsePath command = geogit.command(CheckSparsePath.class);

        assertTrue(command.setStart(commit7.getId()).setEnd(commit1.getId()).call());
        assertTrue(command.setStart(commit6.getId()).setEnd(commit1.getId()).call());
        assertTrue(command.setStart(commit5.getId()).setEnd(commit2.getId()).call());
        assertTrue(command.setStart(commit10.getId()).setEnd(commit1.getId()).call());
        assertFalse(command.setStart(commit10.getId()).setEnd(commit3.getId()).call());
        assertFalse(command.setStart(commit8.getId()).setEnd(commit1.getId()).call());
        assertFalse(command.setStart(commit4.getId()).setEnd(commit2.getId()).call());
        assertFalse(command.setStart(commit7.getId()).setEnd(commit5.getId()).call());

    }
}