package cz.uceeb.refab;

import java.io.InputStream;
import java.util.ArrayList;

public class Signal {
	
	private double distance;
	private double[] noise, data;
	private ArrayList<Burst> incident,reflected;
	private double[] reflectivity;
	private String material;
	private int numberOfBurstsDetected;
	private int[] startIndicesIncident, startIndicesReflected;
	private int burstDelayInSamples, burstLengthInSamples, burstDistanceInSamples;
	private int startIndexNoise, stopIndexNoise;
	private int noiseLength;
	
	public Signal(String path) {
	
	}
	
	public Signal(InputStream data) {
		
	}
	
	public Signal(String pathData, String pathNoise) {

	}
	
	public Signal(InputStream data, InputStream noise) {

	}

	
}
