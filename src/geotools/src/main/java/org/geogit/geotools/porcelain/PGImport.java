/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import java.net.ConnectException;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ImportOp;
import org.geotools.data.DataStore;
import org.opengis.util.ProgressListener;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Imports one or more tables from a PostGIS database.
 * 
 * PostGIS CLI proxy for {@link ImportOp}
 * 
 * @see ImportOp
 */
@Parameters(commandNames = "import", commandDescription = "Import PostGIS database")
public class PGImport extends AbstractPGCommand implements CLICommand {

    /**
     * If this is set, only this table will be imported.
     */
    @Parameter(names = { "--table", "-t" }, description = "Table to import.")
    public String table = "";

    /**
     * If this is set, all tables will be imported.
     */
    @Parameter(names = "--all", description = "Import all tables.")
    public boolean all = false;

    /**
     * do not overwrite features if they exist
     */
    @Parameter(names = { "-n", "--no-overwrite" }, description = "Do not overwrite features if they exist")
    boolean noOverwrite;

    /**
     * Force adding features, even if their feature type do not match the default one of the
     * destination path
     */
    @Parameter(names = { "-f", "--force" }, description = "Force adding features, even if their feature type do not match the default one of the destination path")
    boolean force;

    /**
     * Set the path default feature type to the the feature type of imported features, and modify
     * existing features to match it
     */
    @Parameter(names = { "-a", "--alter" }, description = "Set the path default feature type to the the feature type of imported features, and modify existing features to match it")
    boolean alter;

    /**
     * Destination path to add features to. Only allowed when importing a single table
     */
    @Parameter(names = { "-d", "--dest" }, description = "Path to import to")
    String destTable;

    /**
     * Executes the import command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractPGCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }

        DataStore dataStore = null;
        try {
            dataStore = getDataStore();
        } catch (ConnectException e) {
            cli.getConsole().println("Unable to connect using the specified database parameters.");
            cli.getConsole().flush();
            return;
        }

        try {
            cli.getConsole().println("Importing from database " + commonArgs.database);

            ProgressListener progressListener = cli.getProgressListener();
            cli.getGeogit().command(ImportOp.class).setAll(all).setTable(table).setAlter(alter)
                    .setForce(force).setDestinationPath(destTable).setOverwrite(!noOverwrite)
                    .setDataStore(dataStore).setProgressListener(progressListener).call();

            cli.getConsole().println("Import successful.");

        } catch (GeoToolsOpException e) {
            switch (e.statusCode) {
            case TABLE_NOT_DEFINED:
                cli.getConsole().println(
                        "No tables specified for import. Specify --all or --table <table>.");
                break;
            case ALL_AND_TABLE_DEFINED:
                cli.getConsole().println("Specify --all or --table <table>, both cannot be set.");
                break;
            case NO_FEATURES_FOUND:
                cli.getConsole().println("No features were found in the database.");
                break;
            case TABLE_NOT_FOUND:
                cli.getConsole().println("Could not find the specified table.");
                break;
            case UNABLE_TO_GET_NAMES:
                cli.getConsole().println("Unable to get feature types from the database.");
                break;
            case UNABLE_TO_GET_FEATURES:
                cli.getConsole().println("Unable to get features from the database.");
                break;
            case UNABLE_TO_INSERT:
                cli.getConsole().println("Unable to insert features into the working tree.");
                break;
            case UNCOMPATIBLE_FEATURE_TYPE:
                cli.getConsole().println(
                        "Uncompatible feature type. Use --alter or --force options to import.");
                break;
            case ALTER_AND_FORCE_DEFINED:
                cli.getConsole().println("Specify --alter or --force, both cannot be set.");
                break;
            default:
                cli.getConsole().println("Import failed with exception: " + e.statusCode.name());
            }
        } finally {
            dataStore.dispose();
            cli.getConsole().flush();
        }
    }
}
