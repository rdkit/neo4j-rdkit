package org.rdkit.neo4j.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryMover {
  private static final Logger logger = LoggerFactory.getLogger(LibraryMover.class);

  public static void moveMissingLibraries(List<String> missingLibraries, String folder) {
    // Make new java.library.path
    File temporaryDir = createTempLibraryPath();

    final File jarFile = new File(LibraryLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    Map<String, InputStream> libraries;

    if(jarFile.isFile()) {  // Run with JAR file
      logger.info("Loading libraries from JAR");
      final JarFile jar = new JarFile(jarFile);
      libraries = getJarStreams(jar, folder);
      moveLibraries(libraries, temporaryDir);
      jar.close();
    } else { // Run with IDE
      logger.info("Loading libraries from IDE level");
      String realPath = getResourcePath(folder).toString();
      libraries = getIdeStreams(realPath);
      moveLibraries(libraries, temporaryDir);
    }
  }

  /**
   * Returns path to requested resource
   * @param resource - file to find Path to
   * @return Path
   */
  private static Path getResourcePath(String resource) throws URISyntaxException {
    final URL url = LibraryLoader.class.getClassLoader().getResource(resource);
    return Paths.get(url.toURI()).toAbsolutePath();
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

    OutputStream outputStream;
    byte buffer[] = new byte[4096];
    int bytes;

    for (Map.Entry<String, InputStream> pair: fileStreams.entrySet()) {
      logger.debug("Started copying {}", pair.getKey());
      File temp = new File(tempDirectory, pair.getKey());
      final InputStream iStream = pair.getValue();
      outputStream = new FileOutputStream(temp);

      while ((bytes = iStream.read(buffer)) > 0) {
        outputStream.write(buffer, 0, bytes);
      }
      iStream.close();
      outputStream.close();

      logger.debug("Finished copying {}", pair.getKey());
    }
  }

  /**
   * Method loads files inside jar from specified folder
   * @param jar - JarFile object
   * @param folder - appropriate resource folder
   * @return map of Filenames & InputStreams
   * @throws IOException if jarFile was specified wrong
   */
  private static Map<String,InputStream> getJarStreams(JarFile jar, String folder) throws IOException {
    TreeMap<String, InputStream> jarStreams = new TreeMap<>();
    Set<JarEntry> entriesSet = new HashSet<>();

    String name;
    JarEntry entry;

    /* Get ALL entries in .jar file */
    final Enumeration<JarEntry> entries = jar.entries();
    int items = 0;
    while(entries.hasMoreElements()) {
      entry = entries.nextElement();
      name = entry.getName();
      /* Find the folder with required pattern  */
      if (name.contains(folder)) {
        /* Process ONLY found directory and ignore other entries*/
        if (!entry.isDirectory()) {
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
   * @param folder - folder with native libraries
   * @return map of Filenames & InputStreams
   * @throws FileNotFoundException - if any
   */
  private static Map<String,InputStream> getIdeStreams(String folder) throws FileNotFoundException {
    Map<String, InputStream> ideStreams = new TreeMap<>();
    File files[] = (new File(folder)).listFiles();
    for (File lib: files) {
      String libName = lib.getName();
      InputStream iStream = new FileInputStream(lib);
      ideStreams.put(libName, iStream);
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
    File tempDir = Files.createTempDirectory("rdkit-binaries").toFile();
    tempDir.deleteOnExit();

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
