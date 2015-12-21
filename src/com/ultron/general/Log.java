package com.ultron.general;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
//import java.sql.Timestamp;
//import java.text.SimpleDateFormat;
//import java.util.Arrays;
//import java.util.Date;


public class Log {

	// static const for log level
	public static final int	NONE			= 0;
	public static final int	ERROR			= 1;
	public static final int	WARNING			= 2;
	public static final int	INFORMATION		= 3;
	public static final int	DEBUG			= 4;
	public static final int	ALL				= DEBUG;
	//public static final int	DEFAULT_LEVEL	= WARNING;

	public static final boolean	ECHO		= true;

	// private variables
	private int				logLevel		= Common.DEFAULT_LOG_LEVEL;
	private PrintStream		log				= null;

	/**
	 * Constructor
	 * An outputStream gets opened, associated to the FILE_NAME requested
	 */
	public Log( int p_logLevel, String p_fileName ) {

		// log level
		if (p_logLevel != NONE &&
				p_logLevel != ERROR &&
				p_logLevel != WARNING &&
				p_logLevel != INFORMATION &&
				p_logLevel != DEBUG) {
			logLevel = Common.DEFAULT_LOG_LEVEL;
		} else {
			logLevel = p_logLevel;
		}
				

		// open log file (only for logLevel > NONE !!!)
		if (logLevel > NONE) {
			// if is requested a log but is not supplied a file name then -> write to stdout!!!
			if ( p_fileName.equals("") ) {

				log = System.out;

			} else {
				
				try {

					log = new PrintStream( new FileOutputStream( p_fileName, true /* append */ ));

				} catch (FileNotFoundException e) {
					e.printStackTrace();
					System.err.println("===== ERROR: Error in opening propertis file: file not found!");
					logLevel = NONE;
					log = null;
				} 
				
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
	public void write( int p_level, String p_text, boolean p_echo ) {
		if ( log != null && p_level <= logLevel ) {
			log.print( p_text );
			if (p_echo && log != System.out) { 
				System.out.print( p_text );
			}
		}
	}
	
	public void write( int p_level, String p_text ) {
		this.write( p_level, p_text, !ECHO );
	}
	
	public void write( String p_text ) {
		this.write( NONE, p_text );
	}
	
	/**
	 * Write into log file with new line
	 */
	public void writeln( int p_level, String p_text, boolean p_echo ) {
		this.write( p_level, p_text + "\n", p_echo );
	}
	
	public void writeln( int p_level, String p_text ) {
		this.writeln( p_level, p_text, !ECHO );
	}
	
	public void writeln( String p_text ) {
		this.writeln( NONE, p_text );
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

	public int getLogLevel() {
		return logLevel;
	}
	
	public PrintStream getOutputStream() {
		return log;
	}
	
	/*
	 * Static methods
	 */
	public static String int32ToBin( int n ) {
		String result = "00000000000000000000000000000000" + Integer.toBinaryString( n );
		return result.substring(result.length() - 32, result.length());
	}
	
}
