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
	AudioRecord recorder;
	AutomaticGainControl mAGC;
	private static int idTestSample;
	private static int soundSource;
	
	private static final int RECORDER_SAMPLERATE = 44100;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
	int BytesPerElement = 2; // 2 bytes in 16bit format
	private static String filePath = null;
	private static String file_wav = null;
	
	boolean isRecording = false;
	private Thread recordingThread = null;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);			
		
		//Prepare inner variables for recording
		idTestSample = R.raw.test;
		soundSource= MediaRecorder.AudioSource.VOICE_RECOGNITION;
		
		if (getIntent().getExtras() != null){
			setContentView(R.layout.post_processing_layout);
			filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
			filePath += "/" + (String) getIntent().getExtras().get("cz.uceeb.refab.logFiles");
			Log.d("PLR", "Opened from file " + filePath);				
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
	
	private void setupRecorder(){
	    recorder = new AudioRecord(soundSource,
				RECORDER_SAMPLERATE, 
				RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING, 
				AudioRecord.getMinBufferSize(	RECORDER_SAMPLERATE,
												RECORDER_CHANNELS, 
												RECORDER_AUDIO_ENCODING)
			);	
}	
	
	public void startPlaying(View v){
		player = new MediaPlayer();

		try {			
			player = MediaPlayer.create(getApplicationContext(), idTestSample);

			player.setOnCompletionListener(new OnCompletionListener() {				
				public void onCompletion(MediaPlayer mp) {
					stopRecording(getCurrentFocus());
				}
			});
			player.start();
		} catch (Exception e) {
			Log.d("AGC","Player failed");
		}		

	}
	

	public void playAndRecord(View v){
		setupRecorder();
		
		if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {	    	
			Log.d("PLR", "Recorder not yet initialized");
			Log.d("PLR", "State:" + recorder.getState());
			Toast.makeText(this, "Recorder not yet initialized.", Toast.LENGTH_SHORT).show();
		} else {
			Log.d("PLR", "Recorder successfully initialized");
			Log.d("PLR", "State:" + recorder.getState());
			startRecording();
			startPlaying(getCurrentFocus());
		}		
	}

	public void stopRecording(View v) {
		Log.d("AGC", "Attempting to stop the recorder.");
			player.release();
			player = null;
		if (null != recorder) {
			isRecording = false;
			recorder.stop();
			recorder.release();
			recorder = null;	
			Log.d("AGC", "Attemt executed");
		    TextView mText = (TextView) findViewById(R.id.status_text_view);
		    mText.setText("Recording finished!");
		}
	}
	
	private void startRecording() {
		recorder.startRecording();
	    isRecording = true;
	    recordingThread = new Thread(new Runnable() {
	        public void run() {
	            writeAudioDataToFile();
	        }
	    }, "AudioRecorder Thread");
	    recordingThread.start();	
	    TextView mText = (TextView) findViewById(R.id.status_text_view);
	    mText.setText("Recording now...");
	}
	
	private void writeAudioDataToFile() {
	    // Write the output audio in byte
		String timeStamp = String.format(Locale.ENGLISH,"%1$tY-%<tm-%<td-%<tH%<tM%<tS", Calendar.getInstance());
		Log.d("PLR", "Generated time: "+timeStamp);
		byte[] header=null;
		Long length;
		byte[] buffer;	
		
		filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        filePath += "/rec_temp"+".pcm";	    
	    short sData[] = new short[BufferElements2Rec];

	    FileOutputStream os = null;
	    FileInputStream is = null;
	    
	    try {
	        os = new FileOutputStream(filePath);	     
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    }
	    Log.d("PLR", "Recording state:"+isRecording);
	    while (isRecording) {
	        // gets the voice output from microphone to byte format

	    	if (recorder != null){
	    		recorder.read(sData, 0, BufferElements2Rec);
	    	}
	        //System.out.println("Short writing to file" + sData.toString());
	        try {
	            // // writes the data to file from buffer
	            // // stores the voice buffer
	            byte bData[] = short2byte(sData);
	            os.write(bData, 0, BufferElements2Rec * BytesPerElement);	                 	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }	    	    
	    try {	    	
	        os.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	   
		try {
		//file_wav = Environment.getExternalStorageDirectory().getAbsolutePath();
		file_wav = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
        file_wav += "/rec_"+timeStamp+".wav";		
        os = new FileOutputStream(file_wav);
		is = new FileInputStream(filePath);
		length = is.getChannel().size();
		buffer = new byte[length.intValue()];						
		is.read(buffer);		
		header = assembleHeader(length);
		os.write(header);
		os.write(buffer);
		os.close();
		MediaScannerConnection.scanFile(this, new String[] {file_wav, filePath}, null, null);
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}		    
			
	}

	private byte[] assembleHeader(long l) {
		Long size = l;
		byte[] RIFF = {0x52, 0x49, 0x46, 0x46};
		byte[] header_1_size = int2byte(size.intValue()-8);
		byte[] WAVE = {0x57, 0x41, 0x56, 0x45};
		byte[] fmt = {0x66, 0x6D, 0x74};
		byte[] format = {0x20, 0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x44, (byte) 0xAC, 0x00, 0x00, (byte) 0x88, 0x58, 0x01, 0x00, 0x02, 0x00, 0x10, 0x00};
		byte[] DATA = {0x64, 0x61, 0x74, 0x61};
		byte[] header_2_size = int2byte(size.intValue());
		byte[] write_d = new byte[44];
		
		write_d[0] = RIFF[0];
		write_d[1] = RIFF[1];
		write_d[2] = RIFF[2];
		write_d[3] = RIFF[3];
		write_d[4] = header_1_size[0];
		write_d[5] = header_1_size[1];
		write_d[6] = header_1_size[2];
		write_d[7] = header_1_size[3];
		write_d[8] = WAVE[0];
		write_d[9] = WAVE[1];
		write_d[10] = WAVE[2];
		write_d[11] = WAVE[3];
		write_d[12] = fmt[0];
		write_d[13] = fmt[1];
		write_d[14] = fmt[2];
		write_d[15] = format[0];
		write_d[16] = format[1];
		write_d[17] = format[2];
		write_d[18] = format[3];
		write_d[19] = format[4];
		write_d[20] = format[5];
		write_d[21] = format[6];
		write_d[22] = format[7];
		write_d[23] = format[8];
		write_d[24] = format[9];
		write_d[25] = format[10];
		write_d[26] = format[11];
		write_d[27] = format[12];
		write_d[28] = format[13];
		write_d[29] = format[14];
		write_d[30] = format[15];
		write_d[31] = format[16];
		write_d[32] = format[17];
		write_d[33] = format[18];
		write_d[34] = format[19];
		write_d[35] = format[20];
		write_d[36] = DATA[0];
		write_d[37] = DATA[1];
		write_d[38] = DATA[2];
		write_d[39] = DATA[3];
		write_d[40] = header_2_size[0];
		write_d[41] = header_2_size[1];
		write_d[42] = header_2_size[2];
		write_d[43] = header_2_size[3];						
		
		return write_d;
	}	
	
	private byte[] short2byte(short[] sData) {
	    int shortArrsize = sData.length;
	    byte[] bytes = new byte[shortArrsize * 2];
	    for (int i = 0; i < shortArrsize; i++) {
	        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
	        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
	        sData[i] = 0;
	    }
	    return bytes;
	}
	
	private byte[] int2byte(int sData) {	    
	    byte[] bytes = new byte[4];

	        bytes[0] = (byte) (sData & 0x000000FF);
	        bytes[1] = (byte) ((sData >> 8) & 0x000000FF);
	        bytes[2] = (byte) ((sData >> 16) & 0x000000FF);
	        bytes[3] = (byte) ((sData >> 24) & 0x000000FF);
	        	    
	    return bytes;
	}
	
	public void plotData(View v){
		Toast toast = Toast.makeText(this, "Pressed plotData", Toast.LENGTH_SHORT);
		toast.show();		
	}	
	
}
