package com.ibm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherObservationResponse {
	private Observation observation;
	
	public WeatherObservationResponse() {
	}

	public Observation getObservation() {
		return observation;
	}

	public void setObservation(Observation observation) {
		this.observation = observation;
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Observation {
	private int temp;
	
	public Observation() {
	}

	public int getTemp() {
		return temp;
	}

	public void setTemp(int temp) {
		this.temp = temp;
	}
}
