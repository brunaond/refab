package cz.uceeb.refab.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;
import android.view.inputmethod.ExtractedTextRequest;

public class BurstData {
	public static final byte DETECT_BURSTS = 0;
	public static final byte ASSEMBLE_BURST = 1;
	
	private static final int DELAY_NOISE_TO_BURSTS = 7270; // in number of samples
	private static final int BURST_LENGTH = 176; // 200 samples reflects length of burst region of 4.5ms
	private static final int GAP_BETWEEN_BURSTS = 4240;
	
	private ArrayList<Burst> incident,reflected;
	private int numberOfBurstsExpected,numberOfBurstsDetected;
	private int[] startIndicesIncident, startIndicesReflected;
	private ArrayList<BigDecimal> reflectivity, reflectivityNormalized;
	private double distance; 
	
	public BurstData(short[] inData, int numberOfBurstsExpected, double distance, byte modeOfDetection) {
		this.numberOfBurstsExpected = numberOfBurstsExpected;
		this.distance = distance;
//		this.startIndicesIncident = findStartIndicesIncident(inData);
		this.startIndicesIncident = assembleStartIndicesIncident();
		this.numberOfBurstsDetected = this.startIndicesIncident.length;
		extractBursts(inData, this.startIndicesIncident);
		calculateReflectivity();
		normalizeReflectivity();
	}
	
	/**
	 * Calculates the reflectivity based on detected bursts. The burst need to be detected
	 * correctly to provide a reasonable output. The reflectivity should span from 0.0 to 1.0.
	 * Reflectivity 1.0 means that all the energy was reflected - it should be a very stiff material.*/
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
	 * Scales reflectivity down to scope of amplitude from 0 to 1. 
	 */
	private void normalizeReflectivity(){
		BigDecimal maximum = new BigDecimal(0.0d);
		this.reflectivityNormalized = new ArrayList<BigDecimal>();
		Iterator<BigDecimal> iterator = this.reflectivity.iterator();
		while (iterator.hasNext()) {
			BigDecimal val = (BigDecimal) iterator.next();
			if (val.compareTo(maximum)==1) {
				maximum = val;
			}
		}		
		iterator = this.reflectivity.iterator();
		while (iterator.hasNext()) {
			if (maximum.compareTo(new BigDecimal(0.0d))==0) {
				iterator.next();
			} else {
				this.reflectivityNormalized.add(iterator.next().divide(maximum, BigDecimal.ROUND_UP));
			}
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
	private int[] assembleStartIndicesIncident(){
		int[] temp = new int[this.numberOfBurstsExpected];
		
		temp[0] = BurstData.DELAY_NOISE_TO_BURSTS; 
		for (int i =1; i < temp.length; i++) {
			temp[i] = temp[i-1] + 2*BURST_LENGTH+GAP_BETWEEN_BURSTS;
		}
		return temp;
	}
	
	/**
	 * Saves all burst into the folder. Bursts are named based on their index or order number.
	 * 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public void saveBursts(String folderPath) throws FileNotFoundException, IOException {				
		if (this.incident != null) {			
			StringBuilder sb = new StringBuilder(folderPath);
			
			int pos = sb.indexOf("rec");			
			sb.replace(pos, folderPath.length(), "incident");		

			
			for (Burst b : this.incident) {					
				b.saveBurst(sb.toString());
			}
			
			pos = sb.indexOf("incident");			
			sb.replace(pos, sb.length(), "reflected");			
			
			for (Burst b : this.reflected) {
				b.saveBurst(sb.toString());
			}					
		}
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
	
	/**
	 * @return the reflectivityNormalized
	 */	
	public ArrayList<BigDecimal> getReflectivityNormalized(){
		Log.d("PLR", this.reflectivityNormalized.toString());
		return this.reflectivityNormalized;
	}	
	
	/**
	 * Browse all bursts to collect their frequencies, intended for plotting purposes.
	 * @return freq
	 * */
	public double[] getFrequencies(){
		double[] freq = new double[this.numberOfBurstsDetected];
		int i = 0;
		for (Burst b : this.incident) {
			freq[i] = b.getFrequency();
			i++;
		}
		return freq;
	}
	
	public int[] getBurstIndicesIncident(){
		return this.startIndicesIncident;
	}
}
