/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.Iterator;
import java.util.Map.Entry;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

/**
 * Provides content information for repository objects
 */
public class CatObject extends AbstractGeoGitOp<CharSequence> {

    private Supplier<? extends RevObject> object;

    public CatObject setObject(Supplier<? extends RevObject> object) {
        this.object = object;
        return this;
    }

    @Override
    public CharSequence call() {
        Preconditions.checkState(object != null);
        RevObject revObject = object.get();
        if (revObject instanceof RevFeature) {
            RevFeature feature = (RevFeature) revObject;
            StringBuilder sb = new StringBuilder();
            sb.append(feature.getId().toString()).append("\n");
            ImmutableList<Optional<Object>> values = feature.getValues();
            for (Optional<Object> value : values) {
                if (value.isPresent()) {
                    sb.append(value.get().toString()).append("\n");
                } else {
                    sb.append(value.toString()).append("\n");
                }
            }
            return sb.toString();
        } else if (revObject instanceof RevTree) {
            return new CatTree((RevTree) revObject).call();
        } else if (revObject instanceof RevCommit) {
            StringBuilder sb = new StringBuilder();
            RevCommit commit = (RevCommit) revObject;
            sb.append(commit.getId()).append("\n");
            sb.append(commit.getTimestamp()).append("\n");
            sb.append(commit.getMessage()).append("\n");
            sb.append(commit.getAuthor()).append("\n");
            sb.append(commit.getCommitter()).append("\n");
            sb.append(commit.getTreeId()).append("\n");
            RevTree revTree = command(RevObjectParse.class).setObjectId(commit.getTreeId())
                    .call(RevTree.class).get();
            sb.append(new CatTree(revTree).call());
            return sb.toString();
        } else {
            throw new UnsupportedOperationException("not implemented for "
                    + revObject.getClass().getName());
        }
    }

    private class CatTree {
        private RevTree tree;

        private StringBuilder sb;

        public CatTree(RevTree tree) {
            this.tree = tree;
            this.sb = new StringBuilder();
        }

        public CharSequence call() {
            printTree(this.tree, 0);
            return sb;
        }

        private void printTree(RevTree tree, final int indent) {
            println(tree.getId().toString());
            if (tree.buckets().isPresent()) {
                printBuckets(tree.buckets().get(), indent);
            } else {
                printChildren(tree.children(), indent);
            }
        }

        private void printBuckets(final ImmutableSortedMap<Integer, ObjectId> buckets,
                final int indent) {

            for (Entry<Integer, ObjectId> entry : buckets.entrySet()) {
                Integer bucketId = entry.getKey();
                ObjectId treeId = entry.getValue();
                indent(indent + 1);
                print(Strings.padStart(bucketId.toString(), 3, ' '));
                print("-->");
                RevTree bucketTree = command(RevObjectParse.class).setObjectId(treeId)
                        .call(RevTree.class).get();
                printTree(bucketTree, indent + 1);
            }
        }

        private void printChildren(Iterator<NodeRef> children, int indent) {
            while (children.hasNext()) {
                NodeRef ref = children.next();
                indent(indent + 1);
                print(ref.getObjectId().toString());
                print(" --> ");
                println(ref.getPath() + " ");
            }
        }

        private void indent(final int level) {
            for (int i = 0; i < level; i++) {
                sb.append("    ");
            }
        }

        private void print(String s) {
            sb.append(s);
        }

        private void println(String s) {
            sb.append(s).append('\n');
        }
    }

}
