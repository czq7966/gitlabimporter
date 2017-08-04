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


import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;

@Singleton
class GetImportProjects implements RestReadView<ProjectResource> {

  private Provider<CurrentUser> user;
  private ProjectCache projectCache;

  static class ProjectInfo {
    String appName;
  }

  @Inject
  GetImportProjects(Provider<CurrentUser> user, ProjectCache projectCache) {
    this.user = user;
    this.projectCache = projectCache;
  }

  @Override
  public Response<Collection<ImporterUtil.Row>> apply(ProjectResource rev) {
	Collection<ProjectInfo> list = new ArrayList<>();
        ProjectInfo out = new  ProjectInfo();
	ImporterUtil.Projects projects = ImporterUtil.getRemoteUserImportProjects(user.get().getUserName(), projectCache);


	return Response.ok(projects.object.rows);
  }

}
