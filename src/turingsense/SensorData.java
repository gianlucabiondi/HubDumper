package turingsense;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Scanner;
import java.io.PrintWriter;
import java.util.regex.Pattern;


/**
 * @author gianluca
 *
 */

class SingleSensorData {
	

	/* 
	 * class variables: single sensor message components
	 */
	int   satelliteID;      // Unique satellite's ID

	short accel_X; 			// Accelerometer's: X coordinate 
	short accel_Y; 			// Accelerometer's: Y coordinate 
	short accel_Z; 			// Accelerometer's: Z coordinate 

	short gyro_X;  			// Gyroscope's: X coordinate 
	short gyro_Y;  			// Gyroscope's: Y coordinate 
	short gyro_Z;  			// Gyroscope's: Z coordinate 

	short mag_X;  			// Magnetometer's: X coordinate 
	short mag_Y;  			// Magnetometer's: Y coordinate 
	short mag_Z;  			// Magnetometer's: Z coordinate 

	// here in C struct sensor_record_t there are 2 bytes of structure alignment
	
	float quat_W;  			// Quarternion's: rotation degree around the vector
	float quat_X;  			// Quarternion's: X coordinate 
	float quat_Y;  			// Quarternion's: Y coordinate 
	float quat_Z;  			// Quarternion's: Z coordinate 

	// constructors
	public SingleSensorData( DataInputStream in ) throws IOException {

		satelliteID	= in.readInt();

		accel_X 	= in.readShort();
		accel_Y 	= in.readShort();
		accel_Z 	= in.readShort();

		gyro_X 		= in.readShort();
		gyro_Y 		= in.readShort();
		gyro_Z 		= in.readShort();

		// TODO: read also Magnetometer's coordinate;
//		mag_X 		= in.readShort();
//		mag_Y 		= in.readShort();
//		mag_Z 		= in.readShort();
		
		quat_W 		= in.readFloat();
		quat_X 		= in.readFloat();
		quat_Y 		= in.readFloat();
		quat_Z 		= in.readFloat();

		
	}

	public SingleSensorData( Scanner inScanner ) {

		satelliteID	= inScanner.nextBigInteger().intValue();

		accel_X 	= inScanner.nextShort();
		accel_Y 	= inScanner.nextShort();
		accel_Z 	= inScanner.nextShort();

		gyro_X 		= inScanner.nextShort();
		gyro_Y 		= inScanner.nextShort();
		gyro_Z 		= inScanner.nextShort();

		// TODO: read also Magnetometer's coordinate;
//		mag_X 		= inScanner.nextShort();
//		mag_Y 		= inScanner.nextShort();
//		mag_Z 		= inScanner.nextShort();
		
		quat_W 		= nextFloat(inScanner);
		quat_X 		= nextFloat(inScanner);
		quat_Y 		= nextFloat(inScanner);
		quat_Z 		= nextFloat(inScanner);
		
	}

	// methods 
	private static final Pattern nan = Pattern.compile( "nan", Pattern.CASE_INSENSITIVE );
	private static float nextFloat( Scanner inScanner ) {
	    if ( inScanner.hasNext(nan) ) {
	    	inScanner.next();
	        return Float.NaN;
	    }
	    return inScanner.nextFloat();
	}	
	
	public void send( PrintWriter out ) {
		// sends Sensor Data through the PrintWriter
		out.print( satelliteID );
		out.print( accel_X );
		out.print( accel_Y );
		out.print( accel_Z );
		out.print( gyro_X );
		out.print( gyro_Y );
		out.print( gyro_Z );
		out.print( quat_W );
		out.print( quat_X );
		out.print( quat_Y );
		out.print( quat_Z );
				
	}
	
	@Override
	public String toString() {
		String ret = new String( 
				
				Integer.toString( satelliteID ) + " " +

				Short.toString(accel_X) + " " +
				Short.toString(accel_Y) + " " +
				Short.toString(accel_Z) + " " +

				Short.toString(gyro_X) + " " +
				Short.toString(gyro_Y) + " " +
				Short.toString(gyro_Z) + " " +

				Float.toString(quat_W) + " " +
				Float.toString(quat_X) + " " +
				Float.toString(quat_Y) + " " +
				Float.toString(quat_Z)
				
				);
		
		return ret;
	}

}

/* 
 * Data section of messages with msgType = 'sensorData'
 */
public class SensorData {
	
	final static int BYTES_READ_FROM_HUB_NOMAG		= 368;
	final static int BYTES_READ_FROM_HUB_MAG 		= 456;
	final static int BYTES_READ_FROM_HUB_DEFAULT	= BYTES_READ_FROM_HUB_MAG;

	/* 
	 * class variables: message components
	 */
	int 				hubID;      			// Unique hub's ID
	byte 				frameType;  			// 0 - 10-sensor right handed 
												// 1 - 10-sensor left handed
												// 2 - 5-sensor upper body
												// 3 - 5-sensor lower body
	int					arraySize;				// number of sensors
	int 				timestamp;  			// offset timestamp
	int 				bitmap;     			// bitmap indicating sensor 
	SingleSensorData[] 	objSingleSensorData;	// single sensor data (repeated "arraySize" times)
	
	// constructors
	public SensorData( DataInputStream in ) throws IOException {

		// read header data from DataInputStream
		hubID 		= in.readInt();
		frameType 	= in.readByte();
		// TODO: uncomment subsequent read
		//arraySize   = in.readInt();
		arraySize   = 10;
		timestamp 	= in.readInt();
		bitmap 		= in.readInt();
		
		// sensors data array
		objSingleSensorData = new SingleSensorData[ arraySize ];
		
		// read each sensor data
		for ( int i = 0; i < arraySize ; i++ ) {
			objSingleSensorData[i] = new SingleSensorData( in );
		}
		
	}

	public SensorData( Scanner inScanner ) {
		
			
			// read header data from DataInputStream
			hubID 		= inScanner.nextInt();
			frameType 	= inScanner.nextByte();
			// TODO: uncomment subsequent read
			//arraySize   = inScanner.nextInt();
			arraySize   = 10;
			timestamp 	= inScanner.nextInt();
			bitmap 		= inScanner.nextInt();
			
			// sensors data array
			objSingleSensorData = new SingleSensorData[ arraySize ];
			
			// read each sensor data
			for ( int i = 0; i < arraySize ; i++ ) {
				objSingleSensorData[i] = new SingleSensorData( inScanner );
			}
				
	}

	// methods 
	public void send( PrintWriter out ) {
		// sends Sensor Data through the PrintWriter
		out.print( hubID );
		out.print( frameType );
		// TODO: uncomment subsequent write
		//out.print( arraySize );
		out.print( timestamp );
		out.print( bitmap );
		// read each sensor data
		for ( int i = 0; i < arraySize ; i++ ) {
			objSingleSensorData[i].send( out );
		}
		// sends the data
		out.println();
				
	}
	
	@Override
	public String toString() {
		String ret = new String( 
				
				Integer.toString( hubID ) + " " +
				Integer.toString( frameType ) + " " +
				Integer.toString( timestamp ) + " " +
				Integer.toString( bitmap )
				
				);

		for ( int i = 0; i < arraySize ; i++ ) {
			ret += (" " + objSingleSensorData[i].toString());
		}

		return ret;
	}
	
}

