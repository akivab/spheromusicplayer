package com.musicplayer.MusicPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.AsyncTask;
import android.os.IBinder;

public class MusicPlayerService extends Service implements
		AudioManager.OnAudioFocusChangeListener {
	private MediaPlayer mMediaPlayer;
	public static final String EXTRA_ROOMNAME = "EXTRA_ROOMNAME";
	public static final String EXTRA_SHUFFLE = "EXTRA_SHUFFLE";
	public static final String EXTRA_PAUSE= "EXTRA_PAUSE";
	public static final String EXTRA_VOLUME = "EXTRA_VOLUME";

	public static final String BROADCAST_DATA = "com.musicplayer.MusicPlayer.dataReceiver";
	private String roomName;
	private boolean shuffle;
	private boolean isPaused;
	private int volumeNum;

	public byte[] getRawWaveform() {
		byte[] rawWaveForm = null;
		Visualizer visual = new Visualizer(0);
		visual.setEnabled(true);
		// /get rawWaveForm
		visual.getWaveForm(rawWaveForm);
		return rawWaveForm;
	}
	
	public void pauseMusic() {
		if (!isPaused) {
			mMediaPlayer.pause();
		} else {
			mMediaPlayer.start();
		}
		isPaused = !isPaused;
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		String roomname = intent.getStringExtra(EXTRA_ROOMNAME);
		boolean useShuffle = intent.getBooleanExtra(EXTRA_SHUFFLE, false);
		boolean shouldPause = intent.getBooleanExtra(EXTRA_PAUSE, false);
		int volume = intent.getIntExtra(EXTRA_VOLUME, 10);
		
		if (shouldPause && mMediaPlayer != null) {
			pauseMusic();
		} else if (volume != volumeNum) {
			volumeNum = volume;
			float vol = volumeNum / 100.0f;
			if (mMediaPlayer != null)
			mMediaPlayer.setVolume(vol, vol);
		} else {
			setInfo(roomname, useShuffle);
			new RequestTask().execute(getSongUrl());
			isPaused = false;
		} 
		return START_NOT_STICKY;
	}

	public String getSongUrl() {
		String base = "http://spheromusic.appspot.com/getsong?roomname=" + roomName;
		if (shuffle) base += "&shuffle=on";
		return base;
	}

	public void setInfo(String room, boolean shuffle) {
		this.roomName = room;
		this.shuffle = shuffle;
	}

	public void playSong(String url) {
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
		}
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			mMediaPlayer.setDataSource(url);
			mMediaPlayer.prepare(); // might take long! (for buffering, etc)
			mMediaPlayer.start();
		} catch (Exception e) {
		}
	}

	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_GAIN:
			// resume playback
			if (mMediaPlayer == null)
				onCreate();
			else if (!mMediaPlayer.isPlaying())
				mMediaPlayer.start();
			mMediaPlayer.setVolume(1.0f, 1.0f);
			break;

		case AudioManager.AUDIOFOCUS_LOSS:
			// Lost focus for an unbounded amount of time: stop playback and
			// release media player
			if (mMediaPlayer.isPlaying())
				mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			// Lost focus for a short time, but we have to stop
			// playback. We don't release the media player because playback
			// is likely to resume
			if (mMediaPlayer.isPlaying())
				mMediaPlayer.pause();
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			// Lost focus for a short time, but it's ok to keep playing
			// at an attenuated level
			if (mMediaPlayer.isPlaying())
				mMediaPlayer.setVolume(0.1f, 0.1f);
			break;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void sendMessage(String message) {
		Intent i = new Intent(BROADCAST_DATA);
		i.putExtra("message", message);
		sendBroadcast(i);
	}

	class RequestTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... uri) {
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
			String responseString = null;
			try {
				response = httpclient.execute(new HttpGet(uri[0]));
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					out.close();
					responseString = out.toString();
				} else {
					// Closes the connection.
					response.getEntity().getContent().close();
					throw new IOException(statusLine.getReasonPhrase());
				}
			} catch (Exception e) {
			}
			return responseString;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			String songname = "error";
			String songurl = "error";
			try {
				JSONObject obj = new JSONObject(result);
				songname = obj.getString("songname");
				songurl = obj.getString("songurl");
			} catch (Exception e) {
				songname = e.toString();
			}
			sendMessage(songname);
			playSong(songurl);
		}
	}
}