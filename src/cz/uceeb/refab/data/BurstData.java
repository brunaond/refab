package cz.uceeb.refab.data;

import java.math.BigDecimal;
import java.util.ArrayList;

import android.util.Log;
import android.view.inputmethod.ExtractedTextRequest;

public class BurstData {
	public static final byte DETECT_BURSTS = 0;
	public static final byte ASSEMBLE_BURST = 1;
	
	private static final int DELAY_NOISE_TO_BURSTS = 5000; // in number of samples
	private static final int BURST_LENGTH = 200; // 200 samples reflects length of burst region of 4.5ms
	private static final int GAP_BETWEEN_BURSTS = 2000;
	
	private ArrayList<Burst> incident,reflected;
	private int numberOfBurstsExpected,numberOfBurstsDetected;
	private int[] startIndicesIncident, startIndicesReflected;
	private ArrayList<BigDecimal> reflectivity;
	private double distance; 
	
	public BurstData(short[] inData, int numberOfBurstsExpected, double distance, byte modeOfDetection) {
		this.numberOfBurstsExpected = numberOfBurstsExpected;
		this.distance = distance;
		this.startIndicesIncident = findStartIndicesIncident(inData);
		this.numberOfBurstsDetected = this.startIndicesIncident.length;
		extractBursts(inData, this.startIndicesIncident);
		calculateReflectivity();
	}
	
	private void calculateReflectivity(){
		Burst reflectedBurst;
		this.reflectivity = new ArrayList<BigDecimal>();
		this.reflectivity.clear();
		
		for (Burst incidentBurst : incident) {
			reflectedBurst = this.reflected.get(incidentBurst.getOrderNumber());
			this.reflectivity.add(
					new BigDecimal(
							reflectedBurst.getEnergyAtOrigin()/
							incidentBurst.getEnergyInDistance()));			
		}
	}
	
	/**
	 * Finds bursts based on prior information about the sample signal setup.
	 * Using the local variable describing the signal produce the bursts are looked
	 * for on known location in the signal and extracted without any intelligent
	 * detection efforts.
	 * 
	 * @param burst Region
	 * @param startIndices 
	 */
	private void extractBursts(short[] burstRegion, int[] startIndices){
		// TODO Extracting the reflected burst depends on the distance from measured object. Longer = more samples.
		this.incident = new ArrayList<Burst>();
		this.reflected = new ArrayList<Burst>();
		int k=0;
		for (int index : startIndices) { 			
			// It is expected that the burst length is 4ms resulting in 200 samples. Allowing twice the size for safe detection.
			this.incident.add(new Burst(getSubsequent(index, index+BURST_LENGTH, burstRegion), k, this.distance));
			this.reflected.add(new Burst(getSubsequent(index+BURST_LENGTH, index+2*BURST_LENGTH, burstRegion), k, this.distance));
			k++;
		}		
	}
	
	
	/**
	 * Method looks for bursts within given signal and tries automatically assess 
	 * which part of the signal is most likely a burst and which is just a nose.
	 * Returns indices of start of detected bursts. Length of returned field can be
	 * saved in field numberOfBurstsDetected and compared with numberOfBurstsExpected.
	 * If they match, it suggests that the detector was successful, but it should not 
	 * be taken as granted. Especially on lower frequencies the detection method used
	 * can fail.
	 */
	private int[] findStartIndicesIncident(short[] inData){
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
		
	/**
	 * Unlike other method for setting up the indices this one does so based on hard coded 
	 * information about the signal. The indices are extracted from entered time between
	 * noise at the beginning of the signal and the first burst, time of the burst and then
	 * from gap which is between single bursts. Generally the values for noise tend to be 
	 * less than a second, the gap between noise and first burst is also in seconds,
	 * length of the burst is in milliseconds, cca 1.5ms to 5.0ms and the gap between the bursts
	 * is long enough to relax the speaker and to dissipate unwanted reflections usually about
	 * hundreds of milliseconds.
	 */
	private void assembleStartIndicesIncident(){
		
	}
	
	/**
	 * Is supposed to calculate the standard deviation of values entered to it.
	 *
	 */
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
	
	/**
	 * Is supposed to calculate the mean of given data
	 */
	private double getMean(short[] inData) {
		double mean = 0;
		for (short s : inData) {
			mean += s;
		}
		mean /= inData.length;
		return mean;
	}
	
	/**
	 * Cuts a part of data out from the input array.
	 */
	private short[] getSubsequent(int start, int end, short[] data) {
		short[] output;
		output = new short[end-start+1];
		for (int i = start; i < end+1; i++) {
			output[i-start] = data[i];				
		}
		return output;
	}

	/**
	 * @return the numberOfBurstsDetected
	 */
	public int getNumberOfBurstsDetected() {
		return numberOfBurstsDetected;
	}

	/**
	 * @return the reflectivity
	 */	
	public ArrayList<BigDecimal> getReflectivity(){
		return this.reflectivity;
	}

}
