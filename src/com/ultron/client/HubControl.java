/**
 * Main Class for connecting to the Hub, reading its information and writing into a log file
 * The main method receive:
 * 
 * The main method of the class:
 * - creates a queue
 * - connects a socket to the hub
 * - creates a reader (HubReader) passing to it a DataInputStream and a queue
 * - creates a writer () passing to it a DataOutputStream and a queue
 * 
 * Other methods allows sending commands to the hub:
 * - resetting RTS of satellites
 * - activating satellites
 * - letting satellites start sending frames
 * - ... 
 */
package com.ultron.client;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ultron.general.Common;
import com.ultron.general.CommandData;
import com.ultron.general.Log;

public class HubControl {

	/*
	 * local variables
	 */
	private Socket					hubSocket		= null; // socket to the hub
	private DataInputStream			inStream		= null;	// input stream coming from the hub
	private OutputStream			outStream		= null;	// output stream coming from the hub
	private MyProperties			prop			= null; // command line properties
	private HubReader 				readerThread	= null; // reader object (only one)
	private ArrayList<HubWriter>	writerThread	= new ArrayList<HubWriter>();	// writer objects (one or more)
	private Log						log				= null; // application log
	private boolean					useMagnetometer = Common.DEFAULT_USE_MAGNETOMETER;
	private HubControlState			currentState	= new HubControlState();
	
	/*
	 * Constructor:
	 */
	public HubControl ( String p_propFile ) throws Exception {

		if (! this.init(p_propFile) ) {
			throw new Exception( "Error in instantiate HubControl class" );
		};
		
	}

	/*
	 * Methods:
	 * - initialize the class with parameters (in xml format)
	 * - close the class
	 * - connect to the hub
	 * - stop recording
	 * - send command to the hub
	 */
	private boolean init ( String p_propFile ) throws Exception {
		
		prop = new MyProperties( p_propFile );
//		prop = new MyProperties( );
//		if (! prop.openAlePropertyFile(p_propFile) ) 
//			return false;
		
		// open the application log file
		log = new Log( prop.LOG_LEVEL, prop.LOG_FILE );
		log.writeln( Log.NONE , "\n-----------------------------------------------------------" );
		log.writeln( Log.NONE , "Start application " + (new Timestamp(System.currentTimeMillis())).toString() + "\n" );
				
		// read main properties
		useMagnetometer = (prop.MAGNETOMETER.toUpperCase().equals("YES") ) ? true : false;
		
		log.writeln(Log.INFORMATION, "DUMP_FILE: " + prop.DUMP_FILE);
		log.writeln(Log.INFORMATION, "HUB_IP: " + prop.HUB_IP);
		log.writeln(Log.INFORMATION, "HUB_PORT: " + prop.HUB_PORT);
		log.writeln(Log.INFORMATION, "LOG_FILE: " + prop.LOG_FILE);
		log.writeln(Log.INFORMATION, "LOG_LEVEL: " + prop.LOG_LEVEL);
		log.writeln(Log.INFORMATION, "MAGNETOMETER: " + prop.MAGNETOMETER);
		log.writeln(Log.INFORMATION, "SATELLITES_LIST_ARRAY: " + Arrays.toString(prop.SATELLITES_LIST_ARRAY));

		return true;
	}
	
	private boolean close () {

		// wait for active threads to terinate
		if (readerThread != null) 
			if (readerThread.isAlive())
				try {
					readerThread.join(10);
				} catch (InterruptedException e) {
		        	log.writeln(Log.ERROR, "===== ERROR: I waited for the Reader Thread to die but it dosn't want to");
				}

		// wait for still active threads
		for (int i = 0; i < writerThread.size(); i++) {
			if (writerThread.get(i).isAlive())
				try {
					writerThread.get(i).join(10);
				} catch (InterruptedException e) {
		        	log.writeln(Log.ERROR, "===== ERROR: I waited for the Writer Thread " + i + " to die but it dosn't want to");
				}
		}
		
		// close log file
		log.writeln( Log.NONE, "\nClose application " + (new Timestamp(System.currentTimeMillis())).toString() + "\n");
		log.close();

		return true;
	}

