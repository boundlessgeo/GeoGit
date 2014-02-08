/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import org.geogit.api.NodeRef;
import org.geogit.api.porcelain.FeatureNodeRefFromRefspec;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;

import com.google.common.base.Suppliers;

public class DiffBoundsTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        populate(true, points1);
        insert(points1_modified);
    }

    @Test
    public void testDiffBetweenDifferentTrees() {
        NodeRef oldRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec("HEAD:" + NodeRef.appendChild(polyName, idPG1)).call().orNull();
        NodeRef newRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec(NodeRef.appendChild(polyName, idPG2)).call().orNull();
        

    }

    @Test
    public void testDiffBetweenIdenticalTrees() {
        NodeRef oldRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec(NodeRef.appendChild(pointsName, idP1)).call().orNull();
        NodeRef newRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec(NodeRef.appendChild(pointsName, idP1)).call().orNull();

        assertNull(diffBoundsEnvelope);
        System.out.println(diffBoundsEnvelope);
    }

    @Test
    public void testDiffUnexistentFeature() {
        try {
            NodeRef oldRef = geogit.command(FeatureNodeRefFromRefspec.class)
                    .setRefspec(NodeRef.appendChild(pointsName, "Points.100")).call().orNull();
            NodeRef newRef = geogit.command(FeatureNodeRefFromRefspec.class)
                    .setRefspec(NodeRef.appendChild(pointsName, idP1)).call().orNull();
            geogit.command(DiffFeature.class).setOldVersion(Suppliers.ofInstance(oldRef))
                    .setNewVersion(Suppliers.ofInstance(newRef)).call();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testDiffWrongPath() {
        try {
            NodeRef oldRef = geogit.command(FeatureNodeRefFromRefspec.class).setRefspec(pointsName)
                    .call().orNull();
            NodeRef newRef = geogit.command(FeatureNodeRefFromRefspec.class)
                    .setRefspec(NodeRef.appendChild(pointsName, idP1)).call().orNull();
            geogit.command(DiffFeature.class).setOldVersion(Suppliers.ofInstance(oldRef))
                    .setNewVersion(Suppliers.ofInstance(newRef)).call();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

}
