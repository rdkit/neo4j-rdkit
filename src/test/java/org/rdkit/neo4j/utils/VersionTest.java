package org.rdkit.neo4j.utils;

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

import org.RDKit.RDKFuncs;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.logging.NullLog;
import org.rdkit.neo4j.bin.LibraryLoaderLifecycle;

import static org.junit.Assert.assertEquals;

public class VersionTest {

    @BeforeClass
    public static void loadRdkit() throws Exception {
        new LibraryLoaderLifecycle(NullLog.getInstance()).init();
    }

    @Test
    public void testRdKitVersion() {
        assertEquals("2020.09.1", RDKFuncs.getRdkitVersion());
        assertEquals("Linux|5.4.0-51-generic|UNIX|GNU|64-bit", RDKFuncs.getRdkitBuild());
    }

}
