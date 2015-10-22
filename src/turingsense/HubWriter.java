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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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

	private static final int				SLEEP_A_BIT		= 1000;		// length of idle time waiting the queue fills
	private static final int				MAX_NUM_SLEEPS	= 3;		// max times the thread can wait for the queue to fill
	/*
	 * local variables
	 */
	private ConcurrentLinkedQueue<byte[]>	queue			= null;		// queue where to put readed frames
	private PrintStream						dumpFile		= null;		// here we write frames sent from the hub
	private Log								log				= null;		// log file
	private boolean							thereIsAReader	= true;		// true -> there is a reader feeding the queue
																		// false -> there is no reader feeding the queue

	/*
	 * Constructor:
	 */
	public HubWriter( String p_fileName, ConcurrentLinkedQueue<byte[]> p_que, Log p_log ) {
		
		// record the log's reference
		log = p_log;

		// record the queue's reference
		queue = p_que;

		// open the dump file - append the actual date 
		String 				fileName	= p_fileName;
		SimpleDateFormat	dateFormat	= new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss"); 
		StringBuilder 		sb			= new StringBuilder( fileName );
		fileName = sb.replace( fileName.lastIndexOf("%t"), 
				fileName.lastIndexOf("%t")+2, 
				dateFormat.format(new Date())).toString();

		try {
			if (log != null) log.write( Log.INFORMATION, "Writer Thread: going to write to " + fileName );
			dumpFile = new PrintStream( fileName );

		} catch (FileNotFoundException e) {
			dumpFile = null;
			System.err.println("Error in opening dump file: file not found!");
		} 

		
	}

	public HubWriter( String p_fileName, ConcurrentLinkedQueue<byte[]> p_que ) {
		this( p_fileName, p_que, null );
		
	}
	
	/**
	 * Public Methods
	 */
	public void notifyReaderDeath() {
		thereIsAReader = false;
	}

	/**
	 * Execution method. Here the class start reading from the queue.
	 * 
	 */
	@Override
    public void run() {
		
		boolean	leggi		= true;
		int		numSleeps	= 0;
		int		numFrames	= 0;
		byte[]	frame;
    	long	now;
    	long	startCicle, lenghtCicle, maxCicle = 0, minCicle = Long.MAX_VALUE, sumCicle = 0;
		
		if (log != null) log.write( Log.INFORMATION, "Writer Thread: starting ..." );

		now = System.nanoTime();
		startCicle = now;

		while (leggi) {
			
        	// try to read next element from the queue
			frame = queue.poll();
			
			// if frame is null then the queue is empty
			// otherwise translate the frame & write it into the file 
			if ( frame == null ) {
				
				// if there isn't any reader feeding the queue, then if this is empty It will be like this forever
				// otherwise wait a bit for someone filling the queue
				// exits also if we waited for too long
				if ( ! thereIsAReader | numSleeps >= MAX_NUM_SLEEPS ) {
					if (log != null) log.write( Log.INFORMATION, "Writer Thread: There is not a reader and slept " + numSleeps + " times" );
					break;
				}
				try {
					numSleeps++;
					if (log != null) log.write( Log.INFORMATION, "Writer Thread: The queue is empty - " + numSleeps );
					Thread.sleep( SLEEP_A_BIT );
				} catch (InterruptedException e) {
					continue;
				}
				
			} else {
				
				ByteBuffer buf = ByteBuffer.wrap(frame);

				// Hub speaks LITTLE_ENDIAN "language"
				buf.order( ByteOrder.LITTLE_ENDIAN );

				// reset counts
				numSleeps = 0;
				numFrames++;
				
	        	if (log != null) log.write( Log.INFORMATION, "Writer Thread:  " + Arrays.toString(frame) );

	        	// translate the frame from byte
				//dumpFile.println(Arrays.toString(frame));
				dumpFile.print( buf.getInt() + " ");
				dumpFile.print( buf.get() + " ");
				dumpFile.print( buf.getInt() + " ");
				dumpFile.print( buf.getInt() + " ");
				dumpFile.print( buf.getInt() + " ");

				for (int i = 0; i < 10; i++ ) {
					dumpFile.print( buf.getInt() + " ");
					dumpFile.print( buf.getShort() + " ");
					dumpFile.print( buf.getShort() + " ");
					dumpFile.print( buf.getShort() + " ");
					dumpFile.print( buf.getShort() + " ");
					dumpFile.print( buf.getShort() + " ");
					dumpFile.print( buf.getShort() + " ");
					dumpFile.print( buf.getShort() + " ");
					dumpFile.print( buf.getShort() + " ");
					dumpFile.print( buf.getShort() + " ");
					dumpFile.print( buf.getFloat() + " ");
					dumpFile.print( buf.getFloat() + " ");
					dumpFile.print( buf.getFloat() + " ");
					dumpFile.print( buf.getFloat() + " ");
				}
				
				dumpFile.println();

	        	// performance calc
	        	now = System.nanoTime();
	        	//
	        	lenghtCicle = now - startCicle;
	        	sumCicle += lenghtCicle;
	        	if (lenghtCicle > maxCicle) maxCicle = lenghtCicle;
	        	if (lenghtCicle < minCicle) minCicle = lenghtCicle;
	        	startCicle = now;

	        	if (log != null) log.write( Log.INFORMATION, "Writer Thread: frame " + numFrames + " - bytes written: " + 
	        			frame.length + " in " + Math.round(lenghtCicle/1000F) + "us" );

			}
			
		}
		
		if (log != null) log.write( Log.INFORMATION, "Writer Thread: closing ..." );
		if (numFrames > 0) {
        	if (log != null) log.write( Log.INFORMATION, "Writer Thread: Read " + numFrames + " frames" );
			if (log != null) log.write( Log.INFORMATION, "Writer Thread: CICLE length - max " + Math.round(maxCicle/1000F) + "us " +
					"- min " + Math.round(minCicle/1000F) + "us " +
					"- avg " + Math.round(sumCicle / numFrames /1000F) + "us" );
		}
		
		// close the dump file
		dumpFile.close();
		
	}
}
