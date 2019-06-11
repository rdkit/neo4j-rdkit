package org.rdkit.neo4j.bin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

import java.nio.file.Files;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import java.util.stream.Collectors;
import lombok.val;
import org.rdkit.neo4j.exceptions.LoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryMover {
  private static final Logger logger = LoggerFactory.getLogger(LibraryMover.class);

  /**
   * Method resolves missing libraries: initializes temp dir, filters already present and moves the rest
   * @param missingLibraries to detect/move
   * @param platform where to take libraries from
   * @throws LoaderException if unable to initialize temp dir or unable to move libraries
   */
  public static void resolveMissingLibraries(List<String> missingLibraries, String platform) throws LoaderException {
    // Make new java.library.path
    File temporaryDir;
    try {
      temporaryDir = LibraryMover.createTempLibraryPath();
    } catch (Exception e) {
      logger.error("Unable to initilize temp folder");
      throw new LoaderException("Unable to initialize temp folder", e);
    }

    val tempFolderLibs = LibraryLoader.getLibrariesInFolder(temporaryDir.getAbsolutePath(), missingLibraries);
    logger.warn("Libraries present in temp folder: {}", tempFolderLibs);
    missingLibraries = missingLibraries.stream()
        .filter(x -> !tempFolderLibs.contains(x))
        .collect(Collectors.toList());

    if (missingLibraries.size() > 0) {
      LibraryMover.moveMissingLibraries(missingLibraries, platform, temporaryDir);
    }
  }

  public static void moveMissingLibraries(List<String> missingLibraries, String fromFolder, File temporaryDir) throws LoaderException {
    final File jarFile = new File(LibraryLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    final Map<String, InputStream> libStreams;

    if(jarFile.isFile()) {  // Run with JAR file
      logger.info("Loading libraries from JAR");
      try (final JarFile jar = new JarFile(jarFile)) {
        libStreams = getJarStreams(jar, missingLibraries, fromFolder);
        moveLibraries(libStreams, temporaryDir);
      } catch (IOException e) {
        logger.error("Exception occured during native libraries extraction from JAR");
        throw new LoaderException("Exception during extraction from JAR", e);
      }
    } else { // Run with IDE
      try {
        logger.info("Loading libraries from IDE level");
        libStreams = getIdeStreams(missingLibraries, fromFolder);
        moveLibraries(libStreams, temporaryDir);
      } catch (IOException e) {
        logger.info("Exception occured during native libraries extraction from folders");
        throw new LoaderException("Exception during extraction from folders", e);
      }
    }
  }

  /**
   * Returns path to requested resource
   * @param resource - file to find Path to
   * @return Path
   */
  private static InputStream getNativeAsStream(String resource) {
    try {
      return new FileInputStream(String.format("native/%s", resource));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * * <p>Method moves library from .jar to temporary folder and loads it</p>
   * Temp folder previously should be added to java.library.path
   *
   * @param fileStreams - map of <filename, inputStream> pairs, used for interoperability between loading from JAR and loading from IDE
   * @param tempDirectory - temp directory for libraries
   * @throws FileNotFoundException - in case of troubleshoot with temporary folder.
   * @throws IOException - in case of incorrect InputStreams or inproper `temp` file created
   */
  private static void moveLibraries(Map<String, InputStream> fileStreams, File tempDirectory) throws FileNotFoundException, IOException {
    if (tempDirectory == null || !tempDirectory.exists())
      throw new FileNotFoundException("Temporary directory was not created");

    byte buffer[] = new byte[4096];
    int bytes;

    for (Map.Entry<String, InputStream> pair: fileStreams.entrySet()) {
      logger.debug("Started copying {}", pair.getKey());
      File temp = new File(tempDirectory, pair.getKey());
      try (OutputStream outputStream = new FileOutputStream(temp)) {
        final InputStream iStream = pair.getValue();

        while ((bytes = iStream.read(buffer)) > 0) {
          outputStream.write(buffer, 0, bytes);
        }
        iStream.close();
      }

      logger.debug("Finished copying {}", pair.getKey());
    }
  }

  /**
   * Method loads files inside jar from specified folder
   * @param jar - JarFile object
   * @param missingLibraries
   * @param folder - appropriate resource folder
   * @return map of Filenames & InputStreams
   * @throws IOException if jarFile was specified wrong
   */
  private static Map<String,InputStream> getJarStreams(JarFile jar,
      List<String> missingLibraries, String folder) throws IOException {
    TreeMap<String, InputStream> jarStreams = new TreeMap<>();
    Set<JarEntry> entriesSet = new HashSet<>();

    JarEntry entry;

    /* Get ALL entries in .jar file */
    final Enumeration<JarEntry> entries = jar.entries();
    while(entries.hasMoreElements()) {
      entry = entries.nextElement();
      final String name = entry.getName();
      /* Find the folder with required pattern  */
      if (name.contains(folder)) {
        /* Process ONLY found directory and ignore other entries*/
        if (!entry.isDirectory() && missingLibraries.stream().anyMatch(name::contains)) {
          entriesSet.add(entry);
        }
      }
    }

    /* Transform JarEntries into pairs of Filename & InputStream to that file */
    for (final JarEntry libFile: entriesSet) {
      String filename = libFile.getName();
      filename = filename.substring(filename.lastIndexOf('/') + 1);
      InputStream iStream = jar.getInputStream(libFile);
      jarStreams.put(filename, iStream);
    }

    return jarStreams;
  }


  /**
   * Method loads files from specified folder
   *
   * @param missingLibraries list of filenames+extension to be streamed
   * @param folder - folder with native libraries
   * @return map of Filenames & InputStreams
   */
  private static Map<String,InputStream> getIdeStreams(List<String> missingLibraries,
      String folder) {
    Map<String, InputStream> ideStreams = new TreeMap<>();
    for (String libraryName: missingLibraries) {
      final String path = String.format("%s/%s", folder, libraryName);
      final InputStream iStream = getNativeAsStream(path);
      ideStreams.put(libraryName, iStream);
    }

    return ideStreams;
  }

  /**
   * Method for updating java.library.path
   * Method for updating the java.library.path with a new temp folder for native libraries
   * System.setProperty(path) does not make any sense because JVM sets it during initialization
   * Altering library path requires additional code
   *
   * @return temporary folder file
   */
  private static File createTempLibraryPath() throws NoSuchFieldException, IllegalAccessException, IOException {
    File tempDir;
    String osName = System.getProperty("os.name");
    if (osName.toLowerCase().startsWith("windows")) {
      tempDir = new File(System.getenv("USERPROFILE") + "\\AppData\\Local\\rdkit\\native");
      tempDir.mkdirs();
    } else {
      // todo: analogue solution for *nix systems (I do not want to add dependency on guava only for this)
      tempDir = Files.createTempDirectory("rdkit-binaries").toFile();
      tempDir.deleteOnExit();
    }

    logger.info("Using temp dir: {}", tempDir.getPath());
    System.setProperty("java.io.tmpdir", tempDir.getPath());

    String absPath = tempDir.toString();
    addLibraryPath(absPath);
    logger.debug("Successfully added temp folder to java.library.path [{}]", absPath);

    return tempDir;
  }

  /**
   * Adds the specified path to the java library path without reinitialization of a variable
   *
   * @param pathToAdd the path to add
   * @throws Exception if any
   */
  private static void addLibraryPath(String pathToAdd)
      throws NoSuchFieldException, IllegalAccessException {
    final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
    usrPathsField.setAccessible(true);

    //get array of paths
    final String[] paths = (String[]) usrPathsField.get(null);

    //check if the path to add is already present
    for (String path : paths) {
      if (path.equals(pathToAdd)) {
        return;
      }
    }

    //add the new path
    final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
    newPaths[newPaths.length - 1] = pathToAdd;
    usrPathsField.set(null, newPaths);
  }
}