	public boolean connect () {
		
        String	hostName	= prop.HUB_IP;
        int 	portNumber	= Integer.parseInt(prop.HUB_PORT);

        // check state matrix
        if (! currentState.changeState(1)) {
        	log.writeln(Log.WARNING, "Illegal command: from " + currentState.getCurrentState() + " to 1 - connect", Log.ECHO);
        	return false;
        }
        
        // if socket is already open then return immediately
        if (hubSocket != null) { 
        	return true;
        }
        
        // create a new socket
        try {
        	
        	log.writeln(Log.INFORMATION, "Trying to connect to " + hostName + ":" + portNumber);
        	
        	hubSocket = new Socket( );

        	hubSocket.setSoTimeout( Common.SOCKET_TIMEOUT_MS );
        	log.writeln(Log.INFORMATION, "Set SO_TIMEOUT to " + Common.SOCKET_TIMEOUT_MS + "ms");
        	
        	hubSocket.connect( new InetSocketAddress(hostName, portNumber) );
        	log.writeln(Log.INFORMATION, "Connected succefsully to " + hostName + ":" + portNumber);
        	
        	inStream  = new DataInputStream( hubSocket.getInputStream());
        	outStream = hubSocket.getOutputStream();

        } catch (UnknownHostException e) {
			e.printStackTrace();
        	log.writeln(Log.ERROR, "===== ERROR: Host ? " + hostName, Log.ECHO);
			e.printStackTrace( log.getOutputStream() );
            return false;
        } catch (IOException e) {
        	e.printStackTrace();
        	log.writeln(Log.ERROR, "===== ERROR: I/O error in opening socket", Log.ECHO);
			e.printStackTrace( log.getOutputStream() );
            return false;
        } 
        
		return true;
	}
	
	public boolean disconnect () {
 
		// check state matrix
        if (! currentState.changeState(6)) {
        	log.writeln(Log.WARNING, "Illegal command: from " + currentState.getCurrentState() + " to 6 - Disconnect", Log.ECHO);
        	return false;
        }
        
		try {
			
			if (hubSocket != null) {
				hubSocket.close();
				hubSocket = null;
	        	log.writeln(Log.INFORMATION, "Disconnected from hub");
			}

		} catch (IOException e) {
			e.printStackTrace();
        	log.writeln(Log.ERROR, "===== ERROR: I/O error in closing socket", Log.ECHO);
			e.printStackTrace( log.getOutputStream() );
            return false;
        }

		return true;
	}

	public boolean initSensors ( boolean b_set_rtc, boolean b_set_satellites ) {
		 
		// check state matrix
        if (! currentState.changeState(2)) {
        	log.writeln(Log.WARNING, "Illegal command: from " + currentState.getCurrentState() + " to 2 - Init Sensors", Log.ECHO);
        	return false;
        }
	
		// NOT_ACTIVE & SENDING
		// (eventually SET_RTC & SET_SATELLITES)
		CommandData	command = new CommandData (
				outStream,
				CommandData.WIFI_NOT_ACTIVE | 
				CommandData.WIFI_SEND |
				( b_set_rtc ? CommandData.WIFI_SET_RTC : CommandData.WIFI_VOID ) |
				( b_set_satellites ? CommandData.WIFI_SET_SATELLITES : CommandData.WIFI_VOID ),
				prop.SATELLITES_LIST_ARRAY,
				log );
		
		return command.send();
	}
	
	public boolean startDumping () {

		ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue<byte[]>();	// queue where to put readed frames

		// check state matrix
        if (! currentState.changeState(3)) {
        	log.writeln(Log.WARNING, "Illegal command: from " + currentState.getCurrentState() + " to 3 - Start Dumping", Log.ECHO);
        	return false;
        }

		// prepare reading and writing thread
		// reader
		readerThread = new HubReader( inStream, queue, log, "1", useMagnetometer );
		// writer
		HubWriter hubWriter = null;
		try {
			hubWriter = new HubWriter( prop.DUMP_FILE, queue, log, String.valueOf(writerThread.size()+1), useMagnetometer, prop.SATELLITES_LIST_ARRAY.length );
		} catch (FileNotFoundException e) {
			readerThread = null;
			return false;
		}
		writerThread.add(0, hubWriter);

		// ACTIVE & SENDING
		CommandData	command = new CommandData (	
				outStream,
				CommandData.WIFI_ACTIVE | 
				CommandData.WIFI_SEND,
				prop.SATELLITES_LIST_ARRAY,
				log );
		
		if ( ! command.send() ) {
			readerThread = null;
			return false;
		};
		
		// start new threads to read from the hub & write to the file
		readerThread.start();
		writerThread.get(0).start();
		
		return true;
		
	}
	
	public boolean stopDumping () {

		// check state matrix
        if (! currentState.changeState(5)) {
        	log.writeln(Log.WARNING, "Illegal command: from " + currentState.getCurrentState() + " to 5 - Stop Dumping", Log.ECHO);
        	return false;
        }

		// NOT ACTIVE & SENDING
		CommandData	command = new CommandData (	
				outStream,
				CommandData.WIFI_NOT_ACTIVE | 
				CommandData.WIFI_SEND,
				prop.SATELLITES_LIST_ARRAY,
				log );
		
		if ( ! command.send() ) {
			return false;
		};

		// stop the reading thread 
		readerThread.stopReading();
		readerThread	= null;

		// notify the writer that the reader has stopped
		writerThread.get(0).notifyReaderDeath();

		return true;
	}
	
