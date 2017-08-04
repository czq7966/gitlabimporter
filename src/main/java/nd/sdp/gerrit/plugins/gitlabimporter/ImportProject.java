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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.webui.UiAction;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.config.SitePath;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.config.GerritServerConfig;


import com.google.gerrit.common.data.AccessSection;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.common.data.Permission;
import com.google.gerrit.common.data.PermissionRule;
import com.google.gerrit.extensions.annotations.PluginCanonicalWebUrl;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.common.GroupInfo;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.ProjectConfig;
import com.google.gerrit.server.group.ListGroups;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Gson;  
import com.google.gson.reflect.TypeToken; 
import com.google.gwtorm.server.OrmException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.lib.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



class ImportProject implements  RestModifyView<ProjectResource, ImportProject.Input> {
  static final Logger log = LoggerFactory.getLogger(ImportProject.class);
  static final Gson gson = new Gson();
  private Provider<CurrentUser> user;
  private SitePaths site;
  private ProjectCache projectCache;
  private MetaDataUpdate.Server metaDataUpdateFactory;
  private ListGroups lsGroups;

  @Inject protected GerritApi gApi;
  @Inject @SitePath private  Path sitePath;
  @Inject @GerritServerConfig private Config cfg;
  @Inject  private ImporterConfig config;

  static class Input {
    String message;
  }

  static class Output{
    boolean result;
    String message;
  }

  @Inject
  ImportProject(Provider<CurrentUser> user, SitePaths site, ProjectCache projectCache, ListGroups lsGroups, MetaDataUpdate.Server metaDataUpdateFactory) {
    this.user = user;
    this.site = site;
    this.projectCache = projectCache;
    this.lsGroups = lsGroups;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
  }

  @Override
  public Output apply(ProjectResource rsrc, Input input) {
    String repoName =  input.message;
    Output out = new Output();
    out.result = false;
    out.message = "失败，请联系管理员！";

    String defaultParams = " _remote_server=" + '\"' + config.remote_server + '\"' + 
                    " _local_server=" + '\"' + config.local_server + '\"' + 
                    " _local_server_admin=" + '\"' + config.local_server_admin + '\"' + 
                    " _local_gerrit_site=" + '\"' + config.local_gerrit_site + '\"' + 
		    " _local_bak_dir=" + '\"' + config.local_gerrit_site + "/bak" + '\"' +
                    " _local_git_dir=" + '\"' + config.local_git_dir + '\"';

    String params = defaultParams + " _import_projet=" + '\"' + repoName + '\"';
           params = params + " _current_user=" + '\"' + user.get().getUserName() + '\"';
  
    params = params + " plugin_gerrit_import_project";
    try {
        String script = String.format("/bin/bash -e %s %s", config.importScriptFile, params);
        log.warn("执行迁移脚本：" + script);
        String result = new JavaShellUtil().executeShell(script);
        result = result.trim();
        if (result.length() > 0) {
          out.result = result.substring(0, 1).equals("1");
          out.message = result;
          if (out.result) {
		//setProjectAccess(repoName);
	  }
        } 
    } catch (JsonIOException e) {		   
        log.warn(e.getMessage());
    } catch (JsonSyntaxException e) {
        log.warn(e.getMessage());
    }  catch (IOException e) {
        log.warn(e.getMessage());
    }
    return out;
  }


