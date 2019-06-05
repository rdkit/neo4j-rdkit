package org.rdkit.neo4j.bin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.val;
import org.rdkit.neo4j.exceptions.LoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class loads native libraries depending on os & arch
 */
public class LibraryLoader {

  private static final Logger logger = LoggerFactory.getLogger(LibraryLoader.class);

  /**
   * List of libraries to be loaded for different operating systems (lib order is important).
   */
  private static final Map<String, String[]> LIBRARIES = new HashMap<>();
  private static final String OS_LINUX = "linux";
  private static final String OS_MACOSX = "macosx";
  private static final String OS_WIN32 = "win32";
  private static final String ARCH_X86_64 = "x86_64";
  private static final String ARCH_X86 = "x86";

  /**
   * We define here what libraries are necessary to run the RDKit for the
   * different supported platforms.
   */
  static {
    LIBRARIES.put(OS_WIN32 + "." + ARCH_X86,
        new String[]{"boost_serialization-vc140-mt-1_65_1",
            "GraphMolWrap"});
    LIBRARIES.put(OS_WIN32 + "." + ARCH_X86_64,
        new String[]{
            "boost_serialization-vc140-mt-x64-1_67",
            "boost_zlib-vc140-mt-x64-1_67",
            "boost_bzip2-vc140-mt-x64-1_67",
            "boost_iostreams-vc140-mt-x64-1_67",
            "GraphMolWrap",
        });
    LIBRARIES.put("linux.",
        new String[]{"GraphMolWrap"});
    LIBRARIES.put(OS_LINUX + "." + ARCH_X86_64,
        new String[]{"GraphMolWrap"});
    LIBRARIES.put(OS_MACOSX + "." + ARCH_X86_64,
        new String[]{"GraphMolWrap"});
  }

  public static void loadLibraries() throws LoaderException {
    final String platform = getPlatform();
    final String extension = getExtension(getOs());
    final String[] libraries = listLibraries(platform);
    final List<String> libraryNames = Arrays.stream(libraries)
        .map(x -> String.format("%s.%s", x, extension))
        .collect(Collectors.toList());
    // Contains filename + extension
    final List<String> missingLibraries = findLibraries(libraryNames);

    if (missingLibraries.size() > 0) {
      logger.info("Missing libraries: {}", missingLibraries);
      LibraryMover.resolveMissingLibraries(missingLibraries, platform);
    }

    loadLibraries(libraries);
  }

  private static String getExtension(String os) {
    switch (os) {
      case OS_MACOSX: return "jnilib";
      case OS_LINUX: return "so";
      case OS_WIN32: return "dll";
      default: return null;
    }
  }

  /**
   * Method returns all libraries found in `path`
   * @param path to investigate
   * @param fileNames to detect
   * @return list of detected libraries
   */
  public static List<String> getLibrariesInFolder(String path, List<String> fileNames) {
    final List<String> existingLibraries = new ArrayList<>();
    for (final String library : fileNames) {
      final File fileDir = new File(path);
      if (fileDir.isDirectory()) {
        final File fileLib = new File(fileDir, library);
        try {
          if (fileLib.isFile() && fileLib.canRead()) {
            logger.debug("Library {} found in folder: {}", library, path);
            existingLibraries.add(library);
          }
        } catch (final SecurityException exc) {
          logger.debug("Library {} access denied in folder: {}", library, path);
          // Thrown, if we do not have read access at all - ignore this
        }
      }
    }

    return existingLibraries;
  }


  private static List<String> findLibraries(List<String> libraryNames) {
    final List<String> existingLibraries = new ArrayList<>();

    // Check file in existing java.library.path(s)
    final String[] libraryPaths = getLibraryPaths();
    for (final String strPath : libraryPaths) {
      val found = getLibrariesInFolder(strPath, libraryNames);
      existingLibraries.addAll(found);
    }

    val counted = existingLibraries.stream()
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet()
        .stream()
        .collect(Collectors.toList());

    for (Entry<String, Long> item : counted) {
      if (item.getValue() >= 2) {
        logger.warn("Library {} exists {} times", item.getKey(), item.getValue());
      }
    }

    return libraryNames.stream()
        .filter(x -> !existingLibraries.contains(x))
        .collect(Collectors.toList());
  }

  private static String[] getLibraryPaths() {
    return System.getProperty("java.library.path", "").split(File.pathSeparator);
  }

  private static String[] listLibraries(String platform) {
    logger.info("Libraries will be loaded from {}", platform);
    return LIBRARIES.get(platform);
  }

  private static String getPlatform() {
    final String arch = getArch();
    final String os = getOs();
    return String.format("%s.%s", os, arch);
  }

  /**
   * Method for calling OS to load native libraries stored in java.library.path
   *
   * @param libraries to load
   */
  private static void loadLibraries(String[] libraries) {
    // todo: what about optional path to load files from?
    for (String lib : libraries) {
      System.loadLibrary(lib);
      logger.info("Successfully loaded library={}", lib);
    }
  }


  private static String getArch() {
    String arch = System.getProperty("os.arch");
    if (arch == null) {
      throw new UnsatisfiedLinkError("Can not identify os.arch property");
    }

    return getArchFormatted(arch);
  }

  /**
   * Method returns formatted architecture of the computer
   *
   * @return formatted architecture
   */
  static String getArchFormatted(String arch) {
    return arch.toLowerCase().endsWith("64") ? ARCH_X86_64 : ARCH_X86;
  }

  private static String getOs() {
    String osname = System.getProperty("os.name");
    if (osname == null) {
      throw new UnsatisfiedLinkError("Can not identify os.name property");
    }

    return getOsFormatted(osname);
  }

  /**
   * Method returns formatted OS name of the computer
   *
   * @return formatted OS name
   */
  static String getOsFormatted(String osname) {
    osname = osname.toLowerCase();

    String formattedOs;
    if (osname.contains("linux")) {
      formattedOs = OS_LINUX;
    } else if (osname.contains("mac")) {
      formattedOs = OS_MACOSX;
    } else if (osname.contains("windows")) {
      formattedOs = OS_WIN32;
    } else {
      throw new UnsatisfiedLinkError("Could not determine the system parameters properly");
    }

    return formattedOs;
  }
}
