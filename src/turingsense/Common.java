package turingsense;

class Common {
	
	// socket specific constants
	public static final int		SOCKET_TIMEOUT_MS					= 500; // connection timeout

	// number of sensors constants
	public static final int		MAX_SENSORS							= 11;
	public static final int		MAX_SAT_SENSORS						= ( MAX_SENSORS - 1 );
	
	// type of sensors constants
	public static final boolean	DEFAULT_USE_MAGNETOMETER			= true;
	
	// writer thread constants
	public static final int		WRITER_SLEEP_FOR_EMPTY_QUEUE_MS		= 1000;		// length of idle time waiting the queue fills
	public static final int		WRITER_MAX_NUM_SLEEPS				= 3;		// max times the thread can wait for the queue to fill
	public static final int		WRITER_SLEEP_EVERY_CYCLE_MS			= 1;
	public static final int		WRITER_INTERVAL_PRINT_VALID_FRAMES	= 50;

	// log specific constants
	public static final int		DEFAULT_LOG_LEVEL					= 2;
	
	// dump file specific constants
	public static final String	FIELD_SEPARATOR						= "\t";
	
}
