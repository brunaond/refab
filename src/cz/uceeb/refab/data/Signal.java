package cz.uceeb.refab.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import cz.uceeb.refab.R;

public class Signal {

	public static final int RECORDER_SAMPLERATE = 44100;
	public static final int NOISE_CUTOFF = 12000;
	public static final double[] ALPHA = {2.2877,1.6509,1.7962,1.1580,0.9992,1.0110,1.0097,0.9822,0.8065,0.8297,0.8215,0.8206,0.8687,0.9528,1.0751,0.5372};
	public static final double[] COEFFA = {55.0833,12.4888,54.0666,10.4352,3.0666,3.0303,2.9293,2.7145,1.7792,2.0170,1.9585,2.0740,2.1382,2.2571,3.5222,0.1455};
	//public static final int[] samples16bursts = {372, 292, 224, 175, 141, 112, 88, 70, 56, 45, 35, 28, 23, 17, 14, 11};
	public static final double[] FREQUENCIES = {118, 151, 196, 252, 313, 394, 501, 630, 787, 980, 1260, 1575, 1917, 2594, 3150, 4009};

	private double distance;
	private short[] noise, data;
	private ArrayList<Burst> incident,reflected;
	private double[] reflectivity;
	private double[] frequency = null;
	private String material;
	private int numberOfBurstsExpected,numberOfBurstsDetected;
	private int[] startIndicesIncident, startIndicesReflected;
	private int burstDelayInSamples, burstLengthInSamples, burstDistanceInSamples;
	private int startIndexNoise, stopIndexNoise;
	private int noiseLength;
	private String filePath; //raw data to be processed from the pcm file
	private String fileWav;

	Context context = null;

	public Signal(Context c, String pathRaw, String pathWav) {
		this.filePath = pathRaw;
		this.fileWav = pathWav;
		this.context = c; 
	}

	public Signal(Context c, String pathRaw, String pathWav, int numberOfBurstsExpected) {
		this.filePath = pathRaw;
		this.fileWav = pathWav;
		this.context = c;
		this.numberOfBurstsExpected = numberOfBurstsExpected;
	}

