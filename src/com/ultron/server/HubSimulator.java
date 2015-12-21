/**
 * Main Class for simulating the Hub, reading a log file already present
 * The main method receive:
 * 
 * The main method of the class:
 * - creates a queue
 * - connects a socket to the hub
 * - creates a reader (HubReader) passing to it a DataInputStream and a queue
 * - creates a writer () passing to it a DataOutputStream and a queue
 * 
 * Other methods allows sending commands to the hub:
 * - resetting RTS of satellites
 * - activating satellites
 * - letting satellites start sending frames
 * - ... 
 */
package com.ultron.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;

import com.ultron.general.CommandData;
import com.ultron.general.Log;
import com.ultron.general.SensorData;


/**************************
 **************** 
 * HubSimulatorDaemon - daemon
 ****************
 **************************
 */
class HubSimulator {
	
	private MyProperties			prop	= null; // command line properties
	private ArrayList<SensorData>	list	= new ArrayList<SensorData>(); // sensor data list
	private Log						log		= null; // log file
	
	private long					startTime;
	
	public HubSimulator( String p_PropertyFile, long p_startTime ) throws Exception {
		startTime = p_startTime;
		
		// read property file
		prop = new MyProperties( p_PropertyFile );

		// open the application log file
		log = new Log( prop.LOG_LEVEL, prop.LOG_FILE );

		log.writeln( Log.NONE , "\n-----------------------------------------------------------", Log.ECHO );
		log.writeln( Log.NONE , "Start application " + (new Timestamp(System.currentTimeMillis())).toString() + "\n", Log.ECHO );

		// read the file in the list in a separate thread
        (new HubSimulatorFileReaderThread(1, prop.INPUT_DUMP_FILE, list, log)).start();
	}

	public boolean AcceptConnections() {
	
        boolean listening = true;
        int i = 0;
        
        
        // accept a new connection and transfer the connection to a new thread
        try (ServerSocket serverSocket = new ServerSocket(prop.SERVER_PORT)) { 
        	
        	serverSocket.setSoTimeout( prop.SERVER_SOCKET_TIMEOUT_MS );
            log.writeln(Log.NONE, "Server process - accepting connections .....", Log.ECHO );
            log.writeln(Log.NONE, "Time till now (ms) ....." + ( System.nanoTime() / 1000000L - startTime), Log.ECHO );
        	while (listening) {
                (new HubSimulatorThread(i++, serverSocket.accept(), list, prop, log)).start();
            }
        
        } catch (SocketTimeoutException e) {
        	
        	log.writeln(Log.ERROR, "Accept connection Timeout", Log.ECHO);

        } catch (IOException e) {
        	log.writeln(Log.ERROR, "Could not listen on port " + prop.SERVER_PORT, Log.ECHO);
        	e.printStackTrace( log.getOutputStream() );
            System.exit(-1);
        
        }

        log.writeln(Log.NONE, "Server process - closing .....", Log.ECHO );
        return true;
	}
	
    public static void main(String[] args) throws IOException {
   	 
    	long startTime = System.nanoTime() / 1000000L;
    	
    	// check usage
    	if (args.length != 1) {
	        System.err.println("Correct Usage: java HubSimulator <propertyFile.xml>");
	        System.exit(1);
	    }
 
    	// declare new daemon class
	    HubSimulator hubSimul = null;
	    
	    //initialize new daemon
		try {
			hubSimul = new HubSimulator( args[0], startTime );
		} catch (Exception e) {
	        System.err.println("Cannot instantiate daemon");
	        System.exit(1);
		}
	    
		// main loop accepting connections
	    hubSimul.AcceptConnections();

    }
}

/**************************
 **************** 
 * HubSimulatorThread - manage a connection
 ****************
 **************************
 */
class HubSimulatorThread extends Thread {

    private Socket					socket	= null;
    private ArrayList<SensorData>	list	= null;
    private Log						log		= null;
    private MyProperties			prop	= null;
 
    public HubSimulatorThread(int i, Socket p_socket, ArrayList<SensorData> p_list, MyProperties p_prop, Log p_log) {
        super("HubSimulatorThread - " + Integer.toString(i));
        socket = p_socket;
        list = p_list;
        prop = p_prop;
        log = p_log;
        log.writeln(Log.INFORMATION, this.getName() + " - Accepted connection from " + socket.getInetAddress(), Log.ECHO );
   }
	
