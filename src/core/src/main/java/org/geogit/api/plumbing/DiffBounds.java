/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
    public DiffBounds(List<DiffEntry> entries) {
        entries = new ArrayList<DiffEntry>(entries);
    }

    /**
     * 
     * @param entries - the list of diff- entries
     * @return {@code this}
     */
    public DiffBounds setDiffEntries(List<DiffEntry> entries) {
        this.entries = new ArrayList<DiffEntry>(entries);
        return this;
    }

    /**
     * 
     * @param entries - A list containing the DiffEntries
     * @return Envelope - representing the final bounds
     */
    public Envelope getDiffBounds() {

        List<Envelope> envelopeList = new ArrayList<Envelope>();

        // create a list of envelopes using the entries list
        for (DiffEntry entry : entries) {
            Envelope oldEnvelope = new Envelope();
            Envelope newEnvelope = new Envelope();
            entry.getOldObject().expand(oldEnvelope);
            entry.getNewObject().expand(newEnvelope);
            envelopeList.add(oldEnvelope);
        }

        if (envelopeList.size() >= 1) {
            Envelope firstEnvelope = envelopeList.get(0);
            ListIterator<Envelope> envIterator = envelopeList.listIterator(0);

            // Check if all are overlapping?
            boolean same = true;
            while (same && envIterator.hasNext()) {
                if (firstEnvelope.equals(envIterator.next()))
                    same = true;
                else
                    same = false;
            }

            if (same)
                return firstEnvelope;
            else {
                ListIterator<Envelope> newEnvIterator = envelopeList.listIterator(1);
                Envelope currEnvelope;
                double maxX = firstEnvelope.getMaxX();
                double maxY = firstEnvelope.getMaxY();
                double minX = firstEnvelope.getMinX();
                double minY = firstEnvelope.getMinY();

                while (newEnvIterator.hasNext()) {

                    currEnvelope = newEnvIterator.next();

                    if (currEnvelope.getMaxX() > maxX)
                        maxX = currEnvelope.getMaxX();
                    if (currEnvelope.getMaxY() > maxY)
                        maxY = currEnvelope.getMaxY();
                    if (currEnvelope.getMinX() < minX)
                        minX = currEnvelope.getMinX();
                    if (currEnvelope.getMinY() < minY)
                        minY = currEnvelope.getMinY();

                }
                return new Envelope(maxX, minX, maxY, minY);
            }
        }

        else
            return new Envelope();

    }
}
