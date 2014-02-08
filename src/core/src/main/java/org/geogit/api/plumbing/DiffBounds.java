/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffObjectCount;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Computes the bounds of the difference between the two trees instead of the actual diffs.
 * 
 */

public class DiffBounds extends AbstractGeoGitOp<DiffObjectCount> {

    private Iterable<DiffEntry> entries;

    @Override
    public DiffObjectCount call() {
        // TODO Auto-generated method stub
        return null;
    }

    public DiffBounds() {

    }

    // constructor to initialize the entries
    public DiffBounds(Iterable<DiffEntry> entries) {
        this.entries = entries;
    }

    /**
     * 
     * @param entries - the list of diff- entries
     * @return {@code this}
     */
    public DiffBounds setDiffEntries(Iterable<DiffEntry> entries) {
        this.entries = entries;
        return this;
    }

    /**
     * 
     * @param entries - A list containing the DiffEntries
     * @return Envelope - representing the final bounds
     */
    public Envelope computeDiffBounds() {

        Envelope boundsEnvelope = new Envelope();
        boundsEnvelope.setToNull();

        Envelope oldEnvelope = new Envelope();
        Envelope newEnvelope = new Envelope();

        // create a list of envelopes using the entries list
        for (DiffEntry entry : entries) {

            if (entry.getOldObject() != null) {
                entry.getOldObject().expand(oldEnvelope);
            }

            if (entry.getNewObject() != null) {
                entry.getNewObject().expand(newEnvelope);
            }

            if (!oldEnvelope.equals(newEnvelope)) {
                entry.getOldObject().expand(boundsEnvelope);
                entry.getNewObject().expand(boundsEnvelope);
            }
        }

        return boundsEnvelope;

    }
}
