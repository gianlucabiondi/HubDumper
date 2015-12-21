package com.ultron.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.ultron.general.Common;

/**************************
 **************** 
 * HubSimulatorThread - manage a connection
 ****************
 **************************
 */
class MyProperties {

	public String 	INPUT_DUMP_FILE;
	//public boolean 	USE_MAGNETOMETER;
	public int 		SERVER_PORT;
	public String 	LOG_LEVEL;
	public String 	LOG_FILE;
	public int		SAMPLE_FREQUENCY_MS;
	public int 		SERVER_SOCKET_TIMEOUT_MS;
	

	public MyProperties( String p_propFile ) throws Exception {
		Properties prop = new Properties();
		
		// open and read property file
		try {

			prop.loadFromXML(new FileInputStream(p_propFile));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Error in opening propertis file: file not found!");
            throw e;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error in opening propertis file!");
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Unknown error in opening propertis file!");
			throw e;
		} 
		
		//
		String portNumber = prop.getProperty("SERVER_PORT");
		SERVER_PORT = Integer.parseInt(portNumber);
		//
		INPUT_DUMP_FILE = prop.getProperty("INPUT_DUMP_FILE");
		//
		//String MAGNETOMETER = prop.getProperty("MAGNETOMETER");
		//USE_MAGNETOMETER = ("*YES*Y*SI*S*".indexOf(MAGNETOMETER) > 0 ) ? true : false;
		//
		LOG_LEVEL = prop.getProperty("LOG_LEVEL");
		if (LOG_LEVEL.equals( "NONE" ))			LOG_LEVEL = "0";
		if (LOG_LEVEL.equals( "ERROR" ))		LOG_LEVEL = "1";
		if (LOG_LEVEL.equals( "WARNING" ))		LOG_LEVEL = "2";
		if (LOG_LEVEL.equals( "INFORMATION" ))	LOG_LEVEL = "3";
		if (LOG_LEVEL.equals( "DEBUG" ))		LOG_LEVEL = "4";
		if ( !LOG_LEVEL.equals("0") && !LOG_LEVEL.equals("1") && !LOG_LEVEL.equals("2") && !LOG_LEVEL.equals("3") && !LOG_LEVEL.equals("4") && 
				!LOG_LEVEL.equals("NONE") && !LOG_LEVEL.equals("ERROR") && !LOG_LEVEL.equals("WARNING") && !LOG_LEVEL.equals("INFORMATION") && !LOG_LEVEL.equals("DEBUG") ) {
			LOG_LEVEL = Integer.toString(Common.DEFAULT_LOG_LEVEL);
		}
		//
		LOG_FILE = prop.getProperty("LOG_FILE");
		//
		try { SAMPLE_FREQUENCY_MS = Integer.parseInt(prop.getProperty("SAMPLE_FREQUENCY_MS")); }
		catch (Exception e) { SAMPLE_FREQUENCY_MS = 0; }
		if (SAMPLE_FREQUENCY_MS == 0) SAMPLE_FREQUENCY_MS = Common.DEFAULT_SAMPLE_FREQUENCY_MS;
		//
		String serverSocketTimeoutMS = prop.getProperty("SERVER_SOCKET_TIMEOUT_MS");
		SERVER_SOCKET_TIMEOUT_MS = Integer.parseInt(serverSocketTimeoutMS);
		
	}

}