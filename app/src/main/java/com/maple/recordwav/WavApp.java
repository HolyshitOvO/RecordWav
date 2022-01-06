package com.maple.recordwav;

import android.app.Application;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.File;

@ReportsCrashes(
        mode = ReportingInteractionMode.DIALOG,
        mailTo = "shaoshuai904@gmail.com",
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = R.drawable.ic_launcher,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        // resDialogTheme = R.style.AppTheme_Dialog,
        resDialogOkToast = R.string.crash_dialog_ok_toast
)
public class WavApp extends Application {
    private static WavApp app;
    private static File saveFile;

    @Override
    public void onCreate() {
        app = this;
        super.onCreate();

        ACRA.init(this);
    }

    /**
     * 获取存储路径
     */
    public static File getSaveFile() {
        if (saveFile == null) {
            saveFile = new File(app().getExternalFilesDir(""), "wav_file");
        }
        if (!saveFile.exists()) {
            saveFile.mkdirs();
        }
        Log.d("maple_log", "saveFile：" + saveFile.getAbsolutePath());
        return saveFile;
    }


    public static WavApp app() {
        return app;
    }


}
