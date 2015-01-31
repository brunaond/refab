package cz.uceeb.refab.data;

import java.math.BigDecimal;
import java.util.List;

public class BurstData {
	private static final int DELAY_NOISE_TO_BURSTS = 5000; // in number of samples
	private static final int BURST_LENGT = 200;
	private static final int GAP_BETWEEN_BURSTS = 2000;
	
	private List<Burst> incident,reflected;
	private int numberOfBurstsExpected,numberOfBurstsDetected;
	private int[] startIndicesIncident, startIndicesReflected;
	private List<BigDecimal> reflectivity;
	
	public BurstData(short[] inData, int numberOfBurstsExpected, double distance) {
		
	}
	
	public void calculateReflectivity(){
		Burst reflectedBurst;
		this.reflectivity.clear();
		
		for (Burst incidentBurst : incident) {
			reflectedBurst = this.reflected.get(incidentBurst.getOrderNumber());
			this.reflectivity.add(
					new BigDecimal(
							reflectedBurst.getEnergyAtOrigin()/
							incidentBurst.getEnergyInDistance()));			
		}
	}
	
	/**Finds bursts based on prior information about the sample signal setup.
	 * Using the local variable describing the signal produce the bursts are looked
	 * for on known location in the signal and extracted without any intelligent
	 * detection efforts.*/
	private void extractBursts(){
		
	}
	
	/**Looks for bursts within the data and creates bursts based on found locations
	 * in the data. Unlike the method extractBursts this method is attempting an
	 * automatic burst detection, which may or may not be always successful.*/
	private void findBursts(){
		
	}
	
	/**Method looks for bursts within given signal and tries automatically assess 
	 * which part of the signal is most likely a burst and which is just a nose.
	 * Returns indices of start of detected bursts. Length of returned field can be
	 * saved in field numberOfBurstsDetected and compared with numberOfBurstsExpected.
	 * If they match, it suggests that the detector was successful, but it should not 
	 * be taken as granted. Especially on lower frequencies the detection method used
	 * can fail.*/
	private void findBurstIndices(){
		
	}
}
