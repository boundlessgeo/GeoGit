/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.util.TreeMap;

import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeImpl;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Maps;

/**
 * Reads {@link RevTree trees} from a binary encoded stream.
 * 
 */
class HessianRevTreeReader extends HessianRevReader<RevTree> implements ObjectReader<RevTree> {

    public HessianRevTreeReader() {
    }

    @Override
    protected RevTree read(ObjectId id, Hessian2Input hin, RevObject.TYPE blobType)
            throws IOException {
        Preconditions.checkArgument(RevObject.TYPE.TREE.equals(blobType));

        final long size = hin.readLong();

        Builder<org.geogit.api.Node> features = ImmutableList.builder();
        Builder<org.geogit.api.Node> trees = ImmutableList.builder();
        TreeMap<Integer, ObjectId> subtrees = Maps.newTreeMap();

        while (true) {
            Node type = Node.fromValue(hin.readInt());

            if (type.equals(Node.REF)) {
                org.geogit.api.Node entryRef = readNode(hin);
                if (entryRef.getType().equals(TYPE.TREE)) {
                    trees.add(entryRef);
                } else {
                    features.add(entryRef);
                }
            } else if (type.equals(Node.BUCKET)) {
                parseAndSetSubTree(hin, subtrees);
            } else if (type.equals(Node.END)) {
                break;
            }
        }

        RevTree tree;
        if (subtrees.isEmpty()) {
            tree = RevTreeImpl.createLeafTree(id, size, features.build(), trees.build());
        } else {
            tree = RevTreeImpl.createNodeTree(id, size, subtrees);
        }
        return tree;
    }

    private void parseAndSetSubTree(Hessian2Input hin, TreeMap<Integer, ObjectId> subtrees)
            throws IOException {
        int bucket = hin.readInt();
        ObjectId id = readObjectId(hin);
        subtrees.put(Integer.valueOf(bucket), id);
    }
}
