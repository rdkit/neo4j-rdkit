package org.rdkit.neo4j.bin;

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

import org.apache.commons.io.FileUtils;
import org.neo4j.internal.helpers.collection.Iterables;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.logging.Log;
import org.neo4j.logging.NullLog;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * kernel extension to load native libs
 */
public class LibraryLoaderLifecycle extends LifecycleAdapter {

  private static final String OS_LINUX = "linux";
  private static final String OS_MACOSX = "macosx";
  private static final String OS_WIN32 = "win32";
  private static final String ARCH_X86_64 = "x86_64";
  private static final String ARCH_X86 = "x86";

  private Log log;
  private Path tempDirectory;

  public LibraryLoaderLifecycle(Log userLog) {
    this.log = userLog;
  }

  @Override
  public void init() throws Exception {
    loadNativeLibraries();
  }

  @Override
  public void shutdown() throws Exception {
    if (tempDirectory!= null) {
      FileUtils.deleteDirectory(tempDirectory.toFile());
    }
  }

  private void loadNativeLibraries() throws IOException {
    final String platform = getPlatform();

    final File jarFileOrDirectory = new File(LibraryLoaderLifecycle.class.getProtectionDomain().getCodeSource().getLocation().getPath());

    List<String> librariesToLoad = jarFileOrDirectory.isFile() ?
            copyNativeLibsFromJar(platform, jarFileOrDirectory) :
            getNativeLibsFromLocalDirectory(platform);
    librariesToLoad.forEach(filename -> {
      log.info("trying to load native library " + filename);
      System.load(filename);
    });
  }

  private List<String> getNativeLibsFromLocalDirectory(String platform) throws IOException {
    log.info("Loading libraries from local directories");
    try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("native", platform))) {
      return Iterables.stream(paths).map(Path::toFile).map(File::getAbsolutePath).collect(Collectors.toList());
    }
  }

  private List<String> copyNativeLibsFromJar(String platform, File jarFileOrDirectory) throws IOException {
    log.info("Loading libraries from JAR");
    tempDirectory = Files.createTempDirectory("rdkit-");
    try (FileSystem fs = FileSystems.newFileSystem(jarFileOrDirectory.toPath(), LibraryLoaderLifecycle.class.getClassLoader())) {
      Path folder = fs.getPath("/native", platform);
      try (DirectoryStream<Path> paths = Files.newDirectoryStream(folder)) {

        return Iterables.stream(paths)
                .map(path -> {
                  try {
                    String fileName = path.getFileName().toString();
                    File target = new File(tempDirectory.toFile(), fileName);
                    Files.copy(path, target.toPath());
                    return target.getAbsolutePath();
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                })
                .collect(Collectors.toList());
      }
    }
  }

  private String getPlatform() {
    String osname = System.getProperty("os.name");
    osname = osname.toLowerCase();

    String formattedOs;
    if (osname.contains("linux")) {
      formattedOs = OS_LINUX;
    } else if (osname.contains("mac")) {
      formattedOs = OS_MACOSX;
    } else if (osname.contains("windows")) {
      formattedOs = OS_WIN32;
    } else {
      throw new IllegalArgumentException("Could not determine the system parameters properly " + osname);
    }

    String arch = System.getProperty("os.arch");
    arch = arch.toLowerCase().endsWith("64") ? ARCH_X86_64 : ARCH_X86;

    return String.format("%s.%s", formattedOs, arch);
  }

  public static void loadForTests() {
    try {
      new LibraryLoaderLifecycle(NullLog.getInstance()).init();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
