package org.rdkit.neo4j.index.utils;

/*-
 * #%L
 * RDKit-Neo4j
 * %%
 * Copyright (C) 2019 RDKit
 * %%
 * Copyright (C) 2019 Evgeny Sorokin
 * @@ All Rights Reserved @@
 * This file is part of the RDKit Neo4J integration.
 * The contents are covered by the terms of the BSD license
 * which is included in the file LICENSE, found at the root
 * of the neo4j-rdkit source tree.
 * #L%
 */

import org.junit.After;
import org.junit.Before;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class BaseTest {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected List<String> defaultLabels = Arrays.asList("Chemical", "Structure");
    private DatabaseManagementService dbms;
    protected GraphDatabaseService graphDb;

    @Before
    public void createTestDatabase() {
        TestDatabaseManagementServiceBuilder builder = new TestDatabaseManagementServiceBuilder().impermanent();
        prepareDatabase(builder);
        dbms = builder.build();
        graphDb = dbms.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);
    }

    protected void prepareDatabase(TestDatabaseManagementServiceBuilder builder) {
        // intentionally empty, to be overridden in derived classes
    }

    @After
    public void destroyTestDatabase() {
        dbms.shutdown();
    }

    protected void insertChemblRows() throws Exception {
        graphDb.executeTransactionally("UNWIND $rows as row MERGE (from:Chemical:Structure {smiles: row.smiles, mol_id: row.mol_id})",
                Collections.singletonMap("rows", ChemicalStructureParser.getChemicalRows()));
    }
}
