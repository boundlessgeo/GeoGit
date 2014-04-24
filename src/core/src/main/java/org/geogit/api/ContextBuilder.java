/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.di.GeogitModule;
import org.geogit.repository.Hints;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

public class ContextBuilder {

    public final Context build() {
        return build(new Hints());
    }

    /**
     * @param hints a set of hints to pass over to the injector to be injected into components that
     *        can make use of it
     */
    public Context build(Hints hints) {
        return Guice.createInjector(new GeogitModule(), new HintsModule(hints)).getInstance(
                org.geogit.api.Context.class);
    }

    protected static class HintsModule extends AbstractModule {
        private Hints instance;

        public HintsModule(Hints instance) {
            this.instance = instance;

        }

        @Override
        protected void configure() {
            bind(Hints.class).toInstance(instance);
        }
    }

}
