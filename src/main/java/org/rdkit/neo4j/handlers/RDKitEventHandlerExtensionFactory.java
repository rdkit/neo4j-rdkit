package org.rdkit.neo4j.handlers;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.extension.ExtensionType;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.logging.internal.LogService;

import org.rdkit.neo4j.bin.LibraryLoader;
import org.rdkit.neo4j.bin.LoaderException;
import org.rdkit.neo4j.handlers.RDKitEventHandlerExtensionFactory.Dependencies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDKitEventHandlerExtensionFactory extends KernelExtensionFactory<Dependencies> {
  private static final Logger logger = LoggerFactory.getLogger(RDKitEventHandlerExtensionFactory.class);

  static {
    try {
      LibraryLoader.loadLibraries();
    } catch (LoaderException e) {
      logger.error("Unable to load native libraries: RDKit");
      e.printStackTrace();
    }
  }

  @Override
  public Lifecycle newInstance(KernelContext kernelContext, final Dependencies dependencies) {
    return new LifecycleAdapter() {
//      LogService log = dependencies.log();

      private RDKitEventHandler handler;

      @Override
      public void start() {
        logger.debug("Starting canonical smiles trigger watcher");
        handler = new RDKitEventHandler(dependencies.getGraphDatabaseService());
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

  public RDKitEventHandlerExtensionFactory() {
    super(ExtensionType.DATABASE, "canonicalSmilesEventHandler");
  }
}
