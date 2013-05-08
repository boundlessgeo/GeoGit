package org.geogit.geotools.data;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.geogit.api.GeoGIT;
import org.geogit.api.GeogitTransaction;
import org.geogit.api.Remote;
import org.geogit.api.plumbing.TransactionBegin;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.PullOp;
import org.geogit.api.porcelain.PushOp;
import org.geogit.api.porcelain.RemoteListOp;
import org.geogit.api.porcelain.SynchronizationException;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

public class GeogitSynchronizationHandler {

    private ScheduledExecutorService executor = null;

    private static GeogitSynchronizationHandler instance = new GeogitSynchronizationHandler();

    private final Queue<Pair<GeoGIT, Optional<String>>> repositories;

    private GeogitSynchronizationHandler() {
        repositories = new ConcurrentLinkedQueue<Pair<GeoGIT, Optional<String>>>();

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new GeoGitSynchronizer(), 0, 1, TimeUnit.SECONDS);
    }

    public void setDirty(GeoGIT geogit, @Nullable String branch) {
        ImmutableList<Remote> remotes = geogit.command(RemoteListOp.class).call();
        if (remotes.size() > 0) {
            Pair<GeoGIT, Optional<String>> entry = new Pair<GeoGIT, Optional<String>>(geogit,
                    Optional.fromNullable(branch));
            if (!repositories.contains(entry)) {
                repositories.add(entry);
            }
        }
    }

    public static GeogitSynchronizationHandler get() {
        return instance;
    }

    private class GeoGitSynchronizer implements Runnable {
        public void run() {
            if (repositories.peek() != null) {
                Pair<GeoGIT, Optional<String>> repo = repositories.poll();

                final String workingBranch = repo.getSecond().orNull();

                ImmutableList<Remote> remotes = repo.first.command(RemoteListOp.class).call();
                Iterator<Remote> remoteIter = remotes.iterator();
                while (remoteIter.hasNext()) {
                    GeogitTransaction geogitTx = repo.getFirst().command(TransactionBegin.class)
                            .call();
                    try {
                        if (workingBranch != null) {
                            geogitTx.command(CheckoutOp.class).setForce(true)
                                    .setSource(workingBranch).call();
                        }
                        Optional<Remote> remote = Optional.of(remoteIter.next());
                        PullOp pull = geogitTx.command(PullOp.class).setRemote(
                                Suppliers.ofInstance(remote));
                        if (workingBranch != null) {
                            pull.addRefSpec(workingBranch);
                        } else {
                            pull.setAll(true);
                        }
                        pull.call();

                        PushOp push = geogitTx.command(PushOp.class).setRemote(
                                Suppliers.ofInstance(remote));
                        if (workingBranch != null) {
                            push.addRefSpec(workingBranch);
                        } else {
                            push.setAll(true);
                        }
                        try {
                            push.call();
                        } catch (SynchronizationException e) {
                            // Do nothing
                        }

                        geogitTx.commitSyncTransaction();
                        geogitTx = null;
                    } catch (RuntimeException e) {
                        // Do not stop trying to sync.
                        geogitTx.abort();
                        repositories.add(repo);
                    }
                }
            }
        }
    }

    /**
     * Provides a basic implementation for a pair object.
     */
    private class Pair<F, S> {

        private final F first;

        private final S second;

        /**
         * Constructs a new {@code Pair} with the given objects.
         * 
         * @param first the first object
         * @param second the second object
         */
        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        /**
         * @return the first object
         */
        public F getFirst() {
            return first;
        }

        /**
         * @return the second object
         */
        public S getSecond() {
            return second;
        }

        /**
         * @return the hash code for this pair
         */
        @Override
        public int hashCode() {
            return first.hashCode() ^ second.hashCode();
        }

        /**
         * Compares this pair to another pair.
         * 
         * @param o the pair to compare to
         * @return true if the pairs' objects are equal
         */
        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (!(o instanceof Pair))
                return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return this.first.equals(pair.getFirst()) && this.second.equals(pair.getSecond());
        }

    }
}
