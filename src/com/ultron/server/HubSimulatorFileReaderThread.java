package com.ultron.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.ultron.general.Log;
import com.ultron.general.SensorData;

/**************************
 **************** 
 * HubSimulatorFileReader - read a file into an ArrayList
 ****************
 **************************
 */
class HubSimulatorFileReaderThread extends Thread {
	private ArrayList<SensorData>	frameList	= null;		// sensor data list
	private String					fileName	= null;		// file name
	private Log						log			= null; // log file

	// constructor
	public HubSimulatorFileReaderThread(int i, String p_fileName, ArrayList<SensorData> p_frameList, Log p_log) {
        super("HubSimulatorFileReader - " + Integer.toString(i));
        frameList = p_frameList;
        fileName = p_fileName;
        log = p_log;
        log.writeln(Log.INFORMATION, this.getName() + " - Created file reader", Log.ECHO );
	}

	@Override
    public void run() {
		
		FileInputStream in = null;
		
		try {
			in = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
	        log.writeln(Log.ERROR, this.getName() + ": error in opening file", Log.ECHO );
		}
		 
		//Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
	 
        // read each line of log file
		try {
			int idx = 0;
			String line = null;
			while ((line = br.readLine()) != null) {
            	
            	// and send it to the server
            	SensorData frame = new SensorData ( line, idx++ );
            	frameList.add(frame);

			}
			 
	        log.writeln(Log.INFORMATION, this.getName() + " - readed " + Integer.toString(idx), Log.ECHO );

		} catch (IOException e) {
			e.printStackTrace();
	        log.writeln(Log.ERROR, this.getName() + ": error in reading file", Log.ECHO );
		}

        // close the file
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
	        log.writeln(Log.ERROR, this.getName() + ": error in closing file", Log.ECHO );
		}		
	}
}