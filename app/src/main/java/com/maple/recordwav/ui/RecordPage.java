package com.maple.recordwav.ui;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.maple.recorder.recording.AudioChunk;
import com.maple.recorder.recording.AudioRecordConfig;
import com.maple.recorder.recording.MsRecorder;
import com.maple.recorder.recording.PullTransport;
import com.maple.recorder.recording.Recorder;
import com.maple.recordwav.R;
import com.maple.recordwav.WavApp;
import com.maple.recordwav.base.BaseFragment;
import com.maple.recordwav.utils.DateUtils;
import com.maple.recordwav.utils.T;

import java.io.File;

/**
 * 录制 WavRecorder 界面
 *
 * @author maple
 * @time 16/4/18 下午2:53
 */
public class RecordPage extends BaseFragment {

	private ImageView ivVoiceImg;
	private Chronometer comVoiceTime;
	private Button btStart;
	private Button btPauseResume;
	private Button btStop;
	private CheckBox skipSilence;

	private boolean isRecording = false;
	private long curBase = 0;
	private String voicePath = WavApp.rootPath + "/voice.wav";

	private Recorder recorder;

	@Override
	public View initView(LayoutInflater inflater) {
		view = inflater.inflate(R.layout.fragment_record, null);
		if (view != null) {
			initView();
		}
		return view;
	}
	private void initView() {
		ivVoiceImg = view.findViewById(R.id.iv_voice_img);
		comVoiceTime = view.findViewById(R.id.com_voice_time);
		btStart = view.findViewById(R.id.bt_start);
		btPauseResume = view.findViewById(R.id.bt_pause_resume);
		btStop = view.findViewById(R.id.bt_stop);
		skipSilence = view.findViewById(R.id.skipSilence);
	}

	@Override
	public void initData(Bundle savedInstanceState) {
		String name = "wav-" + DateUtils.date2Str("yyyy-MM-dd-HH-mm-ss");
		voicePath = WavApp.rootPath + name + ".wav";

		setupRecorder();
		btStart.setText(getString(R.string.record));
		btPauseResume.setText(getString(R.string.pause));
		btStop.setText(getString(R.string.stop));
		btPauseResume.setEnabled(false);
		btStop.setEnabled(false);
	}

	@Override
	public void initListener() {
		skipSilence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
				if (isChecked) {
					setupNoiseRecorder();
				} else {
					setupRecorder();
				}
			}
		});
		btStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				recorder.startRecording();

				isRecording = true;
				skipSilence.setEnabled(false);
				btStart.setEnabled(false);
				btPauseResume.setEnabled(true);
				btStop.setEnabled(true);
				ivVoiceImg.setImageResource(R.drawable.mic_selected);
				comVoiceTime.setBase(SystemClock.elapsedRealtime());
				comVoiceTime.start();
			}
		});
		btStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				recorder.stopRecording();

				isRecording = false;
				skipSilence.setEnabled(true);
				btStart.setEnabled(true);
				btPauseResume.setEnabled(false);
				btStop.setEnabled(false);
				btPauseResume.setText(getString(R.string.pause));
				ivVoiceImg.setImageResource(R.drawable.mic_default);
				comVoiceTime.stop();
				curBase = 0;
				btStop.post(new Runnable() {
					@Override
					public void run() {
						animateVoice(0);
					}
				});
			}
		});
		btPauseResume.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				if (isRecording) {
					recorder.pauseRecording();

					isRecording = false;
					btPauseResume.setText(getString(R.string.resume));
					curBase = SystemClock.elapsedRealtime() - comVoiceTime.getBase();
					comVoiceTime.stop();
					ivVoiceImg.setImageResource(R.drawable.mic_default);
					btPauseResume.postDelayed(new Runnable() {
						@Override
						public void run() {
							animateVoice(0);
						}
					}, 100);
				} else {
					recorder.resumeRecording();

					isRecording = true;
					btPauseResume.setText(getString(R.string.pause));
					comVoiceTime.setBase(SystemClock.elapsedRealtime() - curBase);
					comVoiceTime.start();
					ivVoiceImg.setImageResource(R.drawable.mic_selected);
				}
			}
		});

	}

	private void setupRecorder() {
		recorder = MsRecorder.wav(
				new File(voicePath),
				new AudioRecordConfig.Default(),
				new PullTransport.Default()
						.setOnAudioChunkPulledListener(new PullTransport.OnAudioChunkPulledListener() {
							@Override
							public void onAudioChunkPulled(AudioChunk audioChunk) {
								Log.e("max  ", "amplitude: " + audioChunk.maxAmplitude());
								animateVoice((float) (audioChunk.maxAmplitude() / 200.0));
							}
						})
		);
	}

	private void setupNoiseRecorder() {
		recorder = MsRecorder.wav(
				new File(voicePath),
				new AudioRecordConfig.Default(),
				new PullTransport.Noise()
						.setOnAudioChunkPulledListener(new PullTransport.OnAudioChunkPulledListener() {
							@Override
							public void onAudioChunkPulled(AudioChunk audioChunk) {
								animateVoice((float) (audioChunk.maxAmplitude() / 200.0));
							}
						})
						.setOnSilenceListener(new PullTransport.OnSilenceListener() {
							@Override
							public void onSilence(long silenceTime, long discardTime) {
								String message = "沉默时间：" + String.valueOf(silenceTime) +
										" ,丢弃时间：" + String.valueOf(discardTime);
								Log.e("silenceTime", message);
								T.showShort(mContext, message);
							}
						})


		);
	}


	private void animateVoice(float maxPeak) {
		if (maxPeak > 0.5f) {
			return;
		}
		ivVoiceImg.animate()
				.scaleX(1 + maxPeak)
				.scaleY(1 + maxPeak)
				.setDuration(10)
				.start();
	}

}