	public void processData() {
		Long length = null;
		int lengthNoise;
		int[] indices;
		byte[] buffer = null;
		byte[] bufferNoise = null;
		short[] plotData = null;
		short[] noiseData = null;
		short[] burstData = null;
		short[] burstRegion = null;
		FileInputStream is = null;
		InputStream noiseTemplate = null;
		double[] data_corr = null;
		double[] data_corr_butter = null;
		//double[] data_sub = null;
		double time;
		//double distance;
		int maximum;
		int peak;	
		double[] data_sub;
		distance = 0;

		//		TextView mText = (TextView) v.findViewById(R.id.status_text_view);

		if (filePath==null){ 			
			//			mText.setText("No file recorded yet");
			Log.d("PLR", "No file recorded yet");
			return;		
		}
		try {
			is = new FileInputStream(filePath);		
			length = is.getChannel().size();
			buffer = new byte[length.intValue()];
			plotData = new short[length.intValue()];				
			is.read(buffer);		

			noiseTemplate = context.getResources().openRawResource(R.raw.noise);

			lengthNoise = noiseTemplate.available();

			Log.d("PLR", "Size of noise is: " + String.valueOf(lengthNoise));

			bufferNoise = new byte[lengthNoise];				
			noiseTemplate.read(bufferNoise);	

		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}		
		plotData = byte2short(buffer);
		Log.d("PLR", "plotData converted successfully.");
		noiseData = byte2short(bufferNoise);
		Log.d("PLR", "noiseData converted successfully.");
		// The noise is recognized to be between following indices: 4300 and 22100
		//noiseExtracted = simpleMath.getNoiseStart(noiseData, plotData);		
		startIndexNoise = getNoiseStartIndex(noiseData, plotData);
		noise = getSubsequent(startIndexNoise, startIndexNoise+NOISE_CUTOFF, plotData);
		Log.d("PLR", "Noise extracted successfully.");
		// Detecting the regions with bursts - there should be no more than as many bursts as in the test file 
		// burstRegion = simpleMath.getBurstRegion(noiseData, plotData);
		burstRegion = getSubsequent(startIndexNoise+NOISE_CUTOFF, plotData.length-1, plotData);
		Log.d("PLR", "Burst region extracted...");
		indices = getBurstIndices(burstRegion); // returns burst indices from the data without noise
		this.numberOfBurstsDetected = indices.length;		
		Log.d("PLR", "Burst indices obtained.");

		data_corr = new double[2*noise.length];
		data_corr = correlation_fast(noise);
		Log.d("PLR", "Correlation computed.");
		//drawPlot(data_corr);
		data_corr = abs(data_corr);		
		Log.d("PLR", "Abs computed.");
		data_corr_butter = butterworth(data_corr);
		Log.d("PLR", "Filter applied.");
		//drawPlot(data_corr_butter);
		//Log.d("PLR", "Data plotted.");

		peak = localMax(data_corr_butter);
		// TODO Fix the numbers determining where to search for peak
		data_sub = getSubsequent(peak+50, peak+140, data_corr_butter);		
		maximum = localMax(data_sub);
		time = (double)(maximum+50)/RECORDER_SAMPLERATE;
		this.distance = (time*340/2)*100;

		//		TextView bursts = (TextView) v.findViewById(R.id.busrsts_detected_text_view);
		//		bursts.setText("#Bursts: "+ indices.length);
		this.reflectivity = new double[numberOfBurstsDetected];
		this.frequency = new double[numberOfBurstsDetected];
		int i = 0;
		int k = 0;
		if (indices.length == 15) {
			k = 1;
		}

		for (int d : indices) { 			
			// It is expected that the burst length is 4ms resulting in 200 samples. Allowing twice the size for safe detection.
			burstData = getSubsequent(d, d+400, burstRegion);

			// drawPlotClean(burstData);


			this.reflectivity[i] = calcImpedance(burstData, k, 0.15, this.distance, 0.0 );
			Log.d("PLR", "Impedance calculated");
			this.frequency[i] = calcFrequency(getSubsequent(d, d+197, burstRegion));
			frequency[i] = Signal.FREQUENCIES[k];
			Log.d("PLR", "Frequency calculated");
			i++;
			k++;
		}

		writeLog(indices);
	}