	/**
	 * 设置分支访问权限
	 * 对应gerrit画面中Project->Access部分的访问权限设定
	 * */
	private void setProjectAccess(String repoName){
		try {
			/* group name */
			String grpOwner = repoName+"_owner";
			String grpRead = repoName+"_read";
			String grpPush = repoName+"_push";
			String grpReview = repoName+"_review";
			String grpVerified = repoName+"_verified";
			String grpSubmit = repoName+"_submit";
			/* group uuid*/
			UUID uuidOwner = new UUID(findGroupUUID(grpOwner));
			UUID uuidRead = new UUID(findGroupUUID(grpRead));
			UUID uuidPush = new UUID(findGroupUUID(grpPush));
			UUID uuidReview = new UUID(findGroupUUID(grpReview));
			UUID uuidVerified = new UUID(findGroupUUID(grpVerified));
			UUID uuidSubmit = new UUID(findGroupUUID(grpSubmit));
			
			/* 对应设置[refs/*]的访问权限 */
			AccessSection refs = new AccessSection("refs/*"); 
			Permission ownerRefs = new Permission(Permission.OWNER); 
			PermissionRule ruleOwnerRefs = new PermissionRule(); //默认值为ALLOW, [常用:DENY, BLOCK]
			ruleOwnerRefs.setGroup(new GroupReference(uuidOwner, grpOwner));
			ownerRefs.add(ruleOwnerRefs);
			refs.addPermission(ownerRefs); /* owner权限 */
			Permission readRefs = new Permission(Permission.READ); 
			PermissionRule ruleReadRefs = new PermissionRule(); 
			ruleReadRefs.setGroup(new GroupReference(uuidRead, grpRead));
			readRefs.add(ruleReadRefs);
			refs.addPermission(readRefs); /* read权限 */
			
			/* 对应设置[refs/for/refs/*]的访问权限 */
			AccessSection refsFor = new AccessSection("refs/for/refs/*"); 
			Permission pushFor = new Permission(Permission.PUSH); 
			PermissionRule ruleFor = new PermissionRule(); 
			ruleFor.setGroup(new GroupReference(uuidPush, grpPush));
			pushFor.add(ruleFor);
			refsFor.addPermission(pushFor); /* push权限 */
			
			/* 对应设置[refs/heads/*]的访问权限 */
			AccessSection refsHead = new AccessSection("refs/heads/*"); 
			Permission labelCRHead = new Permission(Permission.LABEL+"Code-Review"); 
			PermissionRule ruleLabelCRHead = new PermissionRule(); 
			ruleLabelCRHead.setGroup(new GroupReference(uuidReview, grpReview)); 
			ruleLabelCRHead.setRange(-2, 2); //设置评分 -2..+2
			labelCRHead.add(ruleLabelCRHead);
			refsHead.addPermission(labelCRHead); /* label-code-review权限 */
			Permission labelSVHead = new Permission(Permission.LABEL+"Sonar-Verified"); 
			PermissionRule ruleLabelSVHead = new PermissionRule(); 
			ruleLabelSVHead.setGroup(new GroupReference(uuidReview, grpReview)); 
			ruleLabelSVHead.setRange(-1, 1); 
			labelSVHead.add(ruleLabelSVHead);
			refsHead.addPermission(labelSVHead); /* label-sonar-verified权限 */
			Permission labelVHead = new Permission(Permission.LABEL+"Verified"); 
			PermissionRule ruleLabelVHead = new PermissionRule(); 
			ruleLabelVHead.setGroup(new GroupReference(uuidVerified, grpVerified)); 
			ruleLabelVHead.setRange(-1, 1); 
			labelVHead.add(ruleLabelVHead);
			refsHead.addPermission(labelVHead); /* label-verified权限 */
			Permission submitHead = new Permission(Permission.SUBMIT); 
			PermissionRule ruleSubmitHead = new PermissionRule(); 
			ruleSubmitHead.setGroup(new GroupReference(uuidSubmit, grpSubmit)); 
			submitHead.add(ruleSubmitHead);
			refsHead.addPermission(submitHead); /* submit权限 */
			
			/* 对应设置[refs/meta/config]的访问权限 */
			AccessSection refsConf = new AccessSection("refs/meta/config"); 
			Permission readConf = new Permission(Permission.READ); 
			PermissionRule ruleConf = new PermissionRule(); 
			ruleConf.setGroup(new GroupReference(uuidRead, grpRead)); 
			readConf.add(ruleConf);
			refsConf.addPermission(readConf); /* read权限 */
			
			//利用ProjectConfig进行权限设置
			MetaDataUpdate meta = metaDataUpdateFactory.create(new NameKey(repoName));
			ProjectConfig projCfg = ProjectConfig.read(meta);
			projCfg.replace(refs);
			projCfg.replace(refsFor);
			projCfg.replace(refsHead);
			projCfg.replace(refsConf); 
			projCfg.commit(meta); // 设置好的权限需要提交
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("设置权限失败！");
		}

	}
	
	/**
	 * 根据group name获取对应的uuid
	 * */
	String findGroupUUID(String grpName){
		try {
			List<GroupInfo> groups = lsGroups.get();
			for(GroupInfo g:groups){
				if(grpName.equals(g.name)){
					return g.id;
				}
			}
		} catch (BadRequestException e) {
			e.printStackTrace();
		} catch (OrmException e) {
			e.printStackTrace();
		}
		
		return "";
	}



}
