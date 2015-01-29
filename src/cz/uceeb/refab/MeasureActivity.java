package cz.uceeb.refab;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import cz.uceeb.refab.R;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.audiofx.AutomaticGainControl;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

public class MeasureActivity extends Activity {
	MediaPlayer player;
	RefabRecorder recorder;
	AutomaticGainControl mAGC;
	private static int idTestSample;
	private static int soundSource;
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);			
		
		//Prepare inner variables for recording
		idTestSample = R.raw.test;
		soundSource= MediaRecorder.AudioSource.VOICE_RECOGNITION;
		
		//Create an instance of the RefabRecorder
		recorder = new RefabRecorder(this, soundSource);
		
		if (getIntent().getExtras() != null){
			setContentView(R.layout.post_processing_layout);
			//filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
			//filePath += "/" + (String) getIntent().getExtras().get("cz.uceeb.refab.logFiles");
			//Log.d("PLR", "Opened from file " + filePath);				
		} else {
			setContentView(R.layout.layout);
			Log.d("PLR", "Opened from main menu");
		}	
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		//plotData();
	}		


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_activity_measure, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}	
	
	
	//***************************************
	//Methods for executing the operation
	//***************************************
	
	public void startPlaying(View v){
		player = new MediaPlayer();

		try {			
			player = MediaPlayer.create(getApplicationContext(), idTestSample);

			player.setOnCompletionListener(new OnCompletionListener() {				
				public void onCompletion(MediaPlayer mp) {
					mp.release();
					mp = null;		
					recorder.stop();					
				    TextView mText = (TextView) findViewById(R.id.status_text_view);
				    mText.setText("Recording finished!");
				}
			});
			player.start();
		} catch (Exception e) {
			Log.d("AGC","Player failed");
		}		

	}
	

	public void playAndRecord(View v){	
		if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {	    	
			Log.d("PLR", "Recorder not yet initialized");
			Log.d("PLR", "State:" + recorder.getState());
			Toast.makeText(this, "Recorder not yet initialized.", Toast.LENGTH_SHORT).show();
		} else {
			Log.d("PLR", "Recorder successfully initialized");
			Log.d("PLR", "State:" + recorder.getState());
			//startRecording();
			//Start the RefabRecorder
			recorder.startRecording();
			startPlaying(getCurrentFocus());
			TextView mText = (TextView) findViewById(R.id.status_text_view);
		    mText.setText("Recording now...");
		}		
	}

	
	public void plotData(View v){
		//Verify that string is not null
		//Signal signal = new Signa(rawTempFile);
		//signal.process();
		//signal.getReflectifity();
		
		Toast toast = Toast.makeText(this, "Pressed plotData", Toast.LENGTH_SHORT);
		toast.show();		
	}	
	
}
