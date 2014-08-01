/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.locationtech.geogig.api.plumbing;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.locationtech.geogig.api.AbstractGeoGigOp;
import org.locationtech.geogig.api.GeogigTransaction;
import org.locationtech.geogig.api.hooks.Hookable;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * Creates a new {@link GeogigTransaction} and copies all of the repository refs for that
 * transaction to use.
 * 
 * @see GeogigTransaction
 */
@Hookable(name = "transaction-start")
public class TransactionBegin extends AbstractGeoGigOp<GeogigTransaction> {

    /**
     * Creates a new transaction and returns it.
     * 
     * @return the {@link GeogigTransaction} that was created by the operation
     */
    @Override
    protected GeogigTransaction _call() {
        Preconditions.checkState(!(context instanceof GeogigTransaction),
                "Cannot start a new transaction within a transaction!");

        GeogigTransaction t = new GeogigTransaction(context, UUID.randomUUID());

        // Lock the repository
        try {
            refDatabase().lock();
        } catch (TimeoutException e) {
            Throwables.propagate(e);
        }
        try {
            // Copy original refs
            t.create();
        } finally {
            // Unlock the repository
            refDatabase().unlock();
        }
        // Return the transaction
        return t;
    }
}