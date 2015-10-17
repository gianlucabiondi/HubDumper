package turingsense;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
//import java.nio.ByteBuffer;
import java.util.Arrays;

/*
 * This class correspond to the cloud_to_hub_t c struct.
 */
public class HubCommandData {
	
	// Max number of sensors supported
	public static final int	MAX_SENSORS			= 11;
	public static final int	MAX_SAT_SENSORS		= ( MAX_SENSORS - 1 );
	
	// Size in bytes of this "structure"
	public static final int THIS_STRUCT_SIZE	= 52;
	
	// hub commands
	public static final int WIFI_VOID			= (0);
	public static final int WIFI_INVALID		= (1 << 0);
	public static final int WIFI_ACTIVE			= (1 << 1);		// sat ACTIVE (recording)
	public static final int WIFI_NOT_ACTIVE		= WIFI_VOID;	// sat NOT ACTIVE (recording)
	public static final int WIFI_DIAG			= (1 << 2);
	public static final int WIFI_NOT_SEND		= (1 << 3);		// sat NOT COMMUNICATING with hub
	public static final int WIFI_SEND			= WIFI_VOID;	// sat COMMUNICATING with hub
	public static final int WIFI_CALIBRATE		= (1 << 4);
	public static final int WIFI_SET_RTC		= (1 << 5);		// set sat RTC
	public static final int WIFI_SET_SATELLITES	= (1 << 6);		// set num satellites
	public static final int WIFI_VALID_DATA		= (1 << 30);

	private OutputStream		outStream;
	private int					logLevel	= 2;		
	private PrintStream			log			= null;

	// variables for sending the command to the hub 
	private int				command;
	private int				rtc_value;
	private int				num_of_sat;
	private int[]			satellite_ids;

//	private int				
	/*
	 * Contructors
	 */
	public HubCommandData( OutputStream p_outStream, int p_command ) {
		outStream	= p_outStream;
		command		= p_command;
		rtc_value	= 0;
		satellite_ids = new int[MAX_SAT_SENSORS];
	}
	
	public HubCommandData( OutputStream p_outStream, int p_command, String[] p_satellites ) {
		
		this( p_outStream, p_command);
		num_of_sat = p_satellites.length;
		for (byte i=0; i < p_satellites.length; i++ ) {
			satellite_ids[i] = Integer.parseInt(p_satellites[i]);
		}
	}
	
	public HubCommandData( OutputStream p_outStream, int p_command, String[] p_satellites, int p_logLevel, PrintStream p_log ) {
		
		this( p_outStream, p_command, p_satellites);
		logLevel = p_logLevel;
		log = p_log;
	}
	
	/*
	 * Private Methods
	 */
	/*
	 * Public Methods
	 */
	public boolean clear() {
		command 	= 0;
		rtc_value	= 0;
		num_of_sat	= 0;
		satellite_ids = null;
		
		return true;
	}

	/*
	 * Send data to the hub
	 */
	public boolean send() {

		if (logLevel >= 1 & log != null) {
			log.println("	sending command: " + Integer.toBinaryString( command ));
			log.println("	            rtc: " + Integer.toHexString( rtc_value ));
			log.println("	     num_of_sat: " + Integer.toString( num_of_sat ));
			log.println("	     satellites: " + Arrays.toString( satellite_ids ));
		}

		try {
			byte[] cmd = commandToBytes();
			System.out.println( Arrays.toString( cmd ) );
			outStream.write( cmd );
			outStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	/*
	 * Methods for setting correct command
	 */
	public boolean wifiIsACTIVE ( int p_cmd )		{ return ( (p_cmd & WIFI_NOT_SEND) != 0 ); }
	public int wifiCmdSetACTIVE ( int p_cmd )		{ return (p_cmd | WIFI_ACTIVE); }
	public int wifiCmdClearACTIVE ( int p_cmd )		{ return (p_cmd & ~WIFI_ACTIVE); }

	public boolean wifiIsSetNOTSEND ( int p_cmd )	{ return ( (p_cmd & WIFI_NOT_SEND) != 0 ); }
	public int wifiCmdSetNOTSEND ( int p_cmd )		{ return (p_cmd | WIFI_NOT_SEND); }
	public int wifiCmdClearNOTSEND ( int p_cmd )	{ return (p_cmd & ~WIFI_NOT_SEND); }

	public boolean wifiIsSetRTC ( int p_cmd )		{ return ( (p_cmd & WIFI_SET_RTC) != 0 ); }
	public int wifiCmdSetRTC ( int p_cmd )			{ return (p_cmd | WIFI_SET_RTC); }
	public int wifiCmdClearRTC ( int p_cmd )		{ return (p_cmd & ~WIFI_SET_RTC); }

	public boolean wifiIsSetSAT ( int p_cmd )		{ return ( (p_cmd & WIFI_SET_SATELLITES) != 0 ); }
	public int wifiCmdSetSAT ( int p_cmd )			{ return (p_cmd | WIFI_SET_SATELLITES); }
	public int wifiCmdClearSAT ( int p_cmd )		{ return (p_cmd & ~WIFI_SET_SATELLITES); }

	/*
	 * Methods for transforming this command into byte[]
	 */
	byte[] commandToBytes()
	{
	  byte[] result = new byte[THIS_STRUCT_SIZE];

	  result[ 0] = (byte) (command /*>> 0*/);
	  result[ 1] = (byte) (command >> 8);
	  result[ 2] = (byte) (command >> 16);
	  result[ 3] = (byte) (command >> 24);

	  result[ 4] = (byte) (rtc_value /*>> 0*/);
	  result[ 5] = (byte) (rtc_value >> 8);
	  result[ 6] = (byte) (rtc_value >> 16);
	  result[ 7] = (byte) (rtc_value >> 24);

	  result[ 8] = (byte) (num_of_sat /*>> 0*/);
	  result[ 9] = (byte) (num_of_sat >> 8);
	  result[10] = (byte) (num_of_sat >> 16);
	  result[11] = (byte) (num_of_sat >> 24);

	  for (int i = 0; i< num_of_sat; i++) {
		  result[12+0+(i*4)] = (byte) (satellite_ids[i] /*>> 0*/);
		  result[12+1+(i*4)] = (byte) (satellite_ids[i] >> 8);
		  result[12+2+(i*4)] = (byte) (satellite_ids[i] >> 16);
		  result[12+3+(i*4)] = (byte) (satellite_ids[i] >> 24);
	  }

	  return result;
	}
}
