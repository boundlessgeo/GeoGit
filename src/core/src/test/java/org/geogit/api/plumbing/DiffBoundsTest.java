/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.NodeRef;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.api.porcelain.FeatureNodeRefFromRefspec;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;

import com.google.common.base.Suppliers;
import com.vividsolutions.jts.geom.Envelope;

public class DiffBoundsTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        populate(true, points1);
        insertAndAdd(points1_modified);
        geogit.command(CommitOp.class).call();
    }

    @Test
    public void testDiffBetweenDifferentTrees() {

        Iterator<DiffEntry> entries = geogit.command(DiffOp.class).setOldVersion("HEAD^")
                .setNewVersion("HEAD").call();

        List<DiffEntry> entriesList = new ArrayList<DiffEntry>();

        DiffEntry entry;

        while (entries.hasNext()) {
            entry = entries.next();
            entriesList.add(entry);
        }

        Envelope diffBoundsEnvelope = geogit.command(DiffBounds.class).setDiffEntries(entriesList)
                .computeDiffBounds();

        System.out.println(diffBoundsEnvelope);

    }

    @Test
    public void testDiffBetweenIdenticalTrees() {


    	 Iterator<DiffEntry> entries = geogit.command(DiffOp.class).setOldVersion("HEAD").setNewVersion("HEAD").call();
         
         List<DiffEntry> entriesList = new ArrayList<DiffEntry>();
         
         DiffEntry entry;
         
         while(entries.hasNext()){
      	   entry = entries.next();
      	   entriesList.add(entry);
         }

         Envelope diffBoundsEnvelope = geogit.command(DiffBounds.class)
          					.setDiffEntries(entriesList)
          					.computeDiffBounds();
         				        
        assertTrue(diffBoundsEnvelope.isNull());


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