	@Override
    public void run() {
 
		OutputStream out;
		DataInputStream in;
		
		try {
	    	// to the client
	    	out = socket.getOutputStream();
	    	// from the client
	    	in = new DataInputStream( socket.getInputStream());
		} catch (IOException e) {
            e.printStackTrace();
	        log.writeln(Log.ERROR, this.getName() + ": IOException in reading, closing....", Log.ECHO );
	        return;
        }

        byte[]						bCommand 			= new byte[ CommandData.BYTES_SENT_TO_HUB ];
        int 						nBytesRead;
        int							sendingThreadIdx	= 0;
        boolean 					bActive;
        boolean 					bSend;
        HubSimulatorSendingThread	sendingThread		= null;
 
        // set the initial state NOT ACTIVE & SEND
        bActive				= false;
        bSend 				= true;
        sendingThread		= new HubSimulatorSendingThread(sendingThreadIdx++, out, list, prop, log);
        sendingThread.start();
        
        // accept command from client and answer and take actions accordingly
        // the server initial state is not send and not active
        while (true) {
        	
        	try  {
            	nBytesRead = in.read(bCommand, 0, CommandData.BYTES_SENT_TO_HUB);
           	} catch (IOException e) {
    	        if (!socket.isConnected()) {
    	        	sendingThread.stopSending();
    	        	bSend = false;
    	        	break;
    	        }
                e.printStackTrace();
    	        log.writeln(Log.ERROR, this.getName() + ": IOException in reading, closing....", Log.ECHO );
    	        continue;
            }
            
        	if (nBytesRead < 0) { 
	        	sendingThread.stopSending();
	        	bSend = false;
        		break;
           	}
        	            	
            log.writeln(Log.INFORMATION, this.getName() + " - " + Integer.toString(nBytesRead) + " bytes read", Log.ECHO);
            log.writeln(Log.INFORMATION, this.getName() + " - command received:" + Arrays.toString(bCommand), Log.ECHO);
            
            CommandData command = new CommandData( bCommand, log );
            
            /*
             *  check command
             */
            // Activate
            if (command.wifiIsACTIVE() && ! bActive) {
            	/* start getting sensors data...
            	 * but this is a simulator we don't have any sensor
            	 * and we simulate them reading dump files
            	 */
            	// create a new thread for sending data
                log.writeln(Log.INFORMATION, this.getName() + " - Now is Active", Log.ECHO);
            	bActive = true;
            } 
            // Deactivate
            if (!command.wifiIsACTIVE() && bActive) {
            	/* start getting sensors data...
            	 * but this is a simulator we don't have any sensor
            	 * and we simulate them reading dump files
            	 */
            	// stop the sending thread
                log.writeln(Log.INFORMATION, this.getName() + " - Now is NOT Active", Log.ECHO);
            	bActive = false;
            } 
            // start sending
            if (!command.wifiIsSetNOTSEND() && !bSend) {
                log.writeln(Log.INFORMATION, this.getName() + " - Now is Sending", Log.ECHO);
            	sendingThread = new HubSimulatorSendingThread(sendingThreadIdx++, out, list, prop, log);
            	bSend = true;
            }
            // stop sending
            if (command.wifiIsSetNOTSEND() && bSend) {
                log.writeln(Log.INFORMATION, this.getName() + " - Now is NOT Sending", Log.ECHO);
            	sendingThread.stopSending();
            	bSend = false;
            }
            // set RTC
            if (command.wifiIsSetRTC()) {
                log.writeln(Log.INFORMATION, this.getName() + " - Now will set RTC", Log.ECHO);
            }
            // set satellites
            if (command.wifiIsSetSAT()) {
                log.writeln(Log.INFORMATION, this.getName() + " - Now will set Satellite list", Log.ECHO);
            }
            
        }
        log.writeln(Log.INFORMATION, this.getName() + ": EOF, closing....", Log.ECHO);
        if (bSend) {
            log.writeln(Log.INFORMATION, this.getName() + ": waiting for sending thread to terminate....", Log.ECHO);
        	try {
				sendingThread.join(1000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            log.writeln(Log.INFORMATION, this.getName() + ": sending thread to terminated....", Log.ECHO);
        }
        
     try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
	        log.writeln(Log.ERROR, this.getName() + ": IOException in closing, closing....", Log.ECHO );
        }

    }
}

/**************************
 **************** 
 * HubSimulatorSendingThread - send frames
 ****************
 **************************
 */
class HubSimulatorSendingThread extends Thread {
	
	private OutputStream out;
	private ArrayList<SensorData> list;
	private Log log;
	private boolean bStop = false;
	private MyProperties prop = null;
	
	public HubSimulatorSendingThread(int i, OutputStream p_out, ArrayList<SensorData> p_list, MyProperties p_prop, Log p_log ) {
        super("HubSimulatorSendingThread - " + Integer.toString(i));
        out = p_out;
        list = p_list;
        prop = p_prop;
        log = p_log;
        log.writeln(Log.INFORMATION, this.getName() + " - start sending frames", Log.ECHO );
	}
	
	public void stopSending() {
		bStop = true;
	}
	
	@Override
    public void run() {
		
		int i = 0;
		long t1;
		long t2;
		
		// wait for the list to be non-empty
		while (list.isEmpty()) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) { }
		}

		// main loop
		t1 = System.currentTimeMillis();
		while (!bStop) {
			// get the next frame
			SensorData frame = list.get(i);
			// send it
			try {
				out.write( frame.toByteArray() );

				// go to the next index
				i = ++i % list.size();
				
			} catch (IOException e) {
				if (bStop) {
					continue;
				}
				e.printStackTrace();
				log.writeln( Log.ERROR, this.getName() + ": IOException in sending", Log.ECHO );
			}
			
			// wait
			t2 = System.currentTimeMillis();
			try {
				Thread.sleep(((t2-t1) > prop.SAMPLE_FREQUENCY_MS ? 0 : prop.SAMPLE_FREQUENCY_MS - (t2-t1)));
			} catch (InterruptedException e) {
				log.writeln( Log.WARNING, this.getName() + ": InterruptedException", Log.ECHO );
			}
			t1 = System.currentTimeMillis();
		}
	}
}

