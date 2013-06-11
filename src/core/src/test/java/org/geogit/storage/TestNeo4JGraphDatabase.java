/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.api.Platform;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.google.inject.Inject;

public class TestNeo4JGraphDatabase extends Neo4JGraphDatabase {

    @Inject
    public TestNeo4JGraphDatabase(Platform platform) {
        super(platform);
    }

    @Override
    protected GraphDatabaseService getGraphDatabase() {
        return new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    }

    @Override
    protected void destroyGraphDatabase() {
        graphDB.shutdown();
        databaseServices.remove(dbPath);
    }
}
