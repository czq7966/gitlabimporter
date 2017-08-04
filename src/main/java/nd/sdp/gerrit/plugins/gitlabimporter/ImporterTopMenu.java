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
import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.client.MenuItem;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.account.CapabilityControl;
import com.google.inject.Inject;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Provider;
import java.util.List;


public class ImporterTopMenu implements TopMenu {
  private final List<MenuEntry> menuEntries;
  private final Provider<CurrentUser> userProvider;
  private final String pluginName;

  @Inject
  public ImporterTopMenu(@PluginName String pluginName, Provider<CurrentUser> userProvider) {
    this.pluginName = pluginName;
    this.userProvider = userProvider;
    this.menuEntries = Lists.newArrayListWithCapacity(1);
    if (userProvider.get().isIdentifiedUser() && canImport()) {
	menuEntries.add(new MenuEntry("Projects", Lists.newArrayList(
            new MenuItem("开通门户代码库", "#/x/" + pluginName + "/", ""))));
    }
  }

  @Override
  public List<MenuEntry> getEntries() {
      return menuEntries;
  }

  protected boolean canImport() {
    CapabilityControl ctl = userProvider.get().getCapabilities();
//    return ctl.canAdministrateServer() || ctl.canPerform(pluginName + "-" + GITLAB_IMPORT);
    return ctl.canPerform(pluginName + "-" + GITLAB_IMPORT);
  }

}
