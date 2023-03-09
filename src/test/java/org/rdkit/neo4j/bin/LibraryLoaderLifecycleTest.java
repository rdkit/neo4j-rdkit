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

import org.RDKit.RDKFuncs;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class LibraryLoaderLifecycleTest {

    @Test
    public void testLoadingViaFiles() throws Exception {
        LibraryLoaderLifecycle.loadForTests();

        // this should fail if native libs are not loaded
        String propName = RDKFuncs.getComputedPropName();
        assertEquals("__computedProps", propName);
    }

    @Test
    @Ignore("in a normal build we don't have the jar file available yet")
    public void testJarFileSystem() throws IOException {
        final File jarFileOrDirectory = new File("target/rdkit-index-1.1.0-SNAPSHOT-4.1.jar");
        if (jarFileOrDirectory.isFile()) {
            Path tempDirectory = Files.createTempDirectory("rdkit-");

            FileSystem fs = FileSystems.newFileSystem(jarFileOrDirectory.toPath(), LibraryLoaderLifecycle.class.getClassLoader());
            Path folder = fs.getPath("/native", "linux.x86_64");
            DirectoryStream<Path> paths = Files.newDirectoryStream(folder);
            for (Path path: paths) {
                String fileName = path.getFileName().toString();
                File target = new File(tempDirectory.toFile(), fileName);
                Files.copy(path, target.toPath());
                System.load(target.getAbsolutePath());
            }
        }
    }
}
