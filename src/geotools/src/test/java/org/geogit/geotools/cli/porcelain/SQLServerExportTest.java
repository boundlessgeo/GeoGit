/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli.porcelain;

import java.util.Arrays;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.porcelain.CommitOp;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.InvalidParameterException;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SQLServerExportTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private GeogitCLI cli;

    @Override
    public void setUpInternal() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        cli.setGeogit(geogit);

        // Add points
        insertAndAdd(points1);
        insertAndAdd(points2);
        insertAndAdd(points3);

        geogit.command(CommitOp.class).call();

        // Add lines
        insertAndAdd(lines1);
        insertAndAdd(lines2);
        insertAndAdd(lines3);

        geogit.command(CommitOp.class).call();
    }

    @Override
    public void tearDownInternal() throws Exception {
        cli.close();
    }

    @Test
    public void testExport() throws Exception {

        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList("Points", "Points");
        exportCommand.dataStoreFactory = TestHelper.createTestFactory();
        exportCommand.run(cli);
    }

    @Test
    public void testNullDataStore() throws Exception {
        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList("Points", "Points");
        exportCommand.dataStoreFactory = TestHelper.createNullTestFactory();
        exception.expect(CommandFailedException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testNoArgs() throws Exception {
        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList();
        exportCommand.dataStoreFactory = TestHelper.createNullTestFactory();
        exception.expect(CommandFailedException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportToTableThatExists() throws Exception {
        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList("Points", "table1");
        exportCommand.dataStoreFactory = TestHelper.createTestFactory();
        exception.expect(CommandFailedException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportToTableThatExistsWithOverwrite() throws Exception {
        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList("WORK_HEAD:Points", "testTable");
        exportCommand.dataStoreFactory = TestHelper.createTestFactory();
        exportCommand.run(cli);

        exportCommand.args = Arrays.asList("Lines", "testTable");
        exportCommand.overwrite = true;
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithNonexistentFeatureTypeTree() throws Exception {
        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList("invalidType", "invalidTable");
        exportCommand.dataStoreFactory = TestHelper.createTestFactory();
        exception.expect(InvalidParameterException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithNullTable() throws Exception {
        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList("Points", null);
        exportCommand.dataStoreFactory = TestHelper.createTestFactory();
        exception.expect(InvalidParameterException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithNullFeatureType() throws Exception {
        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList("", "invalidTable");
        exportCommand.dataStoreFactory = TestHelper.createTestFactory();
        exception.expect(InvalidParameterException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithEmptyStringForFeatureType() throws Exception {
        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList("", "invalidTable");
        exportCommand.dataStoreFactory = TestHelper.createTestFactory();
        exception.expect(InvalidParameterException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithEmptyStringForTable() throws Exception {
        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList("Points", "");
        exportCommand.dataStoreFactory = TestHelper.createTestFactory();
        exception.expect(InvalidParameterException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithFeatureNameInsteadOfType() throws Exception {
        SQLServerExport exportCommand = new SQLServerExport();
        exportCommand.args = Arrays.asList("Points/Points.1", "invalidTable");
        exportCommand.dataStoreFactory = TestHelper.createTestFactory();
        exception.expect(InvalidParameterException.class);
        exportCommand.run(cli);
    }

}