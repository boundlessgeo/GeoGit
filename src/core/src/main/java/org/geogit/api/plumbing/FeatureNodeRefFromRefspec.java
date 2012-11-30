package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DepthTreeIterator;
import org.geogit.api.plumbing.diff.DepthTreeIterator.Strategy;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class FeatureNodeRefFromRefspec extends AbstractGeoGitOp<NodeRef> {

    private WorkingTree workTree;

    private ObjectDatabase objectDb;

    private String ref;

    @Inject
    public FeatureNodeRefFromRefspec(WorkingTree workTree, ObjectDatabase objectDb) {
        this.workTree = workTree;
        this.objectDb = objectDb;
    }

    public FeatureNodeRefFromRefspec setRefspec(String ref) {
        this.ref = ref;
        return this;
    }

    private RevFeatureType getFeatureTypeFromRefSpec() {

        String featureTypeRef = NodeRef.parentPath(ref);
        String fullRef;
        if (featureTypeRef.contains(":")) {
            fullRef = featureTypeRef;
        } else {
            fullRef = "WORK_HEAD:" + featureTypeRef;
        }
        Optional<RevObject> revObject = command(RevObjectParse.class).setRefSpec(fullRef).call(
                RevObject.class);
        DepthTreeIterator iter = new DepthTreeIterator((RevTree) revObject.get(), objectDb,
                Strategy.CHILDREN);

        while (iter.hasNext()) {
            NodeRef nodeRef = iter.next();
            if (nodeRef.getType() == TYPE.FEATURE) {
                RevFeatureType revFeatureType = command(RevObjectParse.class)
                        .setObjectId(nodeRef.getMetadataId()).call(RevFeatureType.class).get();
                return revFeatureType;
            }
        }

        // should not reach this, since first we have checked that the feature to compare exists, so
        // there is at least one feature
        return null;

    }

    private Optional<RevFeature> getFeatureFromRefSpec() {

        Optional<RevObject> revObject = command(RevObjectParse.class).setRefSpec(ref).call(
                RevObject.class);

        if (!revObject.isPresent()) { // let's try to see if it is a feature in the working tree
            NodeRef.checkValidPath(ref);
            Optional<NodeRef> treeRef = command(FindTreeChild.class).setParent(workTree.getTree())
                    .setChildPath(ref).call();
            Preconditions.checkArgument(treeRef.isPresent(), "Invalid reference: %s", ref);
            ObjectId treeId = treeRef.get().getObjectId();
            revObject = command(RevObjectParse.class).setObjectId(treeId).call(RevObject.class);
        }

        if (revObject.isPresent()) {
            Preconditions.checkArgument(TYPE.FEATURE.equals(revObject.get().getType()),
                    "%s does not resolve to a feature", ref);
            return Optional.of(RevFeature.class.cast(revObject.get()));
        } else {
            return Optional.absent();
        }
    }

    @Override
    public NodeRef call() {

        Optional<RevFeature> feature = getFeatureFromRefSpec();
        
        if (feature.isPresent()){
            RevFeatureType featureType = getFeatureTypeFromRefSpec();
            RevFeature feat = feature.get();
            return new NodeRef(NodeRef.nodeFromPath(ref), feat.getId(), featureType.getId(),
                    TYPE.FEATURE);

        }
        else{
            return new NodeRef("",ObjectId.NULL, ObjectId.NULL, TYPE.FEATURE);
        }
        

    }

}
