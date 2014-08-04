package org.geogit.api.plumbing.diff;

import java.util.Stack;

import javax.annotation.Nullable;

import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public final class DiffPathTracker {

    private String currentPath;

    private Stack<Node> leftTrees = new Stack<>(), rightTrees = new Stack<>();

    public String getCurrentPath() {
        return currentPath;
    }

    public boolean isEmpty() {
        return leftTrees.isEmpty();
    }

    public Optional<Node> currentLeftTree() {
        return Optional.fromNullable(leftTrees.peek());
    }

    public Optional<Node> currentRightTree() {
        return Optional.fromNullable(rightTrees.peek());
    }

    public Optional<ObjectId> currentLeftMetadataId() {
        return metadataId(leftTrees.peek());
    }

    public Optional<ObjectId> currentrightMetadataId() {
        return metadataId(rightTrees.peek());
    }

    private Optional<ObjectId> metadataId(@Nullable Node treeNode) {
        if (treeNode == null) {
            return Optional.absent();
        }
        return treeNode.getMetadataId();
    }

    public String tree(@Nullable Node left, @Nullable Node right) {
        Preconditions.checkArgument(left != null || right != null);
        this.leftTrees.add(left);
        this.rightTrees.add(right);
        String name = name(left, right);
        if (currentPath == null) {
            currentPath = name;
        } else {
            currentPath = NodeRef.appendChild(currentPath, name);
        }
        return currentPath;
    }

    /**
     * @return the resulting parent path after removing left and right from the stack, or
     *         {@code null} if left and/or right are a root tree.
     */
    public String endTree(@Nullable Node left, @Nullable Node right) {
        final Node popLeft = this.leftTrees.pop();
        final Node popRight = this.rightTrees.pop();
        try {
            Preconditions.checkState(Objects.equal(popLeft, left));
            Preconditions.checkState(Objects.equal(popRight, right));
        } catch (IllegalStateException e) {
            throw e;
        }
        if (NodeRef.ROOT.equals(currentPath)) {
            currentPath = null;
        } else {
            String fullPath = currentPath;
            currentPath = NodeRef.parentPath(fullPath);
        }
        return currentPath;
    }

    public String name(Node left, Node right) {
        return left == null ? right.getName() : left.getName();
    }
}