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
import java.io.PrintStream;

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
	private int								nFrameSize	= SensorData.BYTES_READ_FROM_HUB_NOMAG;
	private PrintStream						log			= null;
	

	/*
	 * Constructor:
	 */
	public HubReader( DataInputStream p_in, ConcurrentLinkedQueue<byte[]> p_que ) {

		this.inStream	= p_in;
		this.queue 		= p_que;
	}
	public HubReader( DataInputStream p_in, ConcurrentLinkedQueue<byte[]> p_que, PrintStream p_log ) {

		this( p_in, p_que );
		this.log		= p_log;
	}

	/*
	 * Execution method. Here the class start reading from the hub.
	 * 
	 */
	@Override
    public void run() {
		
		int nBytesRead;
		int nNumFrames = 0;
		
        // main loop for reading frames from the hub
		try {
			
			// wait until a frame is available
	    	byte[] bFrame = new byte[ nFrameSize ];
	        while ((nBytesRead = inStream.read(bFrame, 0, nFrameSize)) > 0) {

	        	if (log != null) log.println( "Bytes read: " + nBytesRead);
	        	
	        	// count num of frames
	        	nNumFrames++;
	        	
	        	// put every frame into the queue
	        	queue.add( bFrame );
	        	
	        	// re-init the array
	        	Arrays.fill( bFrame, (byte)0 );
	        	
	        }
	        if (log != null) log.println( "Read " + nNumFrames + " frames" );
	        
		} catch (IOException e) { e.printStackTrace(); }
	
	}

	/*
	 * Public Methods:
	 */
	public void setUseMagnetometer( boolean p_bUseMag ) {
		
		if ( p_bUseMag )	{ nFrameSize	= SensorData.BYTES_READ_FROM_HUB_MAG; }
		else				{ nFrameSize	= SensorData.BYTES_READ_FROM_HUB_NOMAG; }
		
	}
	
}
