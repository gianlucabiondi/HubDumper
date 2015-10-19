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
package turingsense;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
//import java.text.SimpleDateFormat;
//import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HubControl {

	/*
	 * local variables
	 */
	private Socket							hubSocket		= null; // socket to the hub
	private DataInputStream					inStream		= null;	// input stream coming from the hub
	private OutputStream					outStream		= null;	// output stream coming from the hub
	private Properties						prop			= null; // command line properties
	private HubReader 						readerThread	= null; // reader class
	/* Main properties:
	 * HUB_IP			string
	 * HUB_PORT			int
	 * LOG_LEVEL		int 	possible value 0:NONE, 1:ERR, 2:WARN 3:ALL
	 * LOG_FILE 		string
	 * SATELLITES_LIST	string	list values separated by comma (,)
	 * DUMP_FILE 		string
	 */
	private Log								log				= null; // application log
//	private Log								dumpFile		= null; // dump file
	private boolean							useMagnetometer = (SensorData.BYTES_READ_FROM_HUB_DEFAULT == SensorData.BYTES_READ_FROM_HUB_MAG ) ? true : false;
	private static int						CONNECT_TIMEOUT	= 500; // connection timeout
	
	/*
	 * Constructor:
	 */

	/*
	 * Methods:
	 * - inizitialize the class with parameters (in xml format)
	 * - close the class
	 * - connect to the hub
	 * - stop recording
	 * - send command to the hub
	 */
	public boolean init ( String p_propFile) {
		
		prop = new Properties();
		
		// open and read property file
		try {

			this.prop.loadFromXML(new FileInputStream(p_propFile));

		} catch (FileNotFoundException e) {
			System.err.println("Error in opening propertis file: file not found!");
            System.exit(1);
		} catch (IOException e) {
			System.err.println("Error in opening propertis file!");
            System.exit(1);
		}
		
		// open the application log file
		log = new Log( prop.getProperty("LOG_LEVEL"), prop.getProperty("LOG_FILE"));
		log.write( Log.INFORMATION , "\nStart application " + (new Timestamp(System.currentTimeMillis())).toString() );
				
		// open the dump file - append the actual date 
//		String 				fileName	= prop.getProperty("DUMP_FILE");
//		SimpleDateFormat	dateFormat	= new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss"); 
//		StringBuilder 		sb			= new StringBuilder( fileName );
//		fileName = sb.replace( fileName.lastIndexOf("%t"), 
//				fileName.lastIndexOf("%t")+2, 
//				dateFormat.format(new Date())).toString();
//		dumpFile = new Log( Log.ALL, fileName );

		// read main properties
		useMagnetometer = (prop.getProperty("MAGNETOMETER").equals("YES") ) ? true : false;
				
		return true;
	}
	
	public boolean close () {

       	log.write( Log.INFORMATION, "Close application " + (new Timestamp(System.currentTimeMillis())).toString() + "\n");
		log.close();

		return true;
	}

	public boolean connect () {
		
        String	hostName	= prop.getProperty("HUB_IP");
        int 	portNumber	= Integer.parseInt(prop.getProperty("HUB_PORT"));

        // if socket is already open then return immediately
        if (hubSocket != null) { 
        	return true;
        }
        
        // create a new socket
        try {
        	
        	log.write(Log.INFORMATION, "	Trying to connect to " + hostName + ":" + portNumber);
        	
        	hubSocket = new Socket( hostName, portNumber );
        	log.write(Log.INFORMATION, "	Connected succefsully to " + hostName + ":" + portNumber);
        	
        	hubSocket.setSoTimeout( CONNECT_TIMEOUT );
        	log.write(Log.INFORMATION, "	Set SO_TIMEOUT to " + CONNECT_TIMEOUT + "ms");
        	
        	inStream  = new DataInputStream( hubSocket.getInputStream());
        	outStream = hubSocket.getOutputStream();

        } catch (UnknownHostException e) {
        	log.write(Log.ERROR, "Host ? " + hostName);
            System.err.println("Host ? " + hostName);
            System.exit(1);
        } catch (IOException e) {
        	log.write(Log.ERROR, "I/O error in opening socket");
            System.err.println("I/O error in opening socket");
            System.exit(1);
        } 
        
		return true;
	}
	
	public boolean disconnect () {
		try {
			
			if (hubSocket != null) {
				hubSocket.close();
				hubSocket = null;
	        	log.write(Log.INFORMATION, "	Disconnected from hub");
			}

		} catch (IOException e) {
        	log.write(Log.ERROR, "I/O error in closing socket");
            System.err.println("I/O error in closing socket");
            System.exit(1);
        }

		return true;
	}

	public boolean initSensors ( boolean b_set_rtc, boolean b_set_satellites ) {
		
		String[] aSatList = prop.getProperty( "SATELLITES_LIST" ).split("\\s*,\\s*");
		
		// NOT_ACTIVE & SENDING
		// (eventually SET_RTC & SET_SATELLITES)
		HubCommandData	command = new HubCommandData (
				outStream,
				HubCommandData.WIFI_NOT_ACTIVE | 
				HubCommandData.WIFI_SEND |
				( b_set_rtc ? HubCommandData.WIFI_SET_RTC : HubCommandData.WIFI_VOID ) |
				( b_set_satellites ? HubCommandData.WIFI_SET_SATELLITES : HubCommandData.WIFI_VOID ),
				aSatList,
				log );
		
		return command.send();
	}
	
	public boolean startDumping () {

		ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue<byte[]>();	// queue where to put readed frames
		String[] aSatList = prop.getProperty( "SATELLITES_LIST" ).split("\\s*,\\s*");

		// prepare Queue and reading thread
		readerThread = new HubReader( inStream, queue, log );
		readerThread.setUseMagnetometer( useMagnetometer );

		// ACTIVE & SENDING
		HubCommandData	command = new HubCommandData (	
				outStream,
				HubCommandData.WIFI_ACTIVE | 
				HubCommandData.WIFI_SEND,
				aSatList,
				log );
		
		if ( ! command.send() ) {
			return false;
		};
		
		// start new thread to read from the hub
		readerThread.start();
		
		return true;
		
	}
	
	public boolean stopDumping () {

		String[] aSatList = prop.getProperty( "SATELLITES_LIST" ).split("\\s*,\\s*");

		// NOT ACTIVE & SENDING
		HubCommandData	command = new HubCommandData (	
				outStream,
				HubCommandData.WIFI_NOT_ACTIVE | 
				HubCommandData.WIFI_SEND,
				aSatList,
				log );
		
		if ( ! command.send() ) {
			return false;
		};

		// stop the reading thread 
		readerThread.stopReading();
		readerThread	= null;
		
		return true;
	}
	
	
	/*
	 * Main method
	 */
	public static void main(String[] args) throws IOException {
		
		char			inputChar;
		HubControl		hubCtrl = new HubControl();
		
		// Init
		if (args.length < 1) {
            System.err.println("usage: java HubControl propertyFile.xml");
            System.exit(1);
		}
		hubCtrl.init(args[0]);
		//System.out.print((prop.getProperty("MAGNETOMETER") == "YES")?"YES":"NO");

		System.out.println(
				"Enter : 1 - Connect\n" +
				"      : 2 - Init Sensors (declare sensors & init RTC)\n" +
				"      : 3 - Start Dumping\n" +
				"      : 4 - Stop Dumping\n" +
				"      : 5 - Deactivate Sensors (NO ACTIVE & SENDING)\n" +
				"      : 6 - Disconnect\n" +
				"      : 7 - Change Dump File\n" +
				"      : q - Quit\n"
						);
		// main loop waiting user input
		while ( (inputChar = (char)System.in.read()) != 'q'  ) {
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
			case '4': // Stop Dumping (NOT ACTIVE & SEND)
				hubCtrl.stopDumping();
				break;
			case '5': // Deactivate Sensors (NOT ACTIVE & SEND)
				hubCtrl.initSensors(false, false);
				break;
			case '6': // Disconnect
				hubCtrl.disconnect();
				break;
			case '7': // Change dump file
				break;
			case 'q': // quit
				break;
			}
		}
		hubCtrl.disconnect();
		hubCtrl.close();
	}

}
