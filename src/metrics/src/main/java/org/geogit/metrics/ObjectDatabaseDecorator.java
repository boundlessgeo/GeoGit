package org.geogit.metrics;

import org.geogit.di.Decorator;
import org.geogit.storage.ForwardingObjectDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;

import com.google.inject.Provider;
import com.google.inject.util.Providers;

class ObjectDatabaseDecorator implements Decorator {

    @Override
    public boolean canDecorate(Object instance) {
        return instance instanceof ObjectDatabase && !(instance instanceof StagingDatabase);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I> I decorate(I subject) {
        Provider<ObjectDatabase> provider = Providers.of((ObjectDatabase) subject);
        return (I) new MetricsODB(provider);
    }

    private static class MetricsODB extends ForwardingObjectDatabase {

        public MetricsODB(Provider<? extends ObjectDatabase> odb) {
            super(odb);
        }

    }
}
