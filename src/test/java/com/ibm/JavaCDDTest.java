package com.ibm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hyperledger.fabric.sdk.shim.ChaincodeStub;

import junit.framework.TestCase;

public class JavaCDDTest extends TestCase {

	public void testNice() { // never goes under 0 normally, it will not be executed

		// mocks
		ChaincodeStub stub = mock(ChaincodeStub.class);
		when(stub.getState(any())).thenReturn(
				"{ \"clientName\" : \"farmer\",  \"temperatureThreshold\" : 0,  \"amountReceivedWhenContractIsActivated\" : 42, \"totalAmountReceived\" : 0}");

		JavaCDD javaCDD = new JavaCDD();

		String[] args = new String[] { "farmer", "43.7102", "7.2620" };
		String result = javaCDD.executeContract(stub, args);
		
		assertFalse(Boolean.parseBoolean(result));

	}

	public void testAlaska() { // never goes over 40 normally, it will be executed
		
		// mocks
		ChaincodeStub stub = mock(ChaincodeStub.class);
		when(stub.getState(any())).thenReturn(
				"{ \"clientName\" : \"farmer\",  \"temperatureThreshold\" : 40,  \"amountReceivedWhenContractIsActivated\" : 42, \"totalAmountReceived\" : 0}");

		JavaCDD javaCDD = new JavaCDD();

		String[] args = new String[] { "farmer", "64.8378", "-147.7164" };
		String result = javaCDD.executeContract(stub, args);
		
		assertTrue(Boolean.parseBoolean(result));


	}

}
