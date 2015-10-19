/**
 * Reader Class: 
 * it receives:
 * - an input stream from the HUB (it can be obtained with socket.getInputStream() where socket is
 * 		an already connected socket to the HUB 
 * - a queue where it place the frames read and
 * TODO: - some way to receive command
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
	//private boolean							bUseMag		= false;// false -> NOT read mag data
																// true -> read mag data
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
	 * stop the thread.
	 */
	public void stopReading() {
		stopWorking = true;
	}
	
	/*
	 * Execution method. Here the class start reading from the hub.
	 * 
	 */
	@Override
    public void run() {
		
		int		nBytesRead;
		int		nNumFrames = 0;
    	byte[]	bFrame = new byte[ nFrameSize ];
    	long	startRead, lenghtRead, maxRead = 0, minRead = Long.MAX_VALUE, sumRead = 0;

		if (log != null) log.write( Log.INFORMATION, "Reader Thread: going to read " + nFrameSize + " bytes per frame" );

        // main loop for reading frames from the hub
		try {
			
			startRead = System.currentTimeMillis();
			
			// wait until a frame is available
	        while (!stopWorking & (nBytesRead = inStream.read(bFrame, 0, nFrameSize)) >= 0) {
	        	
	        	// performance calc
	        	lenghtRead = System.currentTimeMillis() - startRead;
	        	sumRead += lenghtRead;
	        	if (lenghtRead > maxRead) maxRead = lenghtRead;
	        	if (lenghtRead < minRead) minRead = lenghtRead;
	        	
	        	// count num of frames
	        	nNumFrames++;
	        	
	        	if (log != null) log.write( Log.INFORMATION, "Reader Thread: frame " + nNumFrames + " - bytes read: " + nBytesRead + " in " + lenghtRead + "ms" );
		        	
	        	// put every frame into the queue
	        	queue.add( bFrame );
	        	
	        	// re-init the array
	        	Arrays.fill( bFrame, (byte)0 );
	        	
	        	// re-start the clock
				startRead = System.currentTimeMillis();

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
			if (log != null) log.write( Log.INFORMATION, "Reader Thread: max READ length " + maxRead );
			if (log != null) log.write( Log.INFORMATION, "Reader Thread: min READ length " + minRead );
			if (log != null) log.write( Log.INFORMATION, "Reader Thread: avg READ length " + (sumRead / nNumFrames) );
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