	private void writeLog(int[] burstsIndices){
		String fileLog = null;
		//fileLog = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
		if (this.fileWav == null){
			return;
		}
		fileLog = this.fileWav;
		fileLog += ".log";
		FileOutputStream os;
		PrintWriter writer;


		try {            
			os = new FileOutputStream(fileLog);
			writer = new PrintWriter(os);            
			writer.write("Distance:" + Double.toString(this.distance) + "\n");
			writer.write("Bursts detected:" + Integer.toString(burstsIndices.length) + "\n");
			writer.write("Impedance calculated at region from index d to d+200 samples\n");
			writer.write("Noise starts at: " + Integer.toString(startIndexNoise) + "\n");    
			writer.write("Noise cutoff is: " + Integer.toString(NOISE_CUTOFF) + "\n\n");    		
			writer.write("===================================\n");

			for (int i = 0; i < reflectivity.length; i++) {
				writer.write("Impedance calculated:" + Double.toString(reflectivity[i]) + "\n");
				writer.write("Frequency calculated:" + Double.toString(frequency[i]) + "\n");
				writer.write("Burst index number:" + Double.toString(burstsIndices[i]) + "\n");
				writer.write("===================================\n");
			}    		

			writer.close();
			MediaScannerConnection.scanFile(this.context, new String[] {fileLog}, null, null);


		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private short[] byte2short(byte[] bData) {
		short[] sData;
		short sDataTemp;
		int bDataLength = bData.length;
		int sDataLength;

		if (bDataLength%2>0) sDataLength=bDataLength/2+1;
		else sDataLength = bDataLength/2;

		sData = new short[sDataLength];

		for(int i=0;i<sDataLength;i++) {
			sData[i] = 0x0000;
			sDataTemp = 0x0000;
			sData[i] = bData[i*2+1];
			sData[i] = (short) (sData[i]<<8);
			sDataTemp = bData[i*2];
			sData[i] = (short) (sData[i]|sDataTemp);			
		}
		return sData;
	}

	private double getEnergy(short[] dataIn){	
		double energy = 0;		
		for (short d:dataIn){
			energy += Math.pow(d, 2);			
		}

		energy/=dataIn.length; // Normalizing energy per one sample
		//energy = Math.sqrt(energy); 
		return energy;
	}	

	private double[] getSubsequent(int start, int end, double[] data) {
		double[] output;
		output = new double[end-start+1];
		for (int i = start; i < end+1; i++) {
			output[i-start] = data[i];				
		}
		return output;
	}
	
	private short[] getSubsequent(int start, int end, short[] data) {
		short[] output;
		output = new short[end-start+1];
		for (int i = start; i < end+1; i++) {
			output[i-start] = data[i];				
		}
		return output;
	}

	private double[] butterworth(double[] data) {
		int order;
		int length;
		double[] y;
		double[] x;
		double[] b =  {0.0013,    0.0051,    0.0076,    0.0051,    0.0013};
		double[] a =  {1.0000,   -2.8869,    3.2397,   -1.6565,    0.3240};			

		order = a.length;		
		length = data.length;

		x = new double[length];
		y = new double[length];
		x = data;

		if (order!= 5) return null;

		for(int i = 0; i<length; i++){
			y[i] = 0;
		}

		// from MATLAB a =  {1.0000,   -2.8869,    3.2397,   -1.6565,    0.3240};
		// from MATLAB b =  {0.0013,    0.0051,    0.0076,    0.0051,    0.0013};
		for(int i = order; i<length; i++){
			y[i] = b[0]*x[i] + b[1]*x[i-1] + b[2]*x[i-2] + b[3]*x[i-3] + b[4]*x[i-4]
					- a[1]*y[i-1] - a[2]*y[i-2] - a[3]*y[i-3] - a[4]*y[i-4];
		}

		return y;
	}	

	private double[] abs(double[] x){
		for(int i = 0; i<x.length; i++){
			x[i] = Math.sqrt(x[i]*x[i]); 
		}
		return x;
	}

	private int[] diff(int[] x){
		int[] x_diff;
		x_diff = new int[x.length-1];
		for(int i = 1;i<x.length; i++){
			x_diff[i-1] = x[i] - x[i-1];
		}
		return x_diff;
	}

	private static int localMax(double[] data){
		double max;
		int index;

		max = data[0];
		index = 0;
		for (int i = 0; i < data.length; i++) {
			if (max<data[i]) {
				max = data[i];
				index = i;
			} 
		}
		return index;
	}

	private int localMax(int[] data){
		int max;
		int index;

		max = data[0];
		index = 0;
		for (int i = 0; i < data.length; i++) {
			if (max<data[i]) {
				max = data[i];
				index = i;
			} 
		}
		return index;
	}


	/**Computes only half of correlation, we suppose that correlation is symmetric around origin*/
	private double[] correlation_fast(short[] data_in) {
		int length;
		length = data_in.length;
		double[] data = null;
		double[] corr = null;		  	
		double R = 0;

		data = new double[3*length-2];
		corr = new double[2*length];

		for(int i = 0; i<3*length-2;i++){
			if(i<length||i>2*length-1){
				data[i] = 0;
			} else {
				data[i] = data_in[i-length];
			}
		}

		for (int i = 0; i<length; i++){
			for(int j = 0; j<=i;j++){		      
				R += data[length+j]*data_in[length-1-i+j];
			}
			corr[i] = R;
			corr[length-2-i+length] = R;
			R = 0;
		}						
		return corr;
	}

	// This is not full x-correlation.
	private static int getNoiseStartIndex(short[] inTemplate, short[] inData ) {		
		double[] corr = null;
		int index;


		corr = new double[inData.length];

		for (int i = 0; i < corr.length-inTemplate.length; i++) {			
			for (int j = 0; j < inTemplate.length; j++) {
				corr[i] += inData[i+j] * inTemplate[j];				
			}
		}

		index = localMax(corr);
		return index;

	}

	private double calcImpedance(short[] inBurst, int burstNumber, double D_mic_loudspeaker, double D_mic_table, double Thickness_sample){
		double impedance;
		double energyIncident;
		double energyIncidentCalculated;
		double energyReflected;
		double noise = 0.0;
		energyIncident = getEnergy(getSubsequent(90, 190, inBurst));
		energyReflected = getEnergy(getSubsequent(300, 400, inBurst));
		//energyIncidentCalculated = (energyIncident*4*PI*(Math.pow(D_mic_loudspeaker,2)))/(4*PI*Math.pow((D_mic_loudspeaker + 2*(D_mic_table-Thickness_sample)),2));
		energyIncidentCalculated = (COEFFA[burstNumber]*energyIncident)/(Math.pow(D_mic_table, ALPHA[burstNumber]))+noise;
		impedance = energyIncidentCalculated/energyReflected;
		return impedance;
	}	

	private int[] getBurstIndices(short[] inData) {
		int indices[] = new int[20]; // It is expected, that there will not be more than 20 bursts.
		int temp[];
		int k =0;
		double threshold;

		threshold = getStd(inData);

		for (int i = 0; i < inData.length; i++) {

			if (inData[i] > 2*threshold) {
				indices[k] = i; //100 samples are ~2ms with 44100 sampling rate.
				k++;
				if (k==20){
					Log.d("PLR", "Indices overflow");
					break;
				}

				i += 3000; // jump over the burst, continues search for other bursts.
			}
		}

		// Ensures that number of indices in the matrix is equal to number of bursts.
		temp = new int[k];
		for (int i = 0; i < k; i++) {
			temp[i] = indices[i];
		}
		return temp;		
	}

	private double calcFrequency(short[] inBurst){
		double freq;
		int[] indices = new int[inBurst.length];
		int k =0;
		int max;
		int[] temp, tempDiff;


		/* 		 
		 * Check for crossing zero and if it does cross zero, record the index to a new field.
		 */

		for (int i = 1; i < inBurst.length; i++) {
			if ((inBurst[i] > 0) && (inBurst[i-1]) < 0) {
				indices[k] = i;
				k++;
			} else if ((inBurst[i] < 0) && (inBurst[i-1] > 0)) {
				indices[k] = i;
				k++;
			}
		}

		temp = new int[k];
		tempDiff = new int[k];
		for (int i = 0; i < k; i++) {
			temp[i] = indices[i];
		}

		Log.d("PLR", "Freq - k: " + Integer.toString(k));

		if (k > 500) {
			tempDiff = diff(temp); // calculates the difference between all points in the array
			freq = RECORDER_SAMPLERATE/getMean(tempDiff);
			// max = localMax(tempDiff); // finds the greatest difference in the array which is respective to the greatest frequency
			// min = localMin(tempDiff); // finds the smallest difference in the array
			// freq = SAMPLING_FREQ/tempDiff[max]; // calculating the frequency based on the number of samples			
		} else if(k > 1 && k < 500) {
			tempDiff = diff(temp); // calculates the difference between all points in the array
			max = localMax(tempDiff); // finds the greatest difference in the array which is respective to the greatest frequency
			freq = RECORDER_SAMPLERATE/tempDiff[max]; // calculating the frequency based on the number of samples			
		} else {
			// in case only one crossing is detected the most likely frequency is returned
			freq = RECORDER_SAMPLERATE/200;
		}

		return freq;
	}	

	private double getMean(short[] inData) {
		double mean = 0;
		for (short s : inData) {
			mean += s;
		}
		mean /= inData.length;
		return mean;
	}

	private static double getMean(int[] inData) {
		double mean = 0;
		for (int s : inData) {
			mean += s;
		}
		mean /= inData.length;
		return mean;		
	}

	/** Calculates the standard deviation of the data.*/
	private double getStd(short[] inData) {
		double std = 0;
		double mean = 0;
		mean = getMean(inData);

		for (short s : inData) {
			std += ((s-mean)*(s-mean));
		}		
		std /= (inData.length-1);
		std = Math.sqrt(std);
		return std;
	}	
	
	public double getDistance(){
		return this.distance;
	}

	public double getNumberOfBurstsDetected(){
		return this.numberOfBurstsDetected;
	}
}
