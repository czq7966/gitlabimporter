// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package nd.sdp.gerrit.plugins.gitlabimporter;

import static nd.sdp.gerrit.plugins.gitlabimporter.GitlabImportCapability.GITLAB_IMPORT;
import static com.google.gerrit.pgm.init.api.InitUtil.extract;
import static com.google.gerrit.server.change.RevisionResource.REVISION_KIND;
import static com.google.gerrit.server.project.ProjectResource.PROJECT_KIND;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.api.projects.ProjectConfigEntryType;
import com.google.gerrit.extensions.client.InheritableBoolean;
import com.google.gerrit.extensions.config.ExternalIncludedIn;
import com.google.gerrit.extensions.config.CapabilityDefinition;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.events.NewProjectCreatedListener;
import com.google.gerrit.extensions.events.UsageDataPublishedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.extensions.webui.BranchWebLink;
import com.google.gerrit.extensions.webui.FileHistoryWebLink;
import com.google.gerrit.extensions.webui.GwtPlugin;
import com.google.gerrit.extensions.webui.JavaScriptPlugin;
import com.google.gerrit.extensions.webui.PatchSetWebLink;
import com.google.gerrit.extensions.webui.ProjectWebLink;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.extensions.webui.WebUiPlugin;

import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.MergeValidationListener;
import com.google.gerrit.server.git.validators.RefOperationValidationListener;
import com.google.gerrit.server.git.validators.UploadValidationListener;
import com.google.gerrit.server.plugins.ServerPluginProvider;
import com.google.gerrit.server.query.change.ChangeQueryBuilder.ChangeOperatorFactory;
import com.google.gerrit.server.validators.HashtagValidationListener;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.SitePath;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.AbstractModule;

import org.eclipse.jgit.lib.Config;

import java.io.IOException;
import java.nio.file.Paths;


public class Module extends AbstractModule {

  @Inject
  @PluginName
  private String pluginName;

  @Inject
  private Injector sysInjector;

  @Inject
  private ImporterConfig importConfig;

  @Override
  protected void configure() {
    //bind(ImporterConfig.class)
    //ImporterConfig importConfig = sysInjector.getInstance(ImporterConfig.class);

    try {
      extract(Paths.get(importConfig.importScriptFile), getClass(), pluginName + ".sh");
    } catch(IOException e) {
    }

    DynamicSet.bind(binder(), TopMenu.class).to(ImporterTopMenu.class);
    bind(CapabilityDefinition.class).annotatedWith(Exports.named(GITLAB_IMPORT)).to(GitlabImportCapability.class);

    install(new RestApiModule() {
      @Override
      protected void configure() {
         post(PROJECT_KIND, "import-project").to(ImportProject.class);
	 get(PROJECT_KIND, "import-project").to(GetImportProjects.class);
      }
    });

    DynamicSet.bind(binder(), WebUiPlugin.class)
        .toInstance(new GwtPlugin("gitlabimporter"));
  }

}
