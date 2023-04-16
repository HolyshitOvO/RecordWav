package com.maple.recorder.recording;

import android.media.AudioRecord;
import android.os.Build;

import androidx.annotation.RequiresPermission;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base Recorder (Only record the original audio data.)
 *
 * @author maple
 * @time 2018/4/10.
 */
public class BaseDataRecorder implements Recorder {
    protected PullTransport pullTransport;
    protected AudioRecordConfig config;
    protected int pullSizeInBytes;// 缓冲区大小
    protected File file;

    private AudioRecord audioRecord;
    private OutputStream outputStream;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Runnable recordingTask = new Runnable() {
        @Override
        @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
        public void run() {
            startRecord();
        }
    };

    protected BaseDataRecorder(File file, AudioRecordConfig config, PullTransport pullTransport) {
        this.file = file;
        this.config = config;
        this.pullTransport = pullTransport;
        // 计算缓冲区大小
        this.pullSizeInBytes = AudioRecord.getMinBufferSize(
                config.frequency(),
                config.channelPositionMask(),
                config.audioEncoding()
        );
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    private void startRecord() {
        try {
            if (audioRecord == null) {
                audioRecord = new AudioRecord(config.audioSource(), config.frequency(),
                        config.channelPositionMask(), config.audioEncoding(), pullSizeInBytes);
            }
            if (outputStream == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    outputStream = Files.newOutputStream(file.toPath());
                }else{
                    outputStream = new FileOutputStream(file);
                }
            }
            audioRecord.startRecording();
            pullTransport.isEnableToBePulled(true);
            pullTransport.startPoolingAndWriting(audioRecord, pullSizeInBytes, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startRecording() {
        executorService.submit(recordingTask);
    }

    @Override
    public void pauseRecording() {
        pullTransport.isEnableToBePulled(false);
    }

    @Override
    public void resumeRecording() {
        startRecording();
    }

    @Override
    public void stopRecording() {
        pauseRecording();

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
                outputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
