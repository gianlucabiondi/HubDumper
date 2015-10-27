/**
 * Writer Class: this class pulls frames from the queue and writes them down in dump file
 * 	There can be more than one Writer per application but only one per file written.
 * it receives:
 * - a queue where it takes the frames to write
 * - a file name where to dump queue contents
 */
package turingsense;

import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author gianluca
 * 
 * This class manage writes from the queue.
 * Initially it is created with a file name where to write down frames and a queue where it taks frames. 
 * The same queue should be passed to a producer process also (the reader). 
 * When the run method is invoked, it starts polling continuously the queue reading frames.
 *
 */
public class HubWriter extends Thread {

	/*
	 * local variables
	 */
	private ConcurrentLinkedQueue<byte[]>	queue				= null;		// queue where to put read frames
	private PrintStream						dumpFile			= null;		// here we write frames sent from the hub
	private Log								log					= null;		// log file
	private boolean							thereIsStillAReader	= true;		// true -> there is a reader feeding the queue
																			// false -> there is no reader feeding the queue
	private boolean							useMag				= Common.DEFAULT_USE_MAGNETOMETER;
	private int								numActiveSatellites;			// number of effective satellites used

	/*
	 * Constructor:
	 */
	public HubWriter( String p_fileName, ConcurrentLinkedQueue<byte[]> p_que, Log p_log, String p_threadName, boolean p_useMag, int p_numActiveSatellites ) {
		
		// record the log's reference
		log = p_log;

		// record the queue's reference
		queue = p_que;
		
		useMag = p_useMag;
		numActiveSatellites = p_numActiveSatellites;
		
		// set the thread name
		if ( p_threadName != null ) this.setName( p_threadName );

		// open the dump file - append the actual date 
		String 				fileName	= p_fileName;
		SimpleDateFormat	dateFormat	= new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss"); 
		StringBuilder 		sb			= new StringBuilder( fileName );
		fileName = sb.replace( fileName.lastIndexOf("%t"), 
				fileName.lastIndexOf("%t")+2, 
				dateFormat.format(new Date())).toString();

		if (log != null) log.writeln( Log.WARNING, "Writer Thread " + getName() + ": creating ..." );

		try {

			dumpFile = new PrintStream( fileName );
			if (log != null) log.writeln( Log.INFORMATION, "Writer Thread " + getName() + ": going to write to " + fileName );

		} catch (FileNotFoundException e) {
			dumpFile = null;
			System.err.println("Error in opening dump file: file not found!");
		} 

		
	}

	public HubWriter( String p_fileName, ConcurrentLinkedQueue<byte[]> p_que, Log p_log, String p_threadName, boolean p_useMag ) {
		this( p_fileName, p_que, p_log, p_threadName, p_useMag, -1 );
	}
	
	public HubWriter( String p_fileName, ConcurrentLinkedQueue<byte[]> p_que, Log p_log, String p_threadName ) {
		this( p_fileName, p_que, p_log, p_threadName, Common.DEFAULT_USE_MAGNETOMETER );
	}
	
	public HubWriter( String p_fileName, ConcurrentLinkedQueue<byte[]> p_que, Log p_log ) {
		this( p_fileName, p_que, p_log, null );
	}
	
	public HubWriter( String p_fileName, ConcurrentLinkedQueue<byte[]> p_que ) {
		this( p_fileName, p_que, null );
	}
	
	/**
	 * Public Methods
	 */
	public void notifyReaderDeath() {
		thereIsStillAReader = false;
	}

