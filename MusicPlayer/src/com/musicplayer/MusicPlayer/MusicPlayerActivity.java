package com.musicplayer.MusicPlayer;

import orbotix.view.connection.SpheroConnectionView;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MusicPlayerActivity extends Activity {
	MediaPlayer mMediaPlayer;
	StreamingActivity activity;
	private EditText roomname;
	private Button next, shuffle, play;
	private SeekBar volume;
	private int volumeNum;
	private boolean isShuffled, isPaused;
	private String myRoom;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = new StreamingActivity(this);
		setContentView(R.layout.main);
		roomname = (EditText) findViewById(R.id.partyname);
		next = (Button) findViewById(R.id.next);
		play = (Button) findViewById(R.id.play);
		shuffle = (Button) findViewById(R.id.shuffle);
		volume = (SeekBar) findViewById(R.id.volume);
		activity.onCreate((SpheroConnectionView) findViewById(R.id.sphero_connection_view));
		roomname.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
			}

			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}

			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				myRoom = arg0.toString();
			}
		});
		volume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				volumeNum = arg1;
				
			}

			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar arg0) {
				send_msg_type(false);				
			}
			
		});
		shuffle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String next = isShuffled ? "Shuffle Off" : "Shuffle On";
				((Button) v).setText(next);
				isShuffled = !isShuffled;
				send_msg_type(false);
			}
		});
		next.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				isPaused = false;
				((Button) findViewById(R.id.play)).setText("Pause");
				send_msg_type(false);
			}
		});
		play.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String next = isPaused ? "Play" : "Pause";
				((Button) v).setText(next);
				isPaused = !isPaused;
				send_msg_type(true);
			}
		});
	}
	public void onStop() {
		activity.onStop();
	}
	public void send_msg_type(boolean pause) {
		Intent intent = new Intent(this, MusicPlayerService.class);
		intent.putExtra(MusicPlayerService.EXTRA_PAUSE, pause);
		intent.putExtra(MusicPlayerService.EXTRA_ROOMNAME, myRoom);
		intent.putExtra(MusicPlayerService.EXTRA_SHUFFLE, false);
		intent.putExtra(MusicPlayerService.EXTRA_VOLUME, volumeNum);
		startService(intent);
	}
	
	public void onResume() {
		super.onResume();
		newMessage messageReceiver = new newMessage();
		messageReceiver.currentSong = (TextView) findViewById(R.id.currentsong);
		registerReceiver(messageReceiver, new IntentFilter(
				MusicPlayerService.BROADCAST_DATA));
	}
	
	public void update(String s) {
		if (s == "voldown") {
			if (volumeNum > 10) volumeNum -= 10;
			send_msg_type(false);
		}
	}
}

class newMessage extends BroadcastReceiver {
	public TextView currentSong;
	@Override
	public void onReceive(Context context, Intent intent) {
		String songname = intent.getStringExtra("message");
		currentSong.setText("Now playing " + songname);
	}
}

