package cz.uceeb.refab;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import cz.uceeb.refab.R;
import cz.uceeb.refab.data.Signal;
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
					player.release();
					player = null;		
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
	
	public void processRecordings(View v){
		//Verify that string is not null
		TextView tv = (TextView) findViewById(R.id.status_text_view);
		tv.setText("Processing recording");
		Signal signal;
		if (getIntent().getExtras() != null){
			// TODO Path changed - redo to reflect creating new folder for refab and timestamp
			String fileWavPath;
			fileWavPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
			fileWavPath += "/" + (String) getIntent().getExtras().get("cz.uceeb.refab.logFiles");			   
			signal = new Signal(this, fileWavPath, 16);
		} else {
			Log.d("PLR", recorder.getWavFilePath());
			signal = new Signal(this, recorder.getRawFilePath(), recorder.getWavFilePath(), 16);
		}
		signal.processData();
		tv.setText("Measured distance is: " + signal.getDistance());
		tv = (TextView) findViewById(R.id.busrsts_detected_text_view);
		tv.setText("# Bursts: " + signal.getNumberOfBurstsDetected());

		drawPlot(signal.getFrequencies(), signal.getReflectivity());
	}	
	
	public void drawPlot(double[] xData, BigDecimal[] yData) {
		XYPlot plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
		Number[] series1Numbers, domainNumbers;
		if (xData.length != yData.length){
			return;
		}
		series1Numbers = new Number[xData.length];
		domainNumbers = new Number[yData.length];
		
        // Create a couple arrays of y-values to plot:
		for (int i = 0; i< yData.length; i++) {
			series1Numbers[i]=yData[i];
			domainNumbers[i]=xData[i];			
		}
        //Number[] series1Numbers = {1, 8, 5, 2, 7, 4};
        //Number[] series2Numbers = {4, 6, 3, 8, 2, 10};                

        // Turn the above arrays into XYSeries':
        XYSeries series1 = new SimpleXYSeries(
        		Arrays.asList(domainNumbers),          // SimpleXYSeries takes a List so turn our array into a List
                Arrays.asList(series1Numbers), // Y_VALS_ONLY means use the element index as the x value
                "Series1");                             // Set the display title of the series
 
        // same as above
    
        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();     
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(this,
                R.xml.line_formatter_1);
 
        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);        
 
        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);
        plot.redraw();
        plot.setDomainLabel("Frequency[Hz]");
        plot.setRangeLabel("Reflectivity[-]");
		
	}

	
}
