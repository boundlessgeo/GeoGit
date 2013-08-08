/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.gui.cli;

import org.geogit.cli.CLIModule;

import com.google.inject.AbstractModule;

/**
 * @see Map
 */
public class GuiCommandsModule extends AbstractModule implements CLIModule {

    @Override
    protected void configure() {
        bind(Map.class);
    }

}
