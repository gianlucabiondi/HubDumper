package turingsense;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
//import java.sql.Timestamp;
//import java.text.SimpleDateFormat;
//import java.util.Arrays;
//import java.util.Date;

public class Log {

	// static const for log level
	public static int	NONE			= 0;
	public static int	ERROR			= 1;
	public static int	WARNING			= 2;
	public static int	INFORMATION		= 3;
	public static int	ALL				= INFORMATION;
	public static int	DEFAULT_LEVEL	= WARNING;
	
	// private variables
	private int			logLevel		= DEFAULT_LEVEL;
	private PrintStream	log				= null;

	/**
	 * Constructor
	 * An outputStream gets opened, associated to the FILE_NAME requested
	 */
	public Log( int p_logLevel, String p_fileName ) {

		// log level
		logLevel = p_logLevel;
		if (logLevel != NONE &
				logLevel != ERROR &
				logLevel != WARNING &
				logLevel != INFORMATION) {
			logLevel = DEFAULT_LEVEL;
		}
				
		
		// if is requested a log but is not supplied a file name then -> write to stdout!!!
		if ( logLevel > NONE & p_fileName == "" ) {

			log = System.out;
			//System.err.println("LOG_LEVEL > 0 but no LOG_FILE specified");
			//logLevel = NONE;

		} else {
			
			try {

				log = new PrintStream( new FileOutputStream( p_fileName, true /* append */ ));

			} catch (FileNotFoundException e) {
				System.err.println("Error in opening propertis file: file not found!");
				logLevel = NONE;
			} 
			
		}

	}
	
	public Log( String p_logLevel, String p_fileName ) {
		this( Integer.parseInt( p_logLevel ), p_fileName );
	}
	
	public Log( String p_fileName ) {
		this( ALL, p_fileName );
	}
	
	/**
	 * Write into log file 
	 */
	public void write( int p_level, String p_text ) {
		if ( p_level <= logLevel ) {
			log.println( p_text );
		}
	}
	
	public void write( String p_text ) {
		this.write( NONE, p_text );
	}
	
	/**
	 * Close log file
	 * This close the OutputStream where this class writes to
	 */
	public void close( ) {
		if (log != null) { 
			log.close();
			log = null;
		}
	}

	/*
	 * Static methods
	 */
	public static String int32ToBin( int n ) {
		String result = "00000000000000000000000000000000" + Integer.toBinaryString( n );
		return result.substring(result.length() - 32, result.length());
	}
	
}
