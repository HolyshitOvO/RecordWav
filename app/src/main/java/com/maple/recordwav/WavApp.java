package com.maple.recordwav;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class WavApp extends Application {
    private static WavApp app;

    public static String rootPath = "/wav_file/";
    public static File saveFile;
    @Override
    public void onCreate() {
        app = this;
        super.onCreate();

        // ACRA.init(this);

        initPath();
        initFile();
    }

    /**
     * 初始化存储路径
     */
    private void initPath() {
        String ROOT = "";// /storage/emulated/0
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            ROOT = getBaseContext().getFilesDir().getPath();
            ROOT = Environment.getExternalStorageDirectory().getPath();
            Log.e("app", "系统方法：" + ROOT);
        }
        rootPath = ROOT + rootPath;

        File lrcFile = new File(rootPath);
        if (!lrcFile.exists()) {
            lrcFile.mkdirs();
        }
    }

    /**
     * 获取存储路径
     */
    private void initFile() {
        if (saveFile == null) {
            saveFile = getApplicationContext().getExternalFilesDir("wav_file");
        }
        if (!saveFile.exists()) {
            saveFile.mkdirs();
            saveFile = getApplicationContext().getExternalFilesDir("wav_file");
        }
        Log.d("maple_log", "saveFile：" + saveFile.getAbsolutePath());
    }

    public static WavApp app() {
        return app;
    }


}
