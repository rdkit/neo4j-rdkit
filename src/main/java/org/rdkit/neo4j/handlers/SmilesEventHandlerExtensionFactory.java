package org.rdkit.neo4j.handlers;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.extension.ExtensionType;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.logging.internal.LogService;

import org.rdkit.neo4j.handlers.SmilesEventHandlerExtensionFactory.Dependencies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmilesEventHandlerExtensionFactory extends KernelExtensionFactory<Dependencies> {
  private static final Logger logger = LoggerFactory.getLogger(SmilesEventHandlerExtensionFactory.class);

  @Override
  public Lifecycle newInstance(KernelContext kernelContext, final Dependencies dependencies) {
    return new LifecycleAdapter() {
//      LogService log = dependencies.log();

      private CanonicalSmilesEventHandler handler;

      @Override
      public void start() {
        logger.debug("Starting canonical smiles trigger watcher");
        System.out.println("STARTING trigger watcher");
        handler = new CanonicalSmilesEventHandler(dependencies.getGraphDatabaseService());
        dependencies.getGraphDatabaseService().registerTransactionEventHandler(handler);
      }

      @Override
      public void shutdown() {
        logger.debug("Stopping canonical smiles trigger watcher");
        dependencies.getGraphDatabaseService().unregisterTransactionEventHandler(handler);
      }
    };
  }

  interface Dependencies {
    GraphDatabaseService getGraphDatabaseService();
    LogService log();
  }

  public SmilesEventHandlerExtensionFactory() {
    super(ExtensionType.DATABASE, "canonicalSmilesEventHandler");
  }
}
