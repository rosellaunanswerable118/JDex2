package com.jsnow.jdex2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.jsnow.jdex2.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

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

                String targetApp = binding.editTextTextPersonName.getText().toString().trim();
                String whiteList = binding.editTextTextPersonName2.getText().toString().trim();
                String blackList = binding.editTextTextPersonName3.getText().toString().trim();

                boolean hook = binding.switch2.isChecked();
                boolean debugger = binding.switch1.isChecked();
                boolean innerclassesFilter =binding.switch3.isChecked();
                String content =
                        "targetApp=" + targetApp + "\n" +
                                "hook=" + hook + "\n" +
                                "invokeDebugger=" + debugger + "\n" +
                                "whiteList=" + whiteList + "\n" +
                                "blackList=" + blackList + "\n" +
                                "innerclassesFilter=" + innerclassesFilter + "\n";

                writeConfigToSdcard(content);
            }
        });
    }

    private void writeConfigToSdcard(String content) {
        try {
            File file = new File("/sdcard/config.properties");

            FileOutputStream fos = new FileOutputStream(file, false);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();

            Toast.makeText(this, "写入成功：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "写入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
