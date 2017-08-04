package nd.sdp.gerrit.plugins.gitlabimporter;


import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gson.Gson;  
import com.google.gson.reflect.TypeToken; 
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class ImporterUtil {  
    static final Logger log = LoggerFactory.getLogger(ImporterUtil.class);
    static final Gson gson = new Gson();
    static final String reqURL = ImporterConfig.sdpurl_userprojects;// "http://sdp-portal-server.debug.web.nd/v0.2/app/list?type=GERRIT_TRANSFER_GITLAB&skip=1&size=2000&userId=%s";

    public static Projects getRemoteUserProjects(String user) {

	Projects projects = null;
	JsonParser parser=new JsonParser();
	String repStr = HttpRequestUtil.getHttpResponse(String.format(reqURL, user));
	if (repStr != null) {
	    projects = gson.fromJson(repStr, Projects.class);
	    if (projects.result && projects.object.totals > 0) {
		for (final Row row : projects.object.rows){
		  row.appName = row.gitSshUrl.split(":")[1].split(".git")[0];
		}
	    }
	}
        return projects;
    }	 
  
    public static String getLocalAllProjects(ProjectCache projectCache) {
      Collection<String> result = new ArrayList<>();
      for (final Project.NameKey projectName : projectCache.all()) {
	result.add(projectName.get());
      }
      return gson.toJson(result);
    }

   public static Projects getRemoteUserImportProjects(String user, ProjectCache projectCache) {
      Projects result = getRemoteUserProjects(user);
      if (result.result && result.object.totals > 0) {
        for (final Project.NameKey projectName : projectCache.all()) {
	  for (final Row row : result.object.rows){
            if (row.appName.toLowerCase().equals(projectName.get().toLowerCase())){
              result.object.rows.remove(row);
              break;
            } 
	  }
        }	
      }
      return result;
   }

   public class Row {
	public String appName;
	public String gitSshUrl;
	public String gitProjectId;
   }

   public class Object {
	public int totals;
	public Collection<Row> rows;
   }
   
   public class Projects {
	public boolean result;
	public String level;
	public String message;
	public Object object;
   }

}  
