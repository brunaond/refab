package cz.uceeb.refab.data;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.media.MediaScannerConnection;


public class Burst {
//	private static final double[] ALPHA = {2.2877,1.6509,1.7962,1.1580,0.9992,1.0110,1.0097,0.9822,0.8065,0.8297,0.8215,0.8206,0.8687,0.9528,1.0751,0.5372};
//	private  static final double[] COEFFA = {55.0833,12.4888,54.0666,10.4352,3.0666,3.0303,2.9293,2.7145,1.7792,2.0170,1.9585,2.0740,2.1382,2.2571,3.5222,0.1455};
//	private static final double[] ALPHA={ 	2.0707d,
//											2.0275d,
//											2.0350d,
//											2.0841d,
//											2.0738d,
//											2.0768d,
//											2.0199d,
//											1.9551d,
//											2.0937d,
//											1.8834d,
//											2.1319d,
//											1.9132d,
//											1.9117d,
//											1.9554d,
//											1.8588d,
//											1.9272d};
//	private  static final double[] COEFFA = {	136.6664d,
//												104.7057d,
//												116.9680d,
//												141.2588d,
//												131.9383d,
//												141.6459d,
//												109.8468d, 
//												70.3329d,
//												133.6649d, 
//												67.0906d,
//												157.0402d, 
//												80.9513d, 
//												69.1043d,
//												83.8278d, 
//												69.9718d, 
//												97.4525d};
	private static final double[] ALPHA={ 	1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d,
		1.6d};
	
private  static final double[] COEFFA = {	25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d,
		25.0d};		

private  static final double[] TABLE200_N_GREATER = {	0.0060d,
	0.0060d,
	0.0060d,
	0.0082d,
	0.0110d};

private  static final double[] TABLE200_150 = {	0.0112d,
	0.0112d,
	0.0112d,
	0.0145d,
	0.0145d};

private  static final double[] TABLE150_100 = {	0.0210d,
	0.0210d,
	0.0210d,
	0.0325d,
	0.0430d};

	//public static final int[] samples16bursts = {372, 292, 224, 175, 141, 112, 88, 70, 56, 45, 35, 28, 23, 17, 14, 11};
	private static final int[] FREQUENCIES_16BURSTS = {118, 151, 196, 252, 313, 394, 501, 630, 787, 980, 1260, 1575, 1917, 2594, 3150, 4009};
	private static final int[] FREQUENCIES_5BURSTS = {2700, 3150, 4000, 5000, 6400};
	private static final int RECORDER_SAMPLERATE = 44100;
	
	private short[] data;
	private double currentEnergyAtOrigin, calculatedEnergyInDistance;
	private double frequency;
	private int orderNumber;	
	private int[] peakMaxIndices, peakMinIndices, zeroCrossingIndices;
	
	private Context context;
	
	public Burst(Context c, short[] inData, int number, double distance) {
		this.data = inData;		
		if (number > 15) {
			number = 15;
		}
		this.orderNumber = number;
		this.frequency = FREQUENCIES_5BURSTS[number];
		this.context = c;
		this.calculateCurrentEnergyAtOrigin();
		this.calculateEnergyInDistance(distance);
		
	}
	
	private void calculateCurrentEnergyAtOrigin(){
		//TODO implement detection of periods - results in more precision
		this.currentEnergyAtOrigin = 0;		
		for (short d:data){
			this.currentEnergyAtOrigin += Math.pow(d, 2);			
		}
		this.currentEnergyAtOrigin/=data.length;
	}
	
	private void calculateEnergyInDistance(double distance){
		double noise = 0.0;
		double distanceTraveledbySound = (distance*2)/100.0d; // in meters

		// The table so far seems to provide the best resutls.
		// TODO - Do more measurements to get better resolution.
		if (distanceTraveledbySound > 2.0d) {
			this.calculatedEnergyInDistance = this.currentEnergyAtOrigin*TABLE200_N_GREATER[orderNumber];
		} else if ((distanceTraveledbySound < 2.0d)&(distanceTraveledbySound > 1.5d)) {
			this.calculatedEnergyInDistance = this.currentEnergyAtOrigin*TABLE200_150[orderNumber];
		} else if ((distanceTraveledbySound < 1.5d)&(distanceTraveledbySound > 1.0d)) {
			this.calculatedEnergyInDistance = this.currentEnergyAtOrigin*TABLE150_100[orderNumber];
		} else {
			this.calculatedEnergyInDistance = this.currentEnergyAtOrigin*1.9d*TABLE150_100[orderNumber];
		}

		// Yields false results for low frequencies.
		/*this.calculatedEnergyInDistance = 
				(COEFFA[orderNumber]*this.currentEnergyAtOrigin)/
				(Math.pow(distanceTraveledbySound, ALPHA[orderNumber]));*/		
	}
	
	/**Calculates frequency of the burst based on zero crossing detection
	 * NOTE: If possible, perform oversampling, it seems that for high frequencies this does not provide good results.
	 * For now using the table FREQUENCIES_XXSBURSTS seems better. This part would be used only if the test sample is unknown.*/
	private void calculateFrequency(){
		this.frequency =  RECORDER_SAMPLERATE/findMax(diff(this.zeroCrossingIndices));
	}
	
	private int findMax(int[] inData) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private int[] diff(int[] indices) {
		// TODO Auto-generated method stub
		return null;
	}

	private int[] findPeakMaxIndices(){
		//TODO implement this method properly
		int[] indices = null;
		return indices;
	}
	
	private int[] findPeakMinIndices(){
		//TODO implement this method properly		
		int[] indices = null;
		return indices;
	}
	
	private int[] findZeroCrossingIndices(){
		//TODO implement this method properly		
		int[] indices = null;
		return indices;		
	}
	
	
	/**
	 * Saves the burst data to a file for future reference. Mainly intended for debugging purposes.
	 * The input string should include path specification with all the folders and also the prefix
	 * for the burst since it is not possible to recognize from inside this class whether this burst
	 * is incident wave or reflected one.
	 */
	public void saveBurst(String filePath) throws FileNotFoundException, IOException{		
		FileOutputStream os;
		StringBuilder sb = new StringBuilder(filePath);
		sb.append("_burst_");
		sb.append(this.orderNumber);
		sb.append(".pcm");
		os = new FileOutputStream(sb.toString());
		os.write(short2byte(data));
		os.flush();
		os.close();
		MediaScannerConnection.scanFile(this.context, new String[] {sb.toString()}, null, null);
	}
	
	private byte[] short2byte(short[] inData) {
		short [] sData = inData;
	    int shortArrsize = sData.length;
	    byte[] bytes = new byte[shortArrsize * 2];
	    for (int i = 0; i < shortArrsize; i++) {
	        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
	        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);	        
	    }
	    return bytes;
	}
	
	public double getEnergyInDistance(){
		return this.calculatedEnergyInDistance;
	}
	
	public double getEnergyAtOrigin(){
		return this.currentEnergyAtOrigin;
	}

	public int getOrderNumber(){
		return this.orderNumber;
	}
	
	public double getFrequency(){
		return this.frequency;
	}
	
}
