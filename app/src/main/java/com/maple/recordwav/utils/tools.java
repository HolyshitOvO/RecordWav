package com.maple.recordwav.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.maple.recordwav.WavApp;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class tools {
	private static final String TAG = "tools";
	/**
	 *
	 * @param audioPath
	 * @param audioStartTimeUs -1 表示不截取
	 * @param audioEndTimeUs -1 表示不截取
	 * @return
	 */
	public static String audioToAAC(String audioPath,long audioStartTimeUs,long audioEndTimeUs){

		long a=System.currentTimeMillis();
		int audioExtractorTrackIndex=-1;
		int audioMuxerTrackIndex=-1;
		int channelCount=1;
		int sourceSampleRate = 0;
		String newAudioAAc = "";
		long sourceDuration=0;

		try {
			File tempFlie = new File(audioPath);
			String tempName = tempFlie.getName();
			String suffix = tempName.substring(tempName.lastIndexOf(".") + 1);
			//             newAudioAAc= tempFlie.getParentFile().getAbsolutePath() + "/" + tempName.replace(suffix, "aac");
			newAudioAAc= WavApp.rootPath + tempName.replace(suffix, "aac");
			//            muxer = new MediaMuxer(newAudioAAc, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
			Log.i(TAG,"audioPath=="+audioPath+",newAudioAAC=="+newAudioAAc+",newAudioAAc=="+newAudioAAc);
			//音频信息获取
			MediaExtractor audioExtractor = new MediaExtractor();
			audioExtractor.setDataSource(audioPath);
			int trackCount = audioExtractor.getTrackCount();

			MediaFormat sourceFormat=null;
			String sourceMimeType="";
			int timeOutUs=300;

			for (int i = 0; i < trackCount; i++) {
				sourceFormat = audioExtractor.getTrackFormat(i);
				sourceMimeType = sourceFormat.getString(MediaFormat.KEY_MIME);
				channelCount=sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
				sourceSampleRate = sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
				sourceDuration = sourceFormat.getLong(MediaFormat.KEY_DURATION);
				if (sourceMimeType.startsWith("audio/")) { //找到音轨
					Log.i(TAG,"sourceMimeType=="+sourceMimeType+",channelCount=="+channelCount+",sourceSampleRate=="+sourceSampleRate);
					audioExtractorTrackIndex = i;
					break;
				}
			}

			//初始化解码器
			MediaCodec audioDecoder=null;
			audioDecoder = MediaCodec.createDecoderByType(sourceMimeType);
			audioDecoder.configure(sourceFormat, null, null, 0);
			audioDecoder.start();

			//初始化编码
			MediaCodec mEncorder=null;
			mEncorder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
			MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, channelCount);
			format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
			format.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
			format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024*1024 * 10);
			mEncorder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			mEncorder.start();

			audioExtractor.selectTrack(audioExtractorTrackIndex);

			MediaCodec.BufferInfo sourceAudioBufferInfo = new MediaCodec.BufferInfo();
			MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

			ByteBuffer audioByteBuffer = ByteBuffer.allocate(1024*1024*10);
			FileOutputStream mFileStream = new FileOutputStream(newAudioAAc);

			while (true) {
				int readSampleSize = audioExtractor.readSampleData(audioByteBuffer, 0);
				Log.i(TAG, "readSampleSize==" + readSampleSize+",audioByteBuffer.limit=="+audioByteBuffer.limit()+",timeOutUs=="+timeOutUs);
				if (readSampleSize < 0) {
					audioExtractor.unselectTrack(audioExtractorTrackIndex);
					break;
				}

				long audioSampleTime=audioExtractor.getSampleTime();

				//可以做进度回调
				Log.i(TAG, "audioSampleTime==" +audioSampleTime+",,progress=="+((float)audioSampleTime/(float)sourceDuration));
				if (audioStartTimeUs !=-1 && audioSampleTime < audioStartTimeUs) {
					audioExtractor.advance();
					continue;
				}

				if (audioEndTimeUs !=-1 && audioSampleTime > audioEndTimeUs) {
					break;
				}

				int audioSampleFlags=audioExtractor.getSampleFlags();

				//解码
				int sourceInputBufferIndex = audioDecoder.dequeueInputBuffer(timeOutUs);
				if (sourceInputBufferIndex >= 0) {
					ByteBuffer sourceInputBuffer = audioDecoder.getInputBuffer(sourceInputBufferIndex);
					sourceInputBuffer.clear();
					sourceInputBuffer.put(audioByteBuffer);
					audioDecoder.queueInputBuffer(sourceInputBufferIndex, 0, readSampleSize, audioSampleTime, audioSampleFlags);
				}


				int sourceOutputBufferIndex = audioDecoder.dequeueOutputBuffer(sourceAudioBufferInfo, timeOutUs);
				if (sourceOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					// 后续输出格式变化
				}
				while (sourceOutputBufferIndex >= 0) {
					ByteBuffer decoderOutputBuffer = audioDecoder.getOutputBuffer(sourceOutputBufferIndex);



					//编码
					int inputBufferIndex = mEncorder.dequeueInputBuffer(timeOutUs);
					if (inputBufferIndex >= 0) {
						ByteBuffer inputBuffer = mEncorder.getInputBuffer(inputBufferIndex);
						inputBuffer.clear();
						inputBuffer.put(decoderOutputBuffer);
						mEncorder.queueInputBuffer(inputBufferIndex, 0, decoderOutputBuffer.limit(), audioSampleTime, audioSampleFlags);
					}

					int outputBufferIndex = mEncorder.dequeueOutputBuffer(audioBufferInfo, timeOutUs);
					if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
						// 后续输出格式变化
					}
					while (outputBufferIndex >= 0) {

						ByteBuffer outputBuffer = mEncorder.getOutputBuffer(outputBufferIndex);
						int outBufferSize = outputBuffer.limit() + 7;
						byte[] aacBytes = new byte[outBufferSize];
						addADTStoPacket(aacBytes, outBufferSize, channelCount);
						outputBuffer.get(aacBytes, 7, outputBuffer.limit());

						mFileStream.write(aacBytes);

						mEncorder.releaseOutputBuffer(outputBufferIndex, false);
						outputBufferIndex = mEncorder.dequeueOutputBuffer(audioBufferInfo, timeOutUs);
					}




					audioDecoder.releaseOutputBuffer(sourceOutputBufferIndex, false);
					sourceOutputBufferIndex = audioDecoder.dequeueOutputBuffer(sourceAudioBufferInfo, timeOutUs);
				}
				audioExtractor.advance();
			}

			//释放资源
			mEncorder.stop();
			mFileStream.flush();
			mFileStream.close();
			long b=System.currentTimeMillis()-a;
			Log.i(TAG,"编码结束=="+b);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newAudioAAc;
	}

	public static void addADTStoPacket(byte[] packet, int packetLen, int chancfg) {
		int profile = 2;  //AAC LC，MediaCodecInfo.CodecProfileLevel.AACObjectLC;
		int freqIdx = 4;  //见后面注释avpriv_mpeg4audio_sample_rates中441000对应的数组下标，来自ffmpeg源码
		//        int chanCfg = 1;  //见后面注释channel_configuration，AudioFormat.CHANNEL_IN_MONO 单声道(声道数量)
		int chanCfg = chancfg;  //见后面注释channel_configuration，AudioFormat.CHANNEL_IN_MONO 单声道(声道数量)

        /*int avpriv_mpeg4audio_sample_rates[] = {96000, 88200, 64000, 48000, 44100, 32000,24000, 22050, 16000, 12000, 11025, 8000, 7350};
        channel_configuration: 表示声道数chanCfg
        0: Defined in AOT Specifc Config
        1: 1 channel: front-center
        2: 2 channels: front-left, front-right
        3: 3 channels: front-center, front-left, front-right
        4: 4 channels: front-center, front-left, front-right, back-center
        5: 5 channels: front-center, front-left, front-right, back-left, back-right
        6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
        7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
        8-15: Reserved
        */

		// fill in ADTS data
		packet[0] = (byte)0xFF;
		//        packet[1] = (byte)0xF9;
		packet[1] = (byte)0xF1; //解决ios 手机不能播放问题
		packet[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
		packet[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
		packet[4] = (byte)((packetLen&0x7FF) >> 3);
		packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);
		packet[6] = (byte)0xFC;

	}
}
