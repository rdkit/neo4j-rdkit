package org.rdkit.neo4j.bin;

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

import org.neo4j.annotations.service.ServiceProvider;
import org.neo4j.kernel.extension.ExtensionFactory;
import org.neo4j.kernel.extension.ExtensionType;
import org.neo4j.kernel.extension.context.ExtensionContext;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.logging.internal.LogService;

@ServiceProvider
public class LibraryLoaderExtensionFactory extends ExtensionFactory<LibraryLoaderExtensionFactory.Dependencies> {

  public LibraryLoaderExtensionFactory() {
    super(ExtensionType.GLOBAL, "rdkitlibraryloader");
  }

  @Override
  public Lifecycle newInstance(ExtensionContext context, Dependencies dependencies) {
    return new LibraryLoaderLifecycle(dependencies.log().getUserLog(LibraryLoaderLifecycle.class));
  }

  interface Dependencies {
    LogService log();
  }
}
