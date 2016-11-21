
package com.ibm;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.hyperledger.fabric.sdk.shim.ChaincodeBase;
import org.hyperledger.fabric.sdk.shim.ChaincodeStub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <h1>Chaincode using external weather API to trigger cooling degrees days
 * contract</h1>
 * 
 * @author Benjamin Fuentes (bfuentes@fr.ibm.com)
 *
 */
public class JavaCDD extends ChaincodeBase {

	private static Log log = LogFactory.getLog(JavaCDD.class);

	@Override
	/**
	 * Entry point of invocation interaction
	 * 
	 * @param stub
	 * @param function
	 * @param args
	 */
	public String run(ChaincodeStub stub, String function, String[] args) {
		
		log.info("Calling invocation chaincode with function :" + function + " and args :"
				+ org.apache.commons.lang3.StringUtils.join(args, ","));
		
		log.info("Coming from CallerCertificate :" + stub.getCallerCertificate());

		switch (function) {
		case "init":
			init(stub, function, args);
			break;
		case "executeContract":
			String re = executeContract(stub, args);
			log.info("Return of executeContract : " + re);
			return re;
		default:
			String warnMessage = "{\"Error\":\"Error function " + function + " not found\"}";
			log.warn(warnMessage);
			return warnMessage;
		}

		return null;
	}

	/**
	 * This function calls Weather API to check if the temperature on a location
	 * is inferior to the contract's threshold. If yes, the client is redeemed
	 * for the value agreed on the contract
	 * 
	 * @param stub
	 * @param args
	 *            client name, postal Code, country Code
	 * @return true if contract has been executed, false otherwise
	 */
	public String executeContract(ChaincodeStub stub, String[] args) {

		Boolean contractExecuted = false;

		if (args.length != 3) {
			String errorMessage = "{\"Error\":\"Incorrect number of arguments. Expecting 3: client name, postal Code, country Code\"}";
			log.error(errorMessage);
			return errorMessage;
		}
		ObjectMapper mapper = new ObjectMapper();
		ContractRecord contractRecord;
		try {
			contractRecord = mapper.readValue(stub.getState(args[0]), ContractRecord.class);
		} catch (Exception e1) {

			String errorMessage = "{\"Error\":\" Problem retrieving state of client contract : " + e1.getMessage()
					+ "  \"}";
			log.error(errorMessage);
			return errorMessage;
		}

		String postalCode = args[1];
		String countryCode = args[2];

		// weather service
		String url = "https://twcservice.mybluemix.net/api/weather/v1/location/" + postalCode + "%3A4%3A" + countryCode
				+ "/observations.json?language=en-GB";

		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);

		SSLSocketFactory sf;
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, null, null);
			sf = new SSLSocketFactory(sslContext);
		} catch (Exception e1) {
			String errorMessage = "{\"Error\":\" Problem with SSLSocketFactory : " + e1.getMessage() + "  \"}";
			log.error(errorMessage);
			return errorMessage;
		}

		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Scheme sch = new Scheme("https", sf, 443);
		httpclient.getConnectionManager().getSchemeRegistry().register(sch);

		((AbstractHttpClient) httpclient).getCredentialsProvider().setCredentials(
				new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
				new UsernamePasswordCredentials("dfa7551a-2613-4f5c-bff7-339649770aa5", "gvbmK5JsGO"));

		HttpResponse response;
		try {
			response = httpclient.execute(httpget);

			log.info("Called Weather service");

			int statusCode = response.getStatusLine().getStatusCode();

			HttpEntity httpEntity = response.getEntity();
			String responseString = EntityUtils.toString(httpEntity);

			if (statusCode == HttpStatus.SC_OK) {

				log.info("Weather service call OK");

				WeatherObservationResponse weatherObservationResponse = mapper.readValue(responseString,
						WeatherObservationResponse.class);

				if (weatherObservationResponse.getObservation().getTemp() < contractRecord.temperatureThreshold) {
					// then please redeem the client
					contractRecord.totalAmountReceived += contractRecord.amountReceivedWhenContractIsActivated;
					stub.putState(contractRecord.clientName, contractRecord.toString());
					log.info("Contract condition valid " + weatherObservationResponse.getObservation().getTemp() + " < "
							+ contractRecord.temperatureThreshold);
					contractExecuted = true;
				} else {
					log.info("Contract condition invalid " + weatherObservationResponse.getObservation().getTemp()
							+ " > " + contractRecord.temperatureThreshold);
				}

			} else {
				String errorMessage = "{\"Error\":\"Problem while calling Weather API : " + statusCode + " : "
						+ responseString + "\"}";
				log.error(errorMessage);
				return errorMessage;
			}

		} catch (Exception e) {
			String errorMessage = "{\"Error\":\"Problem while calling Weather API : " + e.getMessage() + "\"}";
			log.error(errorMessage);
			try {
				log.error(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(e.getStackTrace()));
			} catch (JsonProcessingException e2) {
				e2.printStackTrace();
			}
			return errorMessage;
		}

		return contractExecuted.toString();
	}

	/**
	 * This function initializes the contract
	 * 
	 * @param stub
	 * @param function
	 * @param args
	 *            client name, temperature threshold, amount received when
	 *            contract is activated
	 * @return
	 */
	public String init(ChaincodeStub stub, String function, String[] args) {
		
		if (args.length != 3) {
			return "{\"Error\":\"Incorrect number of arguments. Expecting 3 : client name, temperature threshold, amount received when contract is activated \"}";
		}
		try {
			ContractRecord contractRecord = new ContractRecord(args[0], Integer.parseInt(args[1]),
					Integer.parseInt(args[2]));
			stub.putState(args[0], contractRecord.toString());
		} catch (NumberFormatException e) {
			return "{\"Error\":\"Expecting integer value for temperature threshold and amount received\"}";
		}
		return null;
	}

	/**
	 * This function can query the current State of the contract
	 * 
	 * @param stub
	 * @param function
	 * @param args
	 *            client name
	 * @return total amount received for this client
	 */
	@Override
	public String query(ChaincodeStub stub, String function, String[] args) {
		
		log.info("Calling query chaincode with function :" + function + " and args :"
				+ org.apache.commons.lang3.StringUtils.join(args, ","));
		
		log.info("Coming from CallerCertificate :" + stub.getCallerCertificate());

		
		if (args.length != 1) {
			return "{\"Error\":\"Incorrect number of arguments. Expecting name of the client to query\"}";
		}		
		
		String clientName = stub.getState(args[0]);

		if (clientName != null && !clientName.isEmpty()) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				ContractRecord contractRecord = mapper.readValue(clientName, ContractRecord.class);
				return "" + contractRecord.totalAmountReceived;
			} catch (Exception e) {
				return "{\"Error\":\"Failed to parse state for client " + args[0] + " : " + e.getMessage() + "\"}";
			}
		} else {
			return "{\"Error\":\"Failed to get state for client " + args[0] + "\"}";
		}

	}

	@Override
	/**
	 * Just a easiest way to retrieve a contract by its name
	 */
	public String getChaincodeID() {
		return "JavaCDD";
	}

	public static void main(String[] args) throws Exception {
		new JavaCDD().start(args);
	}

}
