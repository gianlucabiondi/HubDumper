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
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HubControl {

	/*
	 * local variables
	 */
	private Socket							hubSocket	= null; // socket to the hub
	private DataInputStream					inStream	= null;	// input stream coming from the hub
	private DataOutputStream				outStream	= null;	// output stream coming from the hub
	private ConcurrentLinkedQueue<byte[]>	queue		= null;	// queue where to put readed frames
	private Properties						prop		= null; // command line properties
	/* Main properties:
	 * HUB_IP			string
	 * HUB_PORT			int
	 * LOG_LEVEL		int 	possible value 0:NONE, 1:ERR, 2:WARN 3:ALL
	 * LOG_FILE 		string
	 * SATELLITES_LIST	string	list values separated by comma (,)
	 */
	private int								logLevel	= 2;
	private PrintStream						log			= null;

	/*
	 * Constructor:
	 */
	public HubControl() {
		// TODO Auto-generated constructor stub 
	}

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
		
		// read the properties immediately needed
		logLevel = Integer.parseInt( prop.getProperty("LOG_LEVEL", Integer.toString(logLevel)) );
		if ( logLevel > 1 & prop.getProperty("LOG_FILE") == "" ) {
			System.err.println("LOG_LEVEL > 0 but no LOG_FILE specified");
		}
		if ( prop.getProperty("LOG_FILE") != null ) {
			openLogFile();
		}
       	if (logLevel >= 1 & log != null) log.println("Start application " + (new Timestamp(System.currentTimeMillis())).toString());
				
		return true;
	}
	
	public boolean close () {

       	if (logLevel >= 1 & log != null) log.println("Close application " + (new Timestamp(System.currentTimeMillis())).toString() + "\n");
		closeLogFile();

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
        	
        	if (logLevel >= 3 & log != null) log.println("	Trying to connect to " + hostName + ":" + portNumber);
        	hubSocket = new Socket( hostName, portNumber );
        	if (logLevel >= 3 & log != null) log.println("	Connected succefsully to " + hostName + ":" + portNumber);
        	
        	hubSocket.setSoTimeout(1);
        	if (logLevel >= 3 & log != null) log.println("	Set SO_TIMEOUT to 1ms");
        	
        	inStream  = new DataInputStream( hubSocket.getInputStream());
        	outStream = new DataOutputStream( hubSocket.getOutputStream());

        } catch (UnknownHostException e) {
        	if (logLevel >= 1 & log != null) log.println("Host ? " + hostName);
            System.err.println("Host ? " + hostName);
            System.exit(1);
        } catch (IOException e) {
        	if (logLevel >= 1 & log != null) log.println("I/O error in opening socket");
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
	        	if (logLevel >= 3 & log != null) log.println("	Disconnected from hub");
			}

		} catch (IOException e) {
        	if (logLevel >= 1 & log != null) log.println("I/O error in closing socket");
            System.err.println("I/O error in closing socket");
            System.exit(1);
        }

		return true;
	}

	public boolean activate ( boolean b_set_rtc, boolean b_set_satellites ) {
		String[] aSatList = prop.getProperty( "SATELLITES_LIST" ).split("\\s*,\\s*");
		HubCommandData	command = new HubCommandData (
				outStream,
				HubCommandData.WIFI_NOT_ACTIVE | 
				HubCommandData.WIFI_SEND |
				( b_set_rtc ? HubCommandData.WIFI_SET_RTC : HubCommandData.WIFI_VOID ) |
				( b_set_satellites ? HubCommandData.WIFI_SET_SATELLITES : HubCommandData.WIFI_VOID ),
				aSatList);
		return true;
	}
	
	public boolean startRecording () {
		HubCommandData	command = new HubCommandData (	
				outStream,
				HubCommandData.WIFI_ACTIVE | 
				HubCommandData.WIFI_SEND );
		return true;
	}
	
	private boolean openLogFile () {
		SimpleDateFormat dateFormat = new SimpleDateFormat("_yyyy-MM-dd_HH_mm_ss."); 
		StringBuilder	 sb;
		String			 fileName;

		if (log != null) {
			return true;
		}
		
		try {

			fileName = prop.getProperty("LOG_FILE");
			sb = new StringBuilder( fileName );
			fileName = sb.replace(fileName.lastIndexOf('.'), fileName.lastIndexOf('.')+1, dateFormat.format(new Date())).toString();
			
			log = new PrintStream( new FileOutputStream( fileName, true /* append */ ));

	       	if (logLevel >= 1 & log != null) log.println("Changed log file " + (new Timestamp(System.currentTimeMillis())).toString() + "\n");

		} catch (FileNotFoundException e) {
			System.err.println("Error in opening propertis file: file not found!");
            System.exit(1);
		} 
		
		return true;
		
	}
	
	private boolean closeLogFile () {

		if (log != null) { 
	       	if (logLevel >= 1 & log != null) log.println("Change log file " + (new Timestamp(System.currentTimeMillis())).toString() + "\n");
			log.close();
			log = null;
		}

		return true;
	}

	/*	public boolean sendCommand ( int state, int rtcValue ) {
	return true;
}
*/	
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
		
		System.out.println(
				"Enter : 1 - Connect\n" +
				"      : 2 - Start Recording\n" +
				"      : 3 - Activate Sensors\n" +
				"      : 4 - Deactivate Sensors\n" +
				"      : 5 - Stop Recording\n" +
				"      : 6 - Disconnect\n" +
				"      : 7 - Change Log File\n" +
				"      : q - Quit\n"
						);
		// main loop waiting user input
		while ( (inputChar = (char)System.in.read()) != 'q'  ) {
			switch (inputChar) {
			case '1': // connect
				hubCtrl.connect();
				break;
			case '2': // Start Recording
				hubCtrl.startRecording();
				break;
			case '3': // Activate Sensors
				break;
			case '4': // Deactivate Sensors
				break;
			case '5': // Stop Recording
				break;
			case '6': // Disconnect
				hubCtrl.disconnect();
				break;
			case '7': // Change log file
				hubCtrl.closeLogFile();
				hubCtrl.openLogFile();
				break;
			case 'q': // quit
				break;
			}
		}
		hubCtrl.disconnect();
		hubCtrl.close();
	}

}
