/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.test.integration.repository;

import static org.geogit.api.NodeRef.appendChild;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.repository.WorkingTree;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.feature.NameImpl;
import org.geotools.util.NullProgressListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;

import com.google.common.base.Optional;

/**
 *
 */
public class WorkingTreeTest extends RepositoryTestCase {

    private WorkingTree workTree;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        workTree = repo.getWorkingTree();
    }

    @Test
    public void testInsertSingle() throws Exception {
        Name name = points1.getType().getName();
        String parentPath = name.getLocalPart();
        NodeRef ref = workTree.insert(parentPath, points1);
        ObjectId objectId = ref.objectId();

        assertEquals(objectId, workTree.findUnstaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
    }

    @Test
    public void testInsertCollection() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        List<Node> targetList = new LinkedList<Node>();
        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                targetList, 3);

        assertEquals(3, targetList.size());

        Node ref1 = targetList.get(0);
        Node ref2 = targetList.get(1);
        Node ref3 = targetList.get(2);

        assertEquals(ref1.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(ref2.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(ref3.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP3)).get()
                .getObjectId());
    }

    @Test
    public void testInsertCollectionNullCollectionSize() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        List<Node> targetList = new LinkedList<Node>();
        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                targetList, null);

        assertEquals(3, targetList.size());

        Node ref1 = targetList.get(0);
        Node ref2 = targetList.get(1);
        Node ref3 = targetList.get(2);

        assertEquals(ref1.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(ref2.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(ref3.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP3)).get()
                .getObjectId());
    }

    @Test
    public void testInsertCollectionZeroCollectionSize() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        List<Node> targetList = new LinkedList<Node>();
        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                targetList, 0);

        assertEquals(3, targetList.size());

        Node ref1 = targetList.get(0);
        Node ref2 = targetList.get(1);
        Node ref3 = targetList.get(2);

        assertEquals(ref1.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(ref2.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(ref3.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP3)).get()
                .getObjectId());
    }

    @Test
    public void testInsertCollectionNegativeCollectionSize() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        List<Node> targetList = new LinkedList<Node>();

        exception.expect(IllegalArgumentException.class);
        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                targetList, -5);
    }

    @Test
    public void testInsertCollectionNoTarget() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, null);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
    }

    @Test
    public void testInsertDuplicateFeatures() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        ObjectId oID1 = workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId();

        List<Feature> modifiedFeatures = new LinkedList<Feature>();
        modifiedFeatures.add(points1_modified);

        workTree.insert(pointsName, modifiedFeatures.iterator(), false, new NullProgressListener(),
                null, 1);
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId()
                .equals(oID1));

    }

    @Test
    public void testUpdateFeatures() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        ObjectId oID1 = workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId();

        List<Feature> modifiedFeatures = new LinkedList<Feature>();
        modifiedFeatures.add(points1_modified);

        workTree.update(pointsName, modifiedFeatures.iterator(), new NullProgressListener(), 1);
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId()
                .equals(oID1));

    }

    @Test
    public void testUpdateFeaturesNullCollectionSize() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        ObjectId oID1 = workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId();

        List<Feature> modifiedFeatures = new LinkedList<Feature>();
        modifiedFeatures.add(points1_modified);

        workTree.update(pointsName, modifiedFeatures.iterator(), new NullProgressListener(), null);
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId()
                .equals(oID1));
    }

    @Test
    public void testUpdateFeaturesZeroCollectionSize() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        ObjectId oID1 = workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId();

        List<Feature> modifiedFeatures = new LinkedList<Feature>();
        modifiedFeatures.add(points1_modified);

        workTree.update(pointsName, modifiedFeatures.iterator(), new NullProgressListener(), 0);
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId()
                .equals(oID1));
    }

    @Test
    public void testUpdateFeaturesNegativeCollectionSize() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        List<Feature> modifiedFeatures = new LinkedList<Feature>();
        modifiedFeatures.add(points1_modified);

        exception.expect(IllegalArgumentException.class);
        workTree.update(pointsName, modifiedFeatures.iterator(), new NullProgressListener(), -5);
    }

    @Test
    public void testDeleteSingle() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        ObjectId oldTreeId = workTree.getTree().getId();

        workTree.delete(pointsName, idP2);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        ObjectId newTreeId = workTree.getTree().getId();

        assertFalse(oldTreeId.equals(newTreeId));
    }

    @Test
    public void testDeleteNonexistantFeature() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 2);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        ObjectId oldTreeId = workTree.getTree().getId();

        workTree.delete(pointsName, idP3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        ObjectId newTreeId = workTree.getTree().getId();

        assertTrue(oldTreeId.equals(newTreeId));

    }

    @Test
    public void testDeleteCollection() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        List<Feature> deleteFeatures = new LinkedList<Feature>();
        deleteFeatures.add(points1);
        deleteFeatures.add(points3);

        Name typeName = points1.getName();

        workTree.delete(typeName, null, deleteFeatures.iterator());

        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
    }

    @Test
    public void testDeleteCollectionOfFeaturesNotPresent() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        List<Feature> deleteFeatures = new LinkedList<Feature>();
        deleteFeatures.add(points3);

        Name typeName = points1.getName();

        workTree.delete(typeName, null, deleteFeatures.iterator());

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
    }

    @Test
    public void testDeleteFeatureType() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        featureList = new LinkedList<Feature>();
        featureList.add(lines1);
        featureList.add(lines2);
        featureList.add(lines3);

        workTree.insert(linesName, featureList.iterator(), false, new NullProgressListener(), null,
                3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL3)).isPresent());

        workTree.delete(new NameImpl(pointsName));

        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL3)).isPresent());
    }

    @Test
    public void testHasRoot() throws Exception {
        insert(points1);
        Name typeName = points1.getName();
        assertFalse(workTree.hasRoot(typeName));
    }

    @Test
    public void testGetUnstaged() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        assertEquals(3, workTree.countUnstaged(null));

        Iterator<DiffEntry> changes = workTree.getUnstaged(null);

        assertNotNull(changes);
    }

    @Test
    public void testInsertMultipleFeatureTypes() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        featureList = new LinkedList<Feature>();
        featureList.add(lines1);
        featureList.add(lines2);
        featureList.add(lines3);

        workTree.insert(linesName, featureList.iterator(), false, new NullProgressListener(), null,
                3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL3)).isPresent());

    }

    @Test
    public void testGetFeatureTypeNames() throws Exception {
        List<Feature> featureList = new LinkedList<Feature>();
        featureList.add(points1);
        featureList.add(points2);
        featureList.add(points3);

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        featureList = new LinkedList<Feature>();
        featureList.add(lines1);
        featureList.add(lines2);
        featureList.add(lines3);

        workTree.insert(linesName, featureList.iterator(), false, new NullProgressListener(), null,
                3);

        List<Name> featureTypes = workTree.getFeatureTypeNames();

        assertEquals(2, featureTypes.size());

        List<String> featureTypeNames = new LinkedList<String>();
        for (Name name : featureTypes) {
            featureTypeNames.add(name.getLocalPart());
        }

        assertTrue(featureTypeNames.contains(pointsName));
        assertTrue(featureTypeNames.contains(linesName));
    }

    @Test
    public void testCreateTypeTreeExisting() throws Exception {
        insert(points1);
        try {
            workTree.createTypeTree(pointsName, pointsType);
            fail("expected IAE on existing type tree");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("already exists"));
        }
    }

    @Test
    public void testCreateTypeTree() throws Exception {
        NodeRef treeRef = workTree.createTypeTree("points2", pointsType);
        assertNotNull(treeRef);
        assertEquals("points2", treeRef.path());
        assertEquals("", treeRef.getParentPath());
        assertTrue(treeRef.getNode().getMetadataId().isPresent());
        assertSame(treeRef.getMetadataId(), treeRef.getNode().getMetadataId().get());

        RevFeatureType featureType = repo.getIndex().getDatabase()
                .getFeatureType(treeRef.getMetadataId());
        assertEquals(pointsType, featureType.type());
    }

    @Test
    public void testCreateTypeNestedNonExistingParent() throws Exception {
        NodeRef treeRef = workTree.createTypeTree("path/to/nested/type", pointsType);
        assertNotNull(treeRef);
        assertEquals("path/to/nested/type", treeRef.path());
        assertEquals("path/to/nested", treeRef.getParentPath());
        assertTrue(treeRef.getNode().getMetadataId().isPresent());
        assertSame(treeRef.getMetadataId(), treeRef.getNode().getMetadataId().get());

        RevFeatureType featureType = repo.getIndex().getDatabase()
                .getFeatureType(treeRef.getMetadataId());
        assertEquals(pointsType, featureType.type());
    }

    @Test
    public void testCreateTypeTreeAutomaticallyWhenInsertingWitNoExistingTypeTree()
            throws Exception {

        insert(points1, points2);
        Optional<NodeRef> treeRef = repo.command(FindTreeChild.class).setChildPath(pointsName)
                .setIndex(true).setParent(workTree.getTree()).call();
        assertTrue(treeRef.isPresent());
        assertTrue(treeRef.get().getNode().getMetadataId().isPresent());
        assertFalse(treeRef.get().getNode().getMetadataId().get().isNull());

        RevFeatureType featureType = repo.getIndex().getDatabase()
                .getFeatureType(treeRef.get().getMetadataId());
        assertEquals(pointsType, featureType.type());

    }
}
