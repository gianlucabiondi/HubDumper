package turingsense;

import java.io.IOException;
import java.io.OutputStream;
//import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/*
 * This class correspond to the cloud_to_hub_t c struct.
 */
public class HubCommandData {
	
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
	public static final int	WIFI_VALID_DATA		= (1 << 30);

	private OutputStream	outStream;
	private Log				log					= null;

	// variables for sending the command to the hub 
	private int				command;
	private int				rtc_value;
	private int				num_of_sat;
	private int[]			satellite_ids;
	
	// Size in bytes of this "structure" sent to the hub
	public static final int BYTES_SENT_TO_HUB	= 
			Integer.BYTES + 						// command
			Integer.BYTES + 						// rtc_value
			Integer.BYTES + 						// num_of_sat
			Integer.BYTES * Common.MAX_SAT_SENSORS;	// satellite_ids[ MAX_SAT_SENSORS ]
	
//	private int				
	/*
	 * Constructors
	 * Since I don't know if sat. number and sat. list need to be repeated for each command
	 * I provide constructor also for command without sat. number and sat. list.
	 */
	public HubCommandData( OutputStream p_outStream, int p_command, String[] p_satellites, Log p_log ) {

		outStream		= p_outStream;
		command			= p_command;
		rtc_value		= 0;
		
		num_of_sat = p_satellites.length;
		satellite_ids = new int[Common.MAX_SAT_SENSORS];
		for (byte i=0; i < p_satellites.length; i++ ) {
			satellite_ids[i] = Integer.parseInt(p_satellites[i]);
		}

		log = p_log;
	}
	
	public HubCommandData( OutputStream p_outStream, int p_command, String[] p_satellites ) {
		
		this( p_outStream, p_command, p_satellites, null );

	}
	
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

		if (log != null) {
			log.writeln(Log.INFORMATION, "	----" );
			log.writeln(Log.INFORMATION, "	sending command: " + command + " - " + Log.int32ToBin( command ));
			log.writeln(Log.INFORMATION, "	            rtc: " + Integer.toHexString( rtc_value ));
			log.writeln(Log.INFORMATION, "	     num_of_sat: " + Integer.toString( num_of_sat ));
			log.writeln(Log.INFORMATION, "	     satellites: " + Arrays.toString( satellite_ids ));
			log.writeln(Log.INFORMATION, "	----" );
		}

		// prepare the "string" commend to send
		ByteBuffer	buf	= ByteBuffer.allocate(BYTES_SENT_TO_HUB);
		
		// Hub speaks LITTLE_ENDIAN "language"
		buf.order( ByteOrder.LITTLE_ENDIAN );
		
		buf.putInt(command);
		buf.putInt(rtc_value);
		buf.putInt(num_of_sat);
		for (int i = 0; i<num_of_sat; i++) {
			buf.putInt(satellite_ids[i]);
		}
		if (log != null) log.writeln(Log.DEBUG, Arrays.toString( buf.array() ));

		// send command to the hub via socket stream
		try {

			outStream.write( buf.array() );

		} catch (IOException e) {
			log.writeln(Log.ERROR, "===== ERROR: Error in sending commands to the hub" );
			e.printStackTrace();
		}
		
		return true;
	}
	
	/*
	 * Methods for setting correct command
	 */
/*	public boolean wifiIsACTIVE ( int p_cmd )		{ return ( (p_cmd & WIFI_NOT_SEND) != 0 ); }
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
*/
	
}
