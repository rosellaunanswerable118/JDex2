package com.jsnow.jdex2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import com.jsnow.jdex2.databinding.ActivityMainBinding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "JDex2";
    private ActivityMainBinding binding;

    // 暂存待写入的内容和目标包名，用于 SAF 授权回调后继续写入
    private String pendingContent = null;
    private String pendingTargetApp = null;

    private void writeConfig(String content, String targetApp) {
        try {
            // Android 12 (API 31) 及以上：先尝试零宽字符绕过，失败则回退到 SAF
            if (Build.VERSION.SDK_INT >= 31) {

                // 尝试零宽字符绕过
                String bypassDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/Android/\u200bdata/" + targetApp + "/files/";
                File bypassDirFile = new File(bypassDir);
                if (!bypassDirFile.exists()) bypassDirFile.mkdirs();

                File bypassFile = new File(bypassDir + "config.properties");
                if (!bypassFile.exists()) bypassFile.createNewFile();

                FileOutputStream fos = new FileOutputStream(bypassFile, false);
                fos.write(content.getBytes());
                fos.flush();
                fos.close();

                // 验证是否写入到了真正的 /Android/data/ 目录
                File verifyFile = new File(
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                + "/Android/data/" + targetApp + "/files/config.properties");

                if (verifyFile.exists() && verifyFile.length() > 0) {
                    // 零宽绕过成功（文件确实出现在真正的 data 目录下）
                    Toast.makeText(this, "写入成功（绕过）：" + verifyFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "零宽绕过写入成功: " + verifyFile.getAbsolutePath());
                    return;
                }

                // 绕过失败，清理错误目录中的文件
                Log.w(TAG, "零宽绕过失败，文件写入到了错误目录，回退到 SAF");
                try {
                    bypassFile.delete();
                } catch (Exception ignored) {}

                // 如果无法零宽绕过则回退到 SAF 方式
                writeConfigViaSAF(content, targetApp);
                return;
            }

            // Android 11 (API 30)：直接使用 SAF
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                writeConfigViaSAF(content, targetApp);
                return;
            }

            // Android 10 及以下：直接写入，无 scoped storage 限制，直接写
            String targetDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/Android/data/" + targetApp + "/files/";
            File targetDirFile = new File(targetDir);
            if (!targetDirFile.exists()) targetDirFile.mkdirs();

            File targetFile = new File(targetDir + "config.properties");
            FileOutputStream fos = new FileOutputStream(targetFile, false);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
            Toast.makeText(this, "写入成功：" + targetFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "写入失败: " + e.getMessage());
            Toast.makeText(this, "写入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 通过 SAF写入配置文件
     */
    private void writeConfigViaSAF(String content, String targetApp) {
        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/Android/data/" + targetApp + "/files";
            Uri uri = pathToUri(path, targetApp);
            DocumentFile df = DocumentFile.fromTreeUri(this, uri);

            if (df != null && df.canWrite()) {
                // 已有权限，直接写入
                DocumentFile filesDir = findOrNavigateToFilesDir(df, targetApp);
                if (filesDir == null) {
                    filesDir = df;
                }

                DocumentFile configFile = filesDir.findFile("config.properties");
                if (configFile != null) {
                    // 文件已存在，删除后重建以确保清空内容
                    configFile.delete();
                }
                configFile = filesDir.createFile("application/octet-stream", "config.properties");

                if (configFile != null) {
                    OutputStream os = getContentResolver().openOutputStream(configFile.getUri());
                    if (os != null) {
                        os.write(content.getBytes());
                        os.flush();
                        os.close();
                        Toast.makeText(this, "写入成功（SAF）：" + path, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "SAF 写入成功: " + configFile.getUri());
                        return;
                    }
                }
                Toast.makeText(this, "SAF 写入失败：无法创建文件", Toast.LENGTH_SHORT).show();
            } else {
                // 没有权限，暂存数据并请求授权
                Log.d(TAG, "SAF 无权限，请求用户授权目录");
                pendingContent = content;
                pendingTargetApp = targetApp;
                Toast.makeText(this, "请授权访问目标应用的数据目录", Toast.LENGTH_SHORT).show();
                requestSAFPermission(targetApp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "SAF 写入异常: " + e.getMessage());
            Toast.makeText(this, "SAF 写入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 在 DocumentFile 树中导航到 files 子目录
     */
    private DocumentFile findOrNavigateToFilesDir(DocumentFile root, String targetApp) {
        if (root == null) return null;

        // 尝试直接查找 files 目录
        DocumentFile filesDir = root.findFile("files");
        if (filesDir != null && filesDir.isDirectory()) {
            return filesDir;
        }

        // 如果当前 root 是 Android/data 层级，需要先进入包名目录
        DocumentFile packageDir = root.findFile(targetApp);
        if (packageDir != null && packageDir.isDirectory()) {
            filesDir = packageDir.findFile("files");
            if (filesDir != null && filesDir.isDirectory()) {
                return filesDir;
            }
            // files 目录不存在，创建它
            return packageDir.createDirectory("files");
        }

        return null;
    }

    /**
     * 请求 SAF 目录授权
     * Android 13+ 需要直接授权到包名级别的目录
     */
    private void requestSAFPermission(String targetApp) {
        Uri treeUri;
        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+：必须直接请求到包名级别
            // content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata%2F<package>
            String encoded = "Android%2Fdata%2F" + targetApp;
            treeUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3A" + encoded);
        } else {
            // Android 11-12：请求 Android/data 目录
            treeUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata");
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra("android.provider.extra.INITIAL_URI", treeUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, StoragePermissionManager.REQUEST_CODE_DOCUMENT_TREE);
    }

    /**
     * 将普通路径转换为 SAF 树状 URI
     */
    private Uri pathToUri(String path, String targetApp) {
        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+：tree URI 必须精确到包名级别
            // tree/primary:Android/data/com.xxx.yyy/document/primary:Android/data/com.xxx.yyy/files
            String treePart = "Android%2Fdata%2F" + targetApp;
            String docPart = "Android%2Fdata%2F" + targetApp + "%2Ffiles";
            return Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A"
                    + treePart + "/document/primary%3A" + docPart);
        } else {
            // Android 11-12
            String subPath = path.replace(
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/", "");
            String encodedPath = subPath.replace("/", "%2F");
            return Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A"
                    + encodedPath);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 权限检查和请求
        StoragePermissionManager.checkAndRequestPermission(this);

        binding.button.setOnClickListener(v -> {

            String targetApp = binding.editTextTextPersonName.getText().toString().trim();
            String whiteList = binding.editTextTextPersonName2.getText().toString().trim();
            String blackList = binding.editTextTextPersonName3.getText().toString().trim();

            if (targetApp.isEmpty()) {
                Toast.makeText(this, "请输入目标应用包名", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean hook = binding.switch2.isChecked();
            boolean debugger = binding.switch1.isChecked();
            boolean innerclassesFilter = binding.switch3.isChecked();
            boolean invokeConstructors = binding.invoke.isChecked();
            boolean lazyDump = binding.lazyDump.isChecked();
            String content =
                    "targetApp=" + targetApp + "\n" +
                            "hook=" + hook + "\n" +
                            "invokeDebugger=" + debugger + "\n" +
                            "whiteList=" + whiteList + "\n" +
                            "blackList=" + blackList + "\n" +
                            "lazyDump=" + lazyDump + "\n" +
                            "innerclassesFilter=" + innerclassesFilter + "\n" +
                            "invokeConstructors=" + invokeConstructors + "\n";

            writeConfig(content, targetApp);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == StoragePermissionManager.REQUEST_CODE_ALL_FILES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "所有文件访问权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "未授予权限，无法读写文件", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == StoragePermissionManager.REQUEST_CODE_DOCUMENT_TREE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();

                // 持久化授权
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(uri, takeFlags);

                Toast.makeText(this, "目录授权成功", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "SAF 授权成功: " + uri);

                // 如果有待写入的数据，授权后立即重试写入
                if (pendingContent != null && pendingTargetApp != null) {
                    String content = pendingContent;
                    String targetApp = pendingTargetApp;
                    pendingContent = null;
                    pendingTargetApp = null;

                    // 使用授权后的 URI 直接写入
                    writeConfigWithGrantedUri(content, targetApp, uri);
                }
            } else {
                Toast.makeText(this, "授权被取消", Toast.LENGTH_SHORT).show();
                pendingContent = null;
                pendingTargetApp = null;
            }
        }
    }

    /**
     * 使用授权的 URI 写入配置
     */
    private void writeConfigWithGrantedUri(String content, String targetApp, Uri grantedUri) {
        try {
            DocumentFile root = DocumentFile.fromTreeUri(this, grantedUri);
            if (root == null || !root.canWrite()) {
                Toast.makeText(this, "授权的目录无法写入", Toast.LENGTH_SHORT).show();
                return;
            }

            // 导航到 files 目录
            DocumentFile targetDir = findOrCreatePath(root, targetApp);
            if (targetDir == null) {
                Toast.makeText(this, "无法定位或创建 files 目录", Toast.LENGTH_SHORT).show();
                return;
            }

            // 写入配置文件
            DocumentFile configFile = targetDir.findFile("config.properties");
            if (configFile != null) {
                configFile.delete();
            }
            configFile = targetDir.createFile("application/octet-stream", "config.properties");

            if (configFile != null) {
                OutputStream os = getContentResolver().openOutputStream(configFile.getUri());
                if (os != null) {
                    os.write(content.getBytes());
                    os.flush();
                    os.close();
                    Toast.makeText(this, "写入成功（SAF 授权后）", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "SAF 授权后写入成功: " + configFile.getUri());
                    return;
                }
            }
            Toast.makeText(this, "写入失败：无法创建配置文件", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "SAF 授权后写入异常: " + e.getMessage());
            Toast.makeText(this, "写入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 根据授权的根目录，导航或创建到 files 子目录
     * 授权可能在 Android/data 层级或 Android/data/<package> 层级
     */
    private DocumentFile findOrCreatePath(DocumentFile root, String targetApp) {
        if (root == null) return null;

        // 授权在 Android/data/<package> 层级（Android 13+）
        DocumentFile filesDir = root.findFile("files");
        if (filesDir != null && filesDir.isDirectory()) {
            return filesDir;
        }

        // 尝试创建 files 目录（如果授权在包名层级）
        if (root.findFile(targetApp) == null) {
            // 当前可能就在包名层级，直接创建 files
            filesDir = root.createDirectory("files");
            if (filesDir != null) return filesDir;
        }

        // 授权在 Android/data 层级（Android 11-12）
        DocumentFile packageDir = root.findFile(targetApp);
        if (packageDir != null && packageDir.isDirectory()) {
            filesDir = packageDir.findFile("files");
            if (filesDir != null && filesDir.isDirectory()) {
                return filesDir;
            }
            return packageDir.createDirectory("files");
        }

        return null;
    }
}