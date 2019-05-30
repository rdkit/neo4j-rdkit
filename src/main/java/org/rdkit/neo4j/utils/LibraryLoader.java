package org.rdkit.neo4j.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.rdkit.neo4j.exceptions.LoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class loads native libraries depending on os & arch
 */
public class LibraryLoader {
  private static final Logger logger = LoggerFactory.getLogger(LibraryLoader.class);

  /**
   * List of libraries to be loaded for different operating systems (lib order
   * is important).
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
        new String[] { "boost_serialization-vc140-mt-1_65_1",
            "GraphMolWrap" });
    LIBRARIES.put(OS_WIN32 + "." + ARCH_X86_64,
        new String[] { "boost_serialization-vc140-mt-x64-1_67",
            "boost_iostreams-vc140-mt-x64-1_67",
            "boost_system-vc140-mt-x64-1_67",
            "GraphMolWrap" });
    LIBRARIES.put("linux.",
        new String[] { "GraphMolWrap" });
    LIBRARIES.put(OS_LINUX + "." + ARCH_X86_64,
        new String[] { "GraphMolWrap" });
    LIBRARIES.put(OS_MACOSX + "." + ARCH_X86_64,
        new String[] { "GraphMolWrap" });
  }

  private static String[] listLibraries() {
    final String arch = getArch();
    final String os = getOs();
    return LIBRARIES.get(arch + "." + os);
  }

  /**
   * Method for calling OS to load native libraries stored in java.library.path
   * @param libraryNames to load
   */
  private static void loadLibraries(List<String> libraryNames) {
    for (String lib: libraryNames) {
      System.loadLibrary(lib);
      logger.info("Successfully loaded {} library", lib);
    }
  }

  /**
   * Method returns architecture of the computer
   * @return formatted architecture
   */
  static String getArch() {
    String arch = System.getProperty("os.arch");
    if (arch == null)
      throw new LoaderException("Can not identify os.arch property");

    return arch.toLowerCase().endsWith("64") ? ARCH_X86_64 : ARCH_X86;
  }

  static String getOs() {
    String osname = System.getProperty("os.name");
    if (osname == null)
      throw new LoaderException("Can not identify os.name property");

    osname = osname.toLowerCase();

    String formattedOs;
    if (osname.contains("linux")) {
      formattedOs = OS_LINUX;
    } else if (osname.contains("mac")) {
      formattedOs = OS_MACOSX;
    } else if (osname.contains("windows")) {
      formattedOs = OS_WIN32;
    } else
      throw new LoaderException("Could not determine the system parameters properly");

    return formattedOs;
  }
}
