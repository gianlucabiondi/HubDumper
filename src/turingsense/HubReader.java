/**
 * Reader Class: 
 * it receives:
 * - an input stream from the HUB (it can be obtained with socket.getInputStream() where socket is
 * 		an already connected socket to the HUB 
 * - a queue where it place the frames read and
 */
package turingsense;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.DataInputStream;
import java.io.IOException;
//import java.io.PrintStream;

/**
 * @author gianluca
 * 
 * This class manage reads from the hub.
 * Initially it is created with DataInputStream already connected to the hub and a queue
 * where it add frames. The same queue should be passed to a consumer process also. 
 * When the run method is invoked, it starts polling continuously the hub 
 * reading frames.
 *
 */
public class HubReader extends Thread {

	/*
	 * local variables
	 */
	private DataInputStream					inStream	= null;	// input stream coming from the hub
	private ConcurrentLinkedQueue<byte[]>	queue		= null;	// queue where to put readed frames
	private Log								log			= null;
	private boolean							stopWorking	= false; // when set to FALSE -> the thread stops
	private boolean							useMag		= Common.DEFAULT_USE_MAGNETOMETER;
	

	/*
	 * Constructor:
	 */
	public HubReader( DataInputStream p_in, ConcurrentLinkedQueue<byte[]> p_que, Log p_log, String p_threadName, boolean p_useMag ) {

		inStream	= p_in;
		queue 		= p_que;
		log			= p_log;
		useMag 		= p_useMag;
		if ( p_threadName != null ) this.setName( p_threadName );

		if (log != null) log.writeln( Log.WARNING, "Reader Thread " + getName() + ": creating ..." );
	}
	
	public HubReader( DataInputStream p_in, ConcurrentLinkedQueue<byte[]> p_que, Log p_log, String p_threadName ) {
		this( p_in, p_que, p_log, p_threadName, Common.DEFAULT_USE_MAGNETOMETER );
	}

	public HubReader( DataInputStream p_in, ConcurrentLinkedQueue<byte[]> p_que, Log p_log ) {
		this( p_in, p_que, p_log, null );
	}

	public HubReader( DataInputStream p_in, ConcurrentLinkedQueue<byte[]> p_que ) {
		this( p_in, p_que, null );
	}

	/**
	 * Public Methods
	 * stop the thread.
	 */
	public void stopReading() {
		stopWorking = true;
	}
	
	/**
	 * change the queue.
	 */
	public void changeQueue( ConcurrentLinkedQueue<byte[]> p_newQueue ) {
		queue = p_newQueue;
	}
	
	/**
	 * Execution method. Here the class start reading from the hub.
	 * 
	 */
	@Override
    public void run() {
		
		int		nFrameSize	= (useMag ? SensorData.BYTES_WITH_MAG: SensorData.BYTES_WITHOUT_MAG);
		int		nBytesRead = 0;
		int		nNumFrames = 0;
    	long	now;
    	long	startCicle, lenghtCicle, maxCicle = 0, minCicle = Long.MAX_VALUE, sumCicle = 0;

		if (log != null) log.writeln( Log.WARNING, "Reader Thread " + getName() + ": going to read " + nFrameSize + " bytes per frame" );
		if (log != null) log.writeln( Log.WARNING, "Reader Thread " + getName() + ": starting ..." );

        // main loop reading frames from the hub
		startCicle = System.nanoTime();
		
		// loop until user request
        while (!stopWorking ) {

        	byte[]	bFrame = new byte[ nFrameSize ];
        	
        	// read next buffer into the buffer
        	try {
				if ((nBytesRead = inStream.read(bFrame, 0, nFrameSize)) < 0) {
			        // manage EOF: exit thread
		    		if (log != null) log.writeln( Log.WARNING, "Reader Thread " + getName() + ": EOF found!" );
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				if (log != null) {
					log.writeln( Log.ERROR, "Reader Thread " + getName() + ": IO Exception!" );
					e.printStackTrace( log.getOutputStream() );
				}
			}
       	
        	if (log != null) log.writeln( Log.DEBUG, "Reader Thread " + getName() + ":  " + Arrays.toString(bFrame) );
        	
        	// count num of frames
        	nNumFrames++;
 
        	// put every frame into the queue
        	queue.add( bFrame );
        	
        	// performance calc
        	if ((log != null) & (log.getLogLevel()>= Log.INFORMATION)) {
        		
            	now = System.nanoTime();
            	//
            	lenghtCicle = now - startCicle;
            	sumCicle += lenghtCicle;
            	if (lenghtCicle > maxCicle) maxCicle = lenghtCicle;
            	if (lenghtCicle < minCicle) minCicle = lenghtCicle;
            	startCicle = now;
            	if (log != null) log.writeln( Log.DEBUG, "Reader Thread " + getName() + ": frame " + nNumFrames + " - bytes read: " + 
            												nBytesRead + " in " + Math.round(lenghtCicle/1000F) + "us " );
        	}
	        	
        }

		if (stopWorking) {  // stop requested
    		if (log != null) log.writeln( Log.WARNING, "Reader Thread " + getName() + ": Someone has requested the thread to stop!" );
		}
		
		if (log != null) log.writeln( Log.INFORMATION, "Reader Thread " + getName() + ": closing ..." );
		if (nNumFrames > 0) {
	    	if (log != null) log.writeln( Log.INFORMATION, "Reader Thread " + getName() + ": Read Total " + nNumFrames + " frames" );
			if (log != null) log.writeln( Log.INFORMATION, "Reader Thread " + getName() + ": CICLE length " +
					"- max " + Math.round(maxCicle/1000F) + "us " +
					"- min " + Math.round(minCicle/1000F) + "us " +
					"- avg " + Math.round(sumCicle / nNumFrames /1000F) + "us" );

		}
		
	}

}
