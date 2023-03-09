package org.rdkit.neo4j.config;

/*-
 * #%L
 * RDKit-Neo4j plugin
 * %%
 * Copyright (C) 2019 - 2020 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

import org.junit.Test;
import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RDKitSettingsTest {

    @Test
    public void testDefaultConfig() {
        DatabaseManagementService dbms = new TestDatabaseManagementServiceBuilder().impermanent().build();
        GraphDatabaseAPI db = (GraphDatabaseAPI) dbms.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);

        Config config = db.getDependencyResolver().resolveDependency(Config.class);
        assertTrue(config.get(RDKitSettings.indexSanitize));

        dbms.shutdown();
    }

    @Test
    public void testSanitizeFalse() {
        DatabaseManagementService dbms = new TestDatabaseManagementServiceBuilder()
                .setConfig(RDKitSettings.indexSanitize, false)
                .impermanent().build();
        GraphDatabaseAPI db = (GraphDatabaseAPI) dbms.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);

        Config config = db.getDependencyResolver().resolveDependency(Config.class);
        assertFalse(config.get(RDKitSettings.indexSanitize));

        dbms.shutdown();
    }

}
