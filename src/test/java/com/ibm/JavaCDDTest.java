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

		String[] args = new String[] { "farmer", "06600", "FR" };
		String result = javaCDD.executeContract(stub, args);
		
		assertFalse(Boolean.parseBoolean(result));

	}

	public void testFairbanks() { // never goes over 40 normally, it will be executed

		// mocks
		ChaincodeStub stub = mock(ChaincodeStub.class);
		when(stub.getState(any())).thenReturn(
				"{ \"clientName\" : \"farmer\",  \"temperatureThreshold\" : 40,  \"amountReceivedWhenContractIsActivated\" : 42, \"totalAmountReceived\" : 0}");

		JavaCDD javaCDD = new JavaCDD();

		String[] args = new String[] { "farmer", "99701", "US" };
		String result = javaCDD.executeContract(stub, args);
		
		assertTrue(Boolean.parseBoolean(result));


	}

}
