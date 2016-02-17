/**
 * Copyright (C) 2015 DataTorrent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datatorrent.stram.client;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * ConfigPackage class.</p>
 *
 * @since 2.1.0
 */
public class ConfigPackage extends JarFile implements Closeable
{

  public static final String ATTRIBUTE_DT_CONF_PACKAGE_NAME = "DT-Conf-Package-Name";
  public static final String ATTRIBUTE_DT_APP_PACKAGE_NAME = "DT-App-Package-Name";
  public static final String ATTRIBUTE_DT_APP_PACKAGE_MIN_VERSION = "DT-App-Package-Min-Version";
  public static final String ATTRIBUTE_DT_APP_PACKAGE_MAX_VERSION = "DT-App-Package-Max-Version";
  public static final String ATTRIBUTE_DT_CONF_PACKAGE_DESCRIPTION = "DT-Conf-Package-Description";
  public static final String ATTRIBUTE_CLASS_PATH = "Class-Path";
  public static final String ATTRIBUTE_FILES = "Files";

  private final String configPackageName;
  private final String appPackageName;
  private final String appPackageMinVersion;
  private final String appPackageMaxVersion;
  private final String configPackageDescription;
  private final ArrayList<String> classPath = new ArrayList<String>();
  private final ArrayList<String> files = new ArrayList<String>();
  private final String directory;

  private final Map<String, String> properties = new TreeMap<String, String>();
  private final Map<String, Map<String, String>> appProperties = new TreeMap<String, Map<String, String>>();

  /**
   * Creates an Config Package object.
   *
   * @param file
   * @throws java.io.IOException
   * @throws net.lingala.zip4j.exception.ZipException
   */
  public ConfigPackage(File file) throws IOException, ZipException
  {
    super(file);
    Manifest manifest = getManifest();
    if (manifest == null) {
      throw new IOException("Not a valid config package. MANIFEST.MF is not present.");
    }
    Attributes attr = manifest.getMainAttributes();
    configPackageName = attr.getValue(ATTRIBUTE_DT_CONF_PACKAGE_NAME);
    appPackageName = attr.getValue(ATTRIBUTE_DT_APP_PACKAGE_NAME);
    appPackageMinVersion = attr.getValue(ATTRIBUTE_DT_APP_PACKAGE_MIN_VERSION);
    appPackageMaxVersion = attr.getValue(ATTRIBUTE_DT_APP_PACKAGE_MAX_VERSION);
    configPackageDescription = attr.getValue(ATTRIBUTE_DT_CONF_PACKAGE_DESCRIPTION);
    String classPathString = attr.getValue(ATTRIBUTE_CLASS_PATH);
    String filesString = attr.getValue(ATTRIBUTE_FILES);
    if (configPackageName == null) {
      throw new IOException("Not a valid config package.  DT-Conf-Package-Name is missing from MANIFEST.MF");
    }
    if (!StringUtils.isBlank(classPathString)) {
      classPath.addAll(Arrays.asList(StringUtils.split(classPathString, " ")));
    }
    if (!StringUtils.isBlank(filesString)) {
      files.addAll(Arrays.asList(StringUtils.split(filesString, " ")));
    }

    ZipFile zipFile = new ZipFile(file);
    if (zipFile.isEncrypted()) {
      throw new ZipException("Encrypted conf package not supported yet");
    }
    File newDirectory = new File("/tmp/dt-configPackage-" + Long.toString(System.nanoTime()));
    newDirectory.mkdirs();
    directory = newDirectory.getAbsolutePath();
    zipFile.extractAll(directory);
    processPropertiesXml();
  }

  public String tempDirectory()
  {
    return directory;
  }

  @Override
  public void close() throws IOException
  {
    super.close();
    FileUtils.deleteDirectory(new File(directory));
  }

  public String getConfigPackageName()
  {
    return configPackageName;
  }

  public String getAppPackageName()
  {
    return appPackageName;
  }

  public String getAppPackageMinVersion()
  {
    return appPackageMinVersion;
  }

  public String getAppPackageMaxVersion()
  {
    return appPackageMaxVersion;
  }

  public String getConfigPackageDescription()
  {
    return configPackageDescription;
  }

  public List<String> getClassPath()
  {
    return Collections.unmodifiableList(classPath);
  }

  public List<String> getFiles()
  {
    return Collections.unmodifiableList(files);
  }

  public Map<String, String> getProperties(String appName)
  {
    if (appName == null || !appProperties.containsKey(appName)) {
      return properties;
    } else {
      return appProperties.get(appName);
    }
  }

  private void processPropertiesXml()
  {
    File dir = new File(directory, "META-INF");
    File p = new File(dir, "properties.xml");

    if (p.exists()) {
      parsePropertiesXml(p, properties);
    }
    for (File file : dir.listFiles()) {
      String name = file.getName();
      if (name.length() > 15 && name.startsWith("properties-") && name.endsWith(".xml")) {
        String appName = name.substring(11, name.length() - 4);
        Map<String, String> dp = new TreeMap<String, String>(properties);
        parsePropertiesXml(file, dp);
        appProperties.put(appName, dp);
      }
    }
  }

  private static void parsePropertiesXml(File file, Map<String, String> properties)
  {
    DTConfiguration config = new DTConfiguration();
    try {
      config.loadFile(file);
      for (Map.Entry<String, String> entry : config) {
        String key = entry.getKey();
        String value = entry.getValue();
        properties.put(key, value);
      }
    } catch (Exception ex) {
      LOG.warn("Ignoring {} because of error", ex, file.getName());
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(ConfigPackage.class);

}
