/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffObjectCount;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Computes the bounds of the difference between the two trees instead of the actual diffs.
 * 
 */

public class DiffBounds extends AbstractGeoGitOp<DiffObjectCount> {

    private List<DiffEntry> entries;

    @Override
    public DiffObjectCount call() {
        // TODO Auto-generated method stub
        return null;
    }

    // constructor to initialize the entries
    public DiffBounds(List<DiffEntry> inputEntries) {
        entries = new ArrayList<DiffEntry>(inputEntries);
    }

    /**
     * 
     * @param entries - A list containing the DiffEntries
     * @return Envelope - It represents the final bounds
     */
    public Envelope getDiffBounds() {

        List<Envelope> diffBounds = new ArrayList<Envelope>();
        List<Envelope> envelopeList = new ArrayList<Envelope>();
        
        for (DiffEntry entry : entries) {
            Envelope oldEnvelope = new Envelope();
            entry.expand(oldEnvelope);
            envelopeList.add(oldEnvelope);
        }

        double maxX, maxY, minX, minY;

        for (Envelope currEnvelope : envelopeList) {

        }

        return new Envelope();

    }

}
