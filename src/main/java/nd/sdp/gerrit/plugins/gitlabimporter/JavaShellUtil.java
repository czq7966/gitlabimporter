package nd.sdp.gerrit.plugins.gitlabimporter;
 
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
 

public class JavaShellUtil {
    public String executeShell(String shellCommand) throws IOException {
        String success = "";
        StringBuffer stringBuffer = new StringBuffer();
        BufferedReader bufferedReader = null;

        try {
            Process pid = null;
            // 执行Shell命令
            pid = Runtime.getRuntime().exec(shellCommand);
            if (pid != null) {
                // bufferedReader用于读取Shell的输出内容
                bufferedReader = new BufferedReader(new InputStreamReader(pid.getInputStream()), 1024);
                int exitValue = pid.waitFor();
		if (0 != exitValue) {
		  stringBuffer.append("执行脚本过程中出现错误！\r\n");
		}
            } else {
                stringBuffer.append("没有pid\r\n");
            }

            String line = null;
            // 读取Shell的输出内容，并添加到stringBuffer中
            while (bufferedReader != null
                    && (line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line).append("\r\n");
            }
        } catch (Exception ioe) {
            stringBuffer.append("执行Shell命令时发生异常：\r\n").append(ioe.getMessage())
                    .append("\r\n");
        } finally {
            success = stringBuffer.toString();
        }
        return success;
    }


}