	public boolean changeDumpFile () {

		// check state matrix
        if (! currentState.changeState(4)) {
        	log.writeln(Log.WARNING, "Illegal command: from " + currentState.getCurrentState() + " to 4 - Change Dump File", Log.ECHO);
        	return false;
        }

		ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue<byte[]>();	// new queue
		HubWriter oldHubWriter = writerThread.get(0);

        // create a new writer and start it
		HubWriter hubWriter = null;
		try {
			hubWriter = new HubWriter( prop.DUMP_FILE, queue, log, String.valueOf(writerThread.size()+1), useMagnetometer, prop.SATELLITES_LIST_ARRAY.length );
		} catch (FileNotFoundException e) {
			readerThread = null;
			return false;
		}
		writerThread.add(0, hubWriter);
		writerThread.get(0).start();

		// assign a new queue to the reader
		readerThread.changeQueue(queue);
		
		// tell the old writer that no one will feed his queue anymore
		oldHubWriter.notifyReaderDeath();
		
		return true;
		
	}

	public boolean quit () {

		// check state matrix
        if (! currentState.changeState(7)) {
        	log.writeln(Log.WARNING, "Illegal command: from " + currentState.getCurrentState() + " to q - Quit", Log.ECHO);
        	return false;
        }

		// close application
		close();
	
		return true;
	}

	
	
	/*
	 * Main method
	 */
	public static void main(String[] args) throws IOException {
		
		char			inputChar;
		HubControl		hubCtrl = null;
		
		// Check command line parameter
		if (args.length < 1) {
            System.err.println("usage: java HubControl propertyFile.xml");
            System.exit(1);
		}
		
		// create a new HubControl's object
		try {
			hubCtrl = new HubControl(args[0]);
		} catch (Exception e) {
			e.printStackTrace();
            System.exit(1);
		}

		System.out.println(
				"Enter : 1 - Connect\n" +
				"      : 2 - Init Sensors (NO ACTIVE & SEND & declare sensors & init RTC)\n" +
				"      : 3 - Start Dumping (ACTIVE & SEND)\n" +
				"      : x - 2 + 3\n" +
				"      : 4 - Change Dump File\n" +
				"      : 5 - Stop Dumping (NO ACTIVE & SEND)\n" +
				//"      : 6 - Deactivate Sensors (NO ACTIVE & SEND)\n" +
				"      : 6 - Disconnect\n" +
				"      : q - Quit\n"
						);
		// main loop waiting user input
		while ( true ) {
			// read a command
			inputChar = (char)System.in.read();
			
			switch (inputChar) {
			case '1': // connect
				hubCtrl.connect();
				break;
			case '2': // Init Sensors (declare sensors & init RTC)
				hubCtrl.initSensors(true, true);
				break;
			case '3': // Start Dumping (ACTIVE & SEND)
				hubCtrl.startDumping();
				break;
			case 'x': // Init Sensors (declare sensors & init RTC) + Start Dumping (ACTIVE & SEND)
				hubCtrl.initSensors(true, true);
				hubCtrl.startDumping();
				break;
			case '4': // Change dump file
				hubCtrl.changeDumpFile();
				break;
			case '5': // Stop Dumping (NOT ACTIVE & SEND)
				hubCtrl.stopDumping();
				break;
			//case '6': // Deactivate Sensors (NOT ACTIVE & SEND)
			//	hubCtrl.initSensors(false, false);
			//	break;
			case '6': // Disconnect
				hubCtrl.disconnect();
				break;
			case 'q': // quit
				break;
			}
			if (inputChar == 'q') break;
		}
		hubCtrl.quit();
	}

}


class HubControlState {
	// states matrix: rows -> current state; column -> next state
	private boolean[][]				stateMatrix		= {
			/* start         end -> 	start	1		2		3		4		5		6		q	*/	
			/* initial state */			{false,	true,	false,	false,	false,	false,	false,	true},
			/* 1 - connect */			{false,	false,	true,	false,	false,	false,	true,	false},
			/* 2 - Init Sensors */		{false,	false,	false,	true,	false,	false,	true,	false},
			/* 3 - Start Dumping */		{false,	false,	false,	false,	true,	true,	false,	false},
			/* 4 - Change Dump File */	{false,	false,	false,	false,	true,	true,	false,	false},
			/* 5 - Stop Dumping */		{false,	false,	false,	true,	false,	false,	true,	false},
			/* 6 - Disconnect */		{false,	true,	false,	false,	false,	false,	false,	true},
			/* q - Quit */				{false,	false,	false,	false,	false,	false,	false,	false}
	};
	private int						currentState	= 0; // initial state
	
	public boolean changeState ( int p_newState ) {
        // check state matrix
        if (!stateMatrix[currentState][p_newState]) {
        	// new state is incorrect
        	return false;
        }
        // new state : valid
        currentState = p_newState;
        
        return true;
	}
	
	public int getCurrentState () {
		return currentState;
	}

	
}