package com.jsnow.jdex2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.jsnow.jdex2.databinding.ActivityMainBinding;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private void writeConfigWithRoot(String content, String targetApp) {
        if (targetApp.isEmpty() || !isTargetAppInstalled(targetApp)) {
            Toast.makeText(this,
                    "目标应用不存在：" + targetApp,
                    Toast.LENGTH_LONG).show();
            return;
        }

        Process process = null;
        DataOutputStream os = null;
        try {
            // 1. 先写临时文件到自己的目录
            File tempFile = new File(getFilesDir(), "config.properties");
            FileOutputStream fos = new FileOutputStream(tempFile, false);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();

            // 确认临时文件确实写成功了
            if (!tempFile.exists() || tempFile.length() == 0) {
                Toast.makeText(this, "临时文件创建失败！", Toast.LENGTH_LONG).show();
                return;
            }

            String tempPath = tempFile.getAbsolutePath();
            String targetDir = "/data/data/" + targetApp + "/files/";
            String targetPath = targetDir + "config.properties";

            String myNativeLibDir = getApplicationInfo().nativeLibraryDir;
            String targetLibDir = "/data/data/" + targetApp + "/lib/";

            String logFile = "/data/local/tmp/jdex2_log.txt";

            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());

            os.writeBytes("echo '' > " + logFile + "\n");

            os.writeBytes("setenforce 0\n");

            os.writeBytes("if [ -f " + tempPath + " ]; then "
                    + "echo 'SRC_OK' >> " + logFile + "; "
                    + "else echo 'SRC_MISSING: " + tempPath + "' >> " + logFile + "; fi\n");

            os.writeBytes("mkdir -p " + targetDir + " 2>>" + logFile + "\n");
            os.writeBytes("echo 'mkdir targetDir exit:' $? >> " + logFile + "\n");

            os.writeBytes("cat " + tempPath + " > " + targetPath
                    + " 2>>" + logFile + "\n");
            os.writeBytes("echo 'cp config exit:' $? >> " + logFile + "\n");

            os.writeBytes("if [ -f " + targetPath + " ]; then "
                    + "echo 'DST_CONFIG_OK, size:' $(wc -c < " + targetPath + ") >> " + logFile + "; "
                    + "else echo 'DST_CONFIG_MISSING' >> " + logFile + "; fi\n");

            os.writeBytes("chmod 666 " + targetPath + " 2>>" + logFile + "\n");

            os.writeBytes("TARGET_OWNER=$(stat -c '%U:%G' /data/data/" + targetApp
                    + " 2>>" + logFile + ")\n");
            os.writeBytes("echo 'target owner:' $TARGET_OWNER >> " + logFile + "\n");
            os.writeBytes("chown $TARGET_OWNER " + targetPath + " 2>>" + logFile + "\n");

            os.writeBytes("mkdir -p " + targetLibDir + " 2>>" + logFile + "\n");

            os.writeBytes("if [ -f " + myNativeLibDir + "/libjdex2.so ]; then "
                    + "echo 'SO_SRC_OK' >> " + logFile + "; "
                    + "else echo 'SO_SRC_MISSING: " + myNativeLibDir + "' >> " + logFile + "; fi\n");

            os.writeBytes("cp " + myNativeLibDir + "/libjdex2.so " + targetLibDir
                    + " 2>>" + logFile + "\n");
            os.writeBytes("echo 'cp so exit:' $? >> " + logFile + "\n");

            os.writeBytes("chmod -R 755 " + targetLibDir + " 2>>" + logFile + "\n");
            os.writeBytes("chown -R $TARGET_OWNER " + targetLibDir + " 2>>" + logFile + "\n");

            os.writeBytes("echo '== target files dir ==' >> " + logFile + "\n");
            os.writeBytes("ls -la " + targetDir + " >> " + logFile + " 2>&1\n");
            os.writeBytes("echo '== target lib dir ==' >> " + logFile + "\n");
            os.writeBytes("ls -la " + targetLibDir + " >> " + logFile + " 2>&1\n");

            os.writeBytes("exit\n");
            os.flush();

            int exitValue = process.waitFor();

            // 读取日志内容用于展示
            String logContent = readFileAsRoot(logFile);

            if (exitValue == 0) {

                Log.i("JDex2 Config", "执行完成" + logContent);
                Toast.makeText(this,
                        "执行完成(exit=0)\n",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,
                        "执行失败(exit=" + exitValue + ")\n日志:\n" + logContent,
                        Toast.LENGTH_LONG).show();
            }

            tempFile.delete();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "异常：" + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (Exception ignored) {}
        }
    }

    /**
     * 用 root 权限读取文件内容
     */
    private String readFileAsRoot(String filePath) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat " + filePath});
            java.io.InputStream is = process.getInputStream();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            process.waitFor();
            return sb.toString();
        } catch (Exception e) {
            return "读取日志失败: " + e.getMessage();
        } finally {
            if (process != null) process.destroy();
        }
    }
    private boolean isTargetAppInstalled(String targetApp) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());

            // 用 pm path 判断包是否存在，比 test -d 更可靠
            os.writeBytes("pm path " + targetApp + " > /dev/null 2>&1\n");
            os.writeBytes("exit $?\n");
            os.flush();

            int exitValue = process.waitFor();
            return exitValue == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 动态申请读写权限
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                1
        );


        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("JDex2", "Click");

                String targetApp = binding.editTextTextPersonName.getText().toString().trim();
                String whiteList = binding.editTextTextPersonName2.getText().toString().trim();
                String blackList = binding.editTextTextPersonName3.getText().toString().trim();

                boolean hook = binding.switch2.isChecked();
                boolean debugger = binding.switch1.isChecked();
                boolean innerclassesFilter =binding.switch3.isChecked();
                boolean invokeConstructors = binding.invoke.isChecked();
                String content =
                        "targetApp=" + targetApp + "\n" +
                                "hook=" + hook + "\n" +
                                "invokeDebugger=" + debugger + "\n" +
                                "whiteList=" + whiteList + "\n" +
                                "blackList=" + blackList + "\n" +
                                "innerclassesFilter=" + innerclassesFilter + "\n" +
                                "invokeConstructors=" + invokeConstructors + "\n";

                // 没招了，高版本Android的读写权限太严了，只能用root了
                writeConfigWithRoot(content, targetApp);
            }
        });
    }
}
