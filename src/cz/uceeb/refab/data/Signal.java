package cz.uceeb.refab.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import cz.uceeb.refab.R;

public class Signal {
	
	private double distance;
	private double[] noise, data;
	private ArrayList<Burst> incident,reflected;
	private double[] reflectivity;
	private String material;
	private int numberOfBurstsExpected,numberOfBurstsDetected;
	private int[] startIndicesIncident, startIndicesReflected;
	private int burstDelayInSamples, burstLengthInSamples, burstDistanceInSamples;
	private int startIndexNoise, stopIndexNoise;
	private int noiseLength;
	private String filePath;
	
	Context context = null;
	
	public Signal(Context c, String path) {
		this.filePath = path;
		this.context = c; 
	}
	
	public Signal(String path, int numberOfBurstsExpected) {
		this.numberOfBurstsExpected = numberOfBurstsExpected;
	}

	public void processData(View v) {
		Long length = null;
		int lengthNoise;
		int lengthBurst;
		int[] indices;
		byte[] buffer = null;
		byte[] bufferNoise = null;
		byte[] bufferBurst = null;
		short[] plotData = null;
		short[] noiseData = null;
		short[] burstData = null;
		short[] burstRegion = null;
		short[] noiseExtracted = null;
		FileInputStream is = null;
		InputStream noiseTemplate = null;
		InputStream burstTemplate = null;
		double[] data_corr = null;
		double[] data_corr_butter = null;
		//double[] data_sub = null;
		double time;
		//double distance;
		int maximum;
		int peak;	
		distance = 0;
				
		TextView mText = (TextView) findViewById(R.id.status_text_view);
				
		if (filePath==null){ 			
			mText.setText("No file recorded yet");
			Log.d("PLR", "No file recorded yet");
			return;		
		}
		try {
		is = new FileInputStream(filePath);		
		length = is.getChannel().size();
		buffer = new byte[length.intValue()];
		plotData = new short[length.intValue()];				
		is.read(buffer);		
		
		
		//Uri pathNoise = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.noise);		
		noiseTemplate = context.getResources().openRawResource(R.raw.noise);
		burstTemplate = context.getResources().openRawResource(R.raw.burst6);
		lengthNoise = noiseTemplate.available();
		lengthBurst = burstTemplate.available();
		//lengthNoise = noiseTemplate.getChannel().size();
		Log.d("PLR", "Size of noise is: " + String.valueOf(lengthNoise));
		Log.d("PLR", "Size of burst is: " + String.valueOf(lengthBurst));
		bufferNoise = new byte[lengthNoise];		
		bufferBurst = new byte[lengthBurst];
		noiseTemplate.read(bufferNoise);	
		burstTemplate.read(bufferBurst);
		
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}		
		plotData = byte2short(buffer);
		Log.d("PLR", "plotData converted successfully.");
		noiseData = byte2short(bufferNoise);
		Log.d("PLR", "noiseData converted successfully.");
		burstData = byte2short(bufferNoise);
		Log.d("PLR", "burstData converted successfully.");
		// The noise is recognized to be between following indices: 4300 and 22100
		//noiseExtracted = simpleMath.getNoiseStart(noiseData, plotData);		
		noiseStart = simpleMath.getNoiseStartIndex(noiseData, plotData);
		noiseExtracted = simpleMath.getSubsequent(noiseStart, noiseStart+NOISE_CUTOFF, plotData);
		Log.d("PLR", "Noise extracted successfully.");
		// Detecting the regions with bursts - there should be no more than as many bursts as in the test file 
		// burstRegion = simpleMath.getBurstRegion(noiseData, plotData);
		burstRegion = simpleMath.getSubsequent(noiseStart+NOISE_CUTOFF, plotData.length-1, plotData);
		Log.d("PLR", "Burst region extracted...");
		indices = simpleMath.getBurstIndices(burstRegion); // returns burst indices from the data without noise
		Log.d("PLR", "Burst indices obtained.");
		
		data_corr = new double[2*noiseExtracted.length];
		data_corr = simpleMath.correlation_fast(noiseExtracted);
		Log.d("PLR", "Correlation computed.");
		//drawPlot(data_corr);
		data_corr = simpleMath.abs(data_corr);		
		Log.d("PLR", "Abs computed.");
		data_corr_butter = simpleMath.butterworth(data_corr);
		Log.d("PLR", "Filter applied.");
		//drawPlot(data_corr_butter);
		//Log.d("PLR", "Data plotted.");
		
		peak = simpleMath.localMax(data_corr_butter);
		data_sub = simpleMath.getSubsequent(peak+50, peak+140, data_corr_butter);		
		maximum = simpleMath.localMax(data_sub);
		time = (double)(maximum+50)/RECORDER_SAMPLERATE;
		distance = (time*340/2)*100;
		
		TextView bursts = (TextView) findViewById(R.id.busrsts_detected_text_view);
		bursts.setText("#Bursts: "+ indices.length);
		impedance = new double[indices.length];
		frequency = new double[indices.length];
		int i = 0;
		
 		for (int d : indices) { 			
 			// It is expected that the burst length is 4ms resulting in 200 samples. Allowing twice the size for safe detection.
			burstData = simpleMath.getSubsequent(d, d+400, burstRegion);
			
			// drawPlotClean(burstData);
			
			impedance[i] = simpleMath.calcImpedance(burstData, 0.15, distance, 0.0 );
			Log.d("PLR", "Impedance calculated");
			frequency[i] = simpleMath.calcFrequency(simpleMath.getSubsequent(d, d+197, burstRegion));
			Log.d("PLR", "Frequency calculated");
			i++;
		}
		
		viz();
		writeLog(indices);
	}

	private void writeLog(int[] burstsIndices){
		String fileLog = null;
		//fileLog = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
		if (file_wav == null){
			return;
		}
		fileLog = file_wav;
		fileLog += ".log";
		FileOutputStream os;
		PrintWriter writer;
		
        	
        try {            
			os = new FileOutputStream(fileLog);
			writer = new PrintWriter(os);            
    		writer.write("Distance:" + Double.toString(distance) + "\n");
    		writer.write("Bursts detected:" + Integer.toString(burstsIndices.length) + "\n");
    		writer.write("Impedance calculated at region from index d to d+200 samples\n");
    		writer.write("Noise starts at: " + Integer.toString(noiseStart) + "\n");    
    		writer.write("Noise cutoff is: " + Integer.toString(NOISE_CUTOFF) + "\n\n");    		
			writer.write("===================================\n");
			
    		for (int i = 0; i < impedance.length; i++) {
				writer.write("Impedance calculated:" + Double.toString(impedance[i]) + "\n");
				writer.write("Frequency calculated:" + Double.toString(frequency[i]) + "\n");
				writer.write("Burst index number:" + Double.toString(burstsIndices[i]) + "\n");
				writer.write("===================================\n");
			}    		
    		
    		writer.close();
    		MediaScannerConnection.scanFile(this, new String[] {fileLog}, null, null);     		
    		
        } catch (IOException ex) {
        	ex.printStackTrace();
        }
	}	
}
