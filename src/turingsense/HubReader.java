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
	private int								nFrameSize	= SensorData.BYTES_READ_FROM_HUB_DEFAULT;
	private Log								log			= null;
	private boolean							stopWorking	= false; // when set to FALSE -> the thread stops
	

	/*
	 * Constructor:
	 */
	public HubReader( DataInputStream p_in, ConcurrentLinkedQueue<byte[]> p_que ) {

		this.inStream	= p_in;
		this.queue 		= p_que;
	}
	
	public HubReader( DataInputStream p_in, ConcurrentLinkedQueue<byte[]> p_que, Log p_log ) {

		this( p_in, p_que );
		this.log		= p_log;
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
		
		int		nBytesRead = 0;
		int		nNumFrames = 0;
    	long	now;
    	long	startRead, lenghtRead, maxRead = 0, minRead = Long.MAX_VALUE, sumRead = 0;
    	long	startCicle, lenghtCicle, maxCicle = 0, minCicle = Long.MAX_VALUE, sumCicle = 0;

		if (log != null) log.write( Log.INFORMATION, "Reader Thread: going to read " + nFrameSize + " bytes per frame" );

        // main loop for reading frames from the hub
		try {
			
			now = System.nanoTime();
			startRead = now;
			startCicle = now;
			
			// wait until a frame is available
	        while (!stopWorking ) {

	        	byte[]	bFrame = new byte[ nFrameSize ];
	        	
	        	// read next buffer into the buffer
	        	if ((nBytesRead = inStream.read(bFrame, 0, nFrameSize)) < 0) {
	        		break;
	        	}
	        	
	        	// performance calc
	        	now = System.nanoTime();
	        	//
	        	lenghtRead = now - startRead;
	        	sumRead += lenghtRead;
	        	if (lenghtRead > maxRead) maxRead = lenghtRead;
	        	if (lenghtRead < minRead) minRead = lenghtRead;
	        	//
	        	lenghtCicle = now - startCicle;
	        	sumCicle += lenghtCicle;
	        	if (lenghtCicle > maxCicle) maxCicle = lenghtCicle;
	        	if (lenghtCicle < minCicle) minCicle = lenghtCicle;
	        	startCicle = now;
	        	
	        	// count num of frames
	        	nNumFrames++;
	        	
	        	if (log != null) log.write( Log.INFORMATION, "Reader Thread: frame " + nNumFrames + " - bytes read: " + 
	        												nBytesRead + " in " + Math.round(lenghtRead/1000F) + 
	        												"us ( whole cicle: " + Math.round(lenghtCicle/1000F) + "us )" );
	        	if (log != null) log.write( Log.INFORMATION, "Reader Thread:  " + Arrays.toString(bFrame) );
		        	
	        	// put every frame into the queue
	        	queue.add( bFrame );
	        	
	        	// re-start the clock
				startRead = System.nanoTime();

	        }
        	// manage EOF: exit thread
    		//inStream.close();
    		if (nBytesRead < 0) {  // EOF
        		if (log != null) log.write( Log.INFORMATION, "Reader Thread: EOF found!" );
    		}
    		if (stopWorking) {  // stop requested
        		if (log != null) log.write( Log.INFORMATION, "Reader Thread: Someone has requested the thread to stop!" );
    		}
        	if (log != null) log.write( Log.INFORMATION, "Reader Thread: Read " + nNumFrames + " frames" );
	        
		} catch (IOException e) { 

			e.printStackTrace();
			if (log != null) log.write( Log.INFORMATION, "Reader Thread: IO Exception!" );
 
		}
		
		if (log != null) log.write( Log.INFORMATION, "Reader Thread: closing ..." );
		if (nNumFrames > 0) {
			if (log != null) log.write( Log.INFORMATION, "Reader Thread: READ length - max " + Math.round(maxRead/1000F) + "us " +
					"- min " + Math.round(minRead/1000F) + "us " +
					"- avg " + Math.round(sumRead / nNumFrames /1000F) + "us" );
			if (log != null) log.write( Log.INFORMATION, "Reader Thread: CICLE length - max " + Math.round(maxCicle/1000F) + "us " +
					"- min " + Math.round(minCicle/1000F) + "us " +
					"- avg " + Math.round(sumCicle / nNumFrames /1000F) + "us" );
		}
		
	}

	/*
	 * Public Methods:
	 */
	public void setUseMagnetometer( boolean p_bUseMag ) {
		
		if ( p_bUseMag )	{ nFrameSize	= SensorData.BYTES_READ_FROM_HUB_MAG; }
		else				{ nFrameSize	= SensorData.BYTES_READ_FROM_HUB_NOMAG; }
		
	}
	
}
