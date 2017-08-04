
package nd.sdp.gerrit.plugins.gitlabimporter;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.extensions.annotations.PluginName;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




@Singleton
public final class ImporterConfig {
  static final Logger log = LoggerFactory.getLogger(ImporterConfig.class);
  private final Path sitePath;
  private final Path cfgPath;
  private final Config gerritCfg;

  public String importScriptFile;
  public String remote_server;
  public String local_server;
  public String local_server_admin;
  public String local_gerrit_site;
  public String local_git_dir;
  static public String sdpurl_userprojects;



  @Inject
  public ImporterConfig(@PluginName String pluginName, SitePaths site, @GerritServerConfig Config cfg) throws ConfigInvalidException, IOException {
    this.sitePath = site.site_path;
    this.importScriptFile = site.bin_dir.resolve(pluginName + ".sh").toString();
    this.cfgPath = site.etc_dir.resolve(pluginName + ".config");
    this.gerritCfg = cfg;
    reload();
  }

  public void reload() throws ConfigInvalidException, IOException {
    final FileBasedConfig config = new FileBasedConfig(cfgPath.toFile(), FS.DETECTED);
    if (!config.getFile().exists()) {
      log.warn("Config file " + config.getFile() + " does not exist;");
    }

    try {
      config.load();
      remote_server = config.getString("sshserver", "remote", "server");
      local_server = config.getString("sshserver", "local", "server");
      local_server_admin = config.getString("sshserver", "local", "admin");
      local_gerrit_site = sitePath.toString();
      local_git_dir = sitePath.resolve(gerritCfg.getString("gerrit", null, "basePath")).toString();
      sdpurl_userprojects = config.getString("sdpurl", null, "userprojects");

    } catch (ConfigInvalidException e) {
      throw new ConfigInvalidException(
          String.format("Config file %s is invalid: %s", config.getFile(), e.getMessage()), e);
    } catch (IOException e) {
      throw new IOException(
          String.format("Cannot read %s: %s", config.getFile(), e.getMessage()), e);
    }
  }
}
