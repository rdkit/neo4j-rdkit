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

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Logging;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.rdkit.neo4j.procedures.SubstructureSearch;

import java.util.Map;

import static org.neo4j.graphdb.DependencyResolver.SelectionStrategy.FIRST;

public class TestUtils {
  public static Driver getDriver() {
    Config config = Config.builder()
        .withLogging( Logging.slf4j() )
        .build();
    String uri = "bolt://localhost:7687";
    return GraphDatabase.driver(uri, AuthTokens.basic("neo4j", "test"), config);
  }

  public static Map<String, Object> getFirstRow(Result result) {
    return result.next();
  }

  public static void registerProcedures(GraphDatabaseService db, Class... classes) {
    Procedures proceduresService = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency(Procedures.class);
    try {
      for (Class clazz: classes) {
        proceduresService.registerProcedure(clazz, true);
        proceduresService.registerFunction(clazz, true);
      }

    } catch (KernelException e) {
      throw new RuntimeException(e);
    }
  }
}