	/**
	 * Execution method. Here the class start reading from the queue.
	 * 
	 */
	@Override
    public void run() {
		
		boolean	readAgain			= true;
		int		numNonStopSleeps	= 0;
		int		numFrames			= 0;
		byte[]	frame;
    	long	now;
    	long	startCicle, lenghtCicle, maxCicle = 0, minCicle = Long.MAX_VALUE, sumCicle = 0;
    	int[]	nValidFrames = new int[Common.MAX_SENSORS];	// valid frames for each satellite
		
		if (log != null) log.writeln( Log.WARNING, "Writer Thread " + getName() + ": starting ..." );

		// main cycle popping frames from the queue and write them to the dump file
		startCicle = System.nanoTime();

		while (readAgain) {
			
        	// try to read next element from the queue
			frame = queue.poll();
			
			// if frame is null then the queue is empty
			// otherwise translate the frame & write it into the file 
			if ( frame == null ) {
				
				// if there isn't any reader feeding the queue, then if this is empty It will be like this forever
				// otherwise wait a bit for someone filling the queue
				// exits also if we waited for too long
				if ( ! thereIsStillAReader ) {
					if (log != null) log.writeln( Log.WARNING, "Writer Thread " + getName() + ": There is not a reader and slept " + numNonStopSleeps + " times" );
					break;
				}
				if ( numNonStopSleeps >= Common.WRITER_MAX_NUM_SLEEPS ) {
					if (log != null) log.writeln( Log.WARNING, "Writer Thread " + getName() + ": Slept too much - " + numNonStopSleeps + " times" );
					break;
				}
				try {
					numNonStopSleeps++;
					if (log != null) log.writeln( Log.WARNING, "Writer Thread " + getName() + ": The queue is empty, I am going to sleep - " + numNonStopSleeps );
					Thread.sleep( Common.WRITER_SLEEP_FOR_EMPTY_QUEUE_MS );
				} catch (InterruptedException e) {
					if (log != null) log.writeln( Log.WARNING, "Writer Thread " + getName() + ": Someone else has interrupted my sleep" );
				} 

				startCicle = System.nanoTime();
				continue;
				
			} 
			
        	if (log != null) log.writeln( Log.DEBUG, "Writer Thread " + getName() + ":  frame - " + Arrays.toString(frame) );
			
			// reset counts
			numNonStopSleeps = 0;
			numFrames++;
				
			// the frame is correct ... 
			// translate it into ints/shorts/ floats and write them to the dump file
			SensorData sensorDataFrame = new SensorData( frame, useMag, log );

			// dump the frame
        	dumpFile.println( sensorDataFrame.toString() );
        	
        	// count valid frames per satellite
        	if (numActiveSatellites > 0) {
            	for (int i = 0; i<numActiveSatellites; i++) {
            		if (sensorDataFrame.isSatelliteValid(i)) nValidFrames[i]++;
            	}
            	if ((numFrames % Common.WRITER_INTERVAL_PRINT_VALID_FRAMES) == 0) {
            		if (log != null) log.write(Log.NONE, String.format((Locale)null, " %8d", numFrames ), Log.ECHO); 
                	for (int i = 0; i<numActiveSatellites; i++) {
                		if (log != null) log.write(Log.NONE, String.format((Locale)null, " %5.1f%%", (nValidFrames[i]*100F/numFrames)), Log.ECHO); 
                	}
            		if (log != null) log.write(Log.NONE, "\r", Log.ECHO); 
            	}
        	}
        	
        	// if cycle is very fast, think about introducing a sleep to allow reader process to gain higher priority
        	try {
				Thread.sleep( Common.WRITER_SLEEP_EVERY_CYCLE_MS );
			} catch (InterruptedException e) {
				if (log != null) log.writeln( Log.WARNING, "Writer Thread " + getName() + ": Someone else has interrupted my sleep" );
			} 

        	// performance calc
        	now = System.nanoTime();
        	//
        	lenghtCicle = now - startCicle;
        	sumCicle += lenghtCicle;
        	if (lenghtCicle > maxCicle) maxCicle = lenghtCicle;
        	if (lenghtCicle < minCicle) minCicle = lenghtCicle;
        	startCicle = now;

        	if (log != null) log.writeln( Log.DEBUG, "Writer Thread " + getName() + ": frame " + numFrames + " - bytes written: " + 
        			frame.length + " in " + Math.round(lenghtCicle/1000F) + "us" );
			
		}
		
		if (log != null) log.writeln( Log.WARNING, "Writer Thread " + getName() + ": closing ..." );
		if (numFrames > 0) {
        	if (log != null) log.writeln( Log.INFORMATION, "Writer Thread " + getName() + ": Read Total " + numFrames + " frames" );
			if (log != null) log.writeln( Log.INFORMATION, "Writer Thread " + getName() + ": CICLE length " + 
					"- max " + Math.round(maxCicle/1000F) + "us " +
					"- min " + Math.round(minCicle/1000F) + "us " +
					"- avg " + Math.round(sumCicle / numFrames /1000F) + "us" );
		}
		
		// close the dump file
		dumpFile.close();
		
	}
	
}
