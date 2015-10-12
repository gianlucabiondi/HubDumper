package turingsense;

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
	public static final int WIFI_INVALID		= (1 << 0);
	public static final int WIFI_START			= (1 << 1);		// sat ACTIVE (recording) or not
	public static final int WIFI_DIAG			= (1 << 2);
	public static final int WIFI_WAIT			= (1 << 3);		// sat NOT COMMUNICATING with hub or not
	public static final int WIFI_CALIBRATE		= (1 << 4);
	public static final int WIFI_SET_RTC		= (1 << 5);		// set sat RTC
	public static final int WIFI_SET_SATELLITES	= (1 << 6);		// set num satellites
	public static final int WIFI_VALID_DATA		= (1 << 30);

	// variables for sending the command to the hub 
	private int				command;
	private int				rtc_value;
	private int				num_of_sat;
	private int[]			satellite_ids;

//	private int				
	/*
	 * Contructors
	 */
	/*public HubCommandData() {
		satellite_ids = new int[MAX_SAT_SENSORS];
	}*/
	
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
	 * Methods for setting correct command
	 */
	public int wifiCmdSetSTART ( int p_cmd )	{ return (p_cmd | WIFI_START); }
	public int wifiCmdClearSTART ( int p_cmd )	{ return (p_cmd & ~WIFI_START); }

	public int wifiCmdSetWAIT ( int p_cmd )		{ return (p_cmd | WIFI_WAIT); }
	public int wifiCmdClearWAIT ( int p_cmd )	{ return (p_cmd & ~WIFI_WAIT); }

	public int wifiCmdSetRTC ( int p_cmd )		{ return (p_cmd | WIFI_SET_RTC); }
	public int wifiCmdClearRTC ( int p_cmd )	{ return (p_cmd & ~WIFI_SET_RTC); }

	public int wifiCmdSetSAT ( int p_cmd )		{ return (p_cmd | WIFI_SET_SATELLITES); }
	public int wifiCmdClearSAT ( int p_cmd )	{ return (p_cmd & ~WIFI_SET_SATELLITES); }

}
