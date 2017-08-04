package nd.sdp.gerrit.plugins.gitlabimporter;

import java.io.BufferedReader;  
import java.io.IOException;  
import java.io.InputStream;  
import java.io.InputStreamReader;  
import java.io.OutputStream;  
import java.net.ConnectException;  
import java.net.HttpURLConnection;  
import java.net.URL;  
import java.net.URI;
import java.net.URLConnection;   


import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestUtil {  
    static final Logger log = LoggerFactory.getLogger(HttpRequestUtil.class);

    public static String getHttpResponse(String allConfigUrl) {
        BufferedReader in = null;
        StringBuffer result = null;
        try {
             
            URI uri = new URI(allConfigUrl);
            URL url = uri.toURL();
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Charset", "utf-8");
         
            connection.connect();
             
            result = new StringBuffer();
            //读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
             
            return result.toString();
             
        } catch (Exception e) {
	    log.warn(e.getMessage());
        }finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
		log.warn(e2.getMessage());
            }
        }
         
        return null;
         
    }	 
  
}  
