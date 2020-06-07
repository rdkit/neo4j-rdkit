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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.junit.Assert.*;

public class RDKitSettingsTest {

    @Test
    public void testDefaultConfig() {
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                .newGraphDatabase();

        Config config = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency(Config.class);
        assertTrue(config.get(RDKitSettings.indexSanitize));
        db.shutdown();
    }

    @Test
    public void testSanitizeFalse() {
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                .setConfig(RDKitSettings.indexSanitize, "false")
                .newGraphDatabase();

        Config config = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency(Config.class);
        assertFalse(config.get(RDKitSettings.indexSanitize));
        db.shutdown();
    }

}
