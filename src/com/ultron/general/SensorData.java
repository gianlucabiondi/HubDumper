package com.ultron.general;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * @author gianluca
 *
 * Data section of messages with msgType = 'sensorData'
 */
public class SensorData {
	
	public final static int BYTES_HEADER_ONLY	= 
			Integer.BYTES + 										// hubID
			Integer.BYTES + 										// frameType
			Integer.BYTES + 										// timestamp
			Integer.BYTES;											// bitmap

	public final static int BYTES_WITH_MAG	= 
			BYTES_HEADER_ONLY +										// header
			SingleSensorData.BYTES_WITH_MAG * Common.MAX_SENSORS;	// satellite_ids[ MAX_SAT_SENSORS ]

	public final static int BYTES_WITHOUT_MAG = 
			BYTES_HEADER_ONLY +										// header
			SingleSensorData.BYTES_WITHOUT_MAG * Common.MAX_SENSORS;// satellite_ids[ MAX_SAT_SENSORS ]

	public final static int ELEMENTS_WITH_MAG = (4 + (14 * Common.MAX_SENSORS));
	public final static int ELEMENTS_WITHOUT_MAG = (4 + (10 * Common.MAX_SENSORS));
	
	/* 
	 * class variables: message components
	 */
	private int 				hubID;      			//  0 - Unique hub's ID
	private byte				frameType;  			//  4 - 0 - 10-sensor right handed 
														//      1 - 10-sensor left handed
														//      2 - 5-sensor upper body
														//      3 - 5-sensor lower body
	// ?????
	private byte  				unused_1;
	private byte  				unused_2;
	private byte  				unused_3;
	
	private int 				timestamp;  			//  8 - offset timestamp
	private int 				bitmap;     			// 12 - bitmap indicating sensor 
	private SingleSensorData[] 	objSingleSensorData;	// 16 - single sensor data (repeated "arraySize" times)
	
	/* 
	 * class variables: others
	 */
	private int					idx;					// freame number
	private boolean 			bUseMag;				// with or without magnetometer

	// constructors
	public SensorData( byte[] p_frame, boolean p_bUseMag, Log p_log, int p_idx ) {
		
		bUseMag = p_bUseMag;
		idx = p_idx;
		
		int sensorDataLength = (p_bUseMag ? SingleSensorData.BYTES_WITH_MAG : SingleSensorData.BYTES_WITHOUT_MAG);
		
		if (p_log != null) p_log.writeln(Log.DEBUG, "-->Sensor data " + Arrays.toString(p_frame));

		ByteBuffer buf = ByteBuffer.wrap( p_frame );
		
		// Hub speaks LITTLE_ENDIAN "language"
		buf.order( ByteOrder.LITTLE_ENDIAN );

    	// translate the frame from byte
		hubID 		= buf.getInt();
		frameType	= buf.get();
		unused_1	= buf.get();
		unused_2	= buf.get();
		unused_3	= buf.get();
		timestamp	= buf.getInt();
		bitmap		= buf.getInt();
		
		objSingleSensorData = new SingleSensorData[Common.MAX_SENSORS];
		
		for (int i = 0; i < Common.MAX_SENSORS; i++ ) { 
			int start = Integer.BYTES +
						Integer.BYTES +
						Integer.BYTES +
						Integer.BYTES + 
						(i * sensorDataLength);
			int stop = start + sensorDataLength;
			boolean bActive = ((bitmap & (1 << i)) != 0);
			
			objSingleSensorData[i] = new SingleSensorData( i, Arrays.copyOfRange( p_frame, start, stop), p_bUseMag, bActive, p_log );
		}
		
	}

	public SensorData( byte[] p_frame, boolean p_bUseMag, Log p_log ) {
		this( p_frame, p_bUseMag, p_log, -1 );
	}
	
	public SensorData( byte[] p_frame, boolean p_bUseMag ) {
		this( p_frame, p_bUseMag, null );
	}

	public SensorData( String p_line, int p_idx ) {
		
		bUseMag = (p_line.split("\\s+").length == ELEMENTS_WITH_MAG);
		idx = p_idx;

    	// tokenize the line just read into a SensorData object
    	Scanner scanner = new Scanner( p_line );
    	scanner.useLocale( Locale.ENGLISH );
    	
		// read header data from DataInputStream
		hubID 		= scanner.nextInt();
		frameType 	= scanner.nextByte();
		timestamp 	= scanner.nextInt();
		bitmap 		= scanner.nextInt();
		
		// sensors data array
		objSingleSensorData = new SingleSensorData[ Common.MAX_SENSORS ];
		
		// read each sensor data
		for ( int i = 0; i < Common.MAX_SENSORS ; i++ ) {
			boolean bActive = ((bitmap & (1 << i)) != 0);
			objSingleSensorData[i] = new SingleSensorData( i, scanner, bUseMag, bActive );
		}
    	
    	// close the scanner
    	scanner.close();

	}
	
	// methods
	public String toString(boolean p_sobstituteTimestamp) {
		String ret = new String( 
				
				Integer.toString( hubID ) + Common.FIELD_SEPARATOR +
				Byte.toString( frameType ) + Common.FIELD_SEPARATOR +
				(p_sobstituteTimestamp ? idx : Integer.toString( timestamp )) + Common.FIELD_SEPARATOR +
				Integer.toString( bitmap )
				
				);

		for ( int i = 0; i < Common.MAX_SENSORS ; i++ ) {
			ret += (Common.FIELD_SEPARATOR + objSingleSensorData[i].toString());
		}

		return ret;
	}
	
	@Override
	public String toString() {
		return toString( false );
	}
	
	public byte[] toByteArray(boolean p_sobstituteTimestamp) {

		ByteBuffer buf = ByteBuffer.allocate( bUseMag ? BYTES_WITH_MAG : BYTES_WITHOUT_MAG );
		
		buf.order( ByteOrder.LITTLE_ENDIAN );
		
		buf.putInt(hubID);
		buf.put(frameType);
		buf.put(unused_1);
		buf.put(unused_2);
		buf.put(unused_3);
		if (p_sobstituteTimestamp) { 
			buf.putInt(idx); 
		} else { 
			buf.putInt(timestamp); 
		}
		buf.putInt(bitmap);
		
		for (int i = 0; i < Common.MAX_SENSORS; i++ ) {
			buf.put(objSingleSensorData[i].toByteArray());
		}
		
		return buf.array();

	}
	
	public byte[] toByteArray() {
		return toByteArray( false );
	}
	
	public boolean isSatelliteValid (int p_satIdx) { 
		if ((p_satIdx >= 0) && (p_satIdx < Common.MAX_SENSORS)) return objSingleSensorData[p_satIdx].isSatelliteValid();
		return false; 
	}

}

/**
 * 
 * Single sensor data structure
 *
 */
class SingleSensorData {
	
	final static int BYTES_WITH_MAG		= 
			Integer.BYTES + 		// satelliteID
			Short.BYTES * 3 + 		// accel_X_Y_Z
			Short.BYTES * 3 + 		// gyro_X_Y_Z
			Short.BYTES * 3 + 		// mag_X_Y_Z
			Byte.BYTES * 2 + 		// unused
			Float.BYTES * 4; 		// quaternion

	final static int BYTES_WITHOUT_MAG	= 
			Integer.BYTES + 		// satelliteID
			Short.BYTES * 3 + 		// accel_X_Y_Z
			Short.BYTES * 3 + 		// gyro_X_Y_Z
			Float.BYTES * 4; 		// quaternion

	private boolean bUseMag;
	private boolean bSatActive;

	/* 
	 * class variables: single sensor message components
	 */
	private int   satelliteID;      // Unique satellite's ID

	private short accel_X; 			// Accelerometer's: X coordinate 
	private short accel_Y; 			// Accelerometer's: Y coordinate 
	private short accel_Z; 			// Accelerometer's: Z coordinate 

	private short gyro_X;  			// Gyroscope's: X coordinate 
	private short gyro_Y;  			// Gyroscope's: Y coordinate 
	private short gyro_Z;  			// Gyroscope's: Z coordinate 

	private short mag_X;  			// Magnetometer's: X coordinate 
	private short mag_Y;  			// Magnetometer's: Y coordinate 
	private short mag_Z;  			// Magnetometer's: Z coordinate 

	// here in C struct sensor_record_t there are 2 bytes of structure alignment
	private byte  unused_1 = 0;
	private byte  unused_2 = 0;
	
	private float quat_W;  			// Quarternion's: rotation degree around the vector
	private float quat_X;  			// Quarternion's: X coordinate 
	private float quat_Y;  			// Quarternion's: Y coordinate 
	private float quat_Z;  			// Quarternion's: Z coordinate 

	// constructors
	public SingleSensorData( int idx, byte[] p_frame, boolean p_useMag, boolean p_satActive, Log p_log ) {

		ByteBuffer buf = ByteBuffer.wrap( p_frame );
		
		// Hub speaks LITTLE_ENDIAN "language"
		buf.order( ByteOrder.LITTLE_ENDIAN );

		if (p_log != null) p_log.writeln(Log.DEBUG, "-->Single Sensor data (" + Boolean.toString(p_satActive) + ") " + Arrays.toString(p_frame));
		
		bUseMag 	= p_useMag;
		bSatActive	= p_satActive;
		
		satelliteID	= buf.getInt();
		if (!bSatActive) satelliteID = idx;

		accel_X 	= buf.getShort();
		accel_Y 	= buf.getShort();
		accel_Z 	= buf.getShort();

		gyro_X 		= buf.getShort();
		gyro_Y 		= buf.getShort();
		gyro_Z 		= buf.getShort();

		if (p_useMag) {
			mag_X 		= buf.getShort();
			mag_Y 		= buf.getShort();
			mag_Z 		= buf.getShort();
			unused_1	= buf.get(); // alignment
			unused_2	= buf.get(); // alignment
		}
		
		quat_W 		= buf.getFloat();
		quat_X 		= buf.getFloat();
		quat_Y 		= buf.getFloat();
		quat_Z 		= buf.getFloat();

	}

	public SingleSensorData( int idx, byte[] p_frame, boolean p_useMag, boolean p_satActive ) {
		this( idx, p_frame, p_useMag, p_satActive, null );
	}

	public SingleSensorData( int idx, Scanner p_inScanner, boolean p_useMag, boolean p_satActive ) {

		bUseMag 	= p_useMag;
		bSatActive	= p_satActive;

		satelliteID	= p_inScanner.nextBigInteger().intValue();

		accel_X 	= p_inScanner.nextShort();
		accel_Y 	= p_inScanner.nextShort();
		accel_Z 	= p_inScanner.nextShort();

		gyro_X 		= p_inScanner.nextShort();
		gyro_Y 		= p_inScanner.nextShort();
		gyro_Z 		= p_inScanner.nextShort();

		if (p_useMag) {
			mag_X 		= p_inScanner.nextShort();
			mag_Y 		= p_inScanner.nextShort();
			mag_Z 		= p_inScanner.nextShort();
		}
		
		quat_W 		= nextFloat(p_inScanner);
		quat_X 		= nextFloat(p_inScanner);
		quat_Y 		= nextFloat(p_inScanner);
		quat_Z 		= nextFloat(p_inScanner);
		
	}

	// methods 

	@Override
	public String toString() {
		String ret;
		
		if (bSatActive) {
			
			ret = new String( 
					
					Integer.toString( satelliteID ) + Common.FIELD_SEPARATOR +

					Short.toString(accel_X) + Common.FIELD_SEPARATOR +
					Short.toString(accel_Y) + Common.FIELD_SEPARATOR +
					Short.toString(accel_Z) + Common.FIELD_SEPARATOR +

					Short.toString(gyro_X) + Common.FIELD_SEPARATOR +
					Short.toString(gyro_Y) + Common.FIELD_SEPARATOR +
					Short.toString(gyro_Z) + Common.FIELD_SEPARATOR +
					
					(bUseMag ? Short.toString(mag_X) + Common.FIELD_SEPARATOR: "") +
					(bUseMag ? Short.toString(mag_Y) + Common.FIELD_SEPARATOR: "") +
					(bUseMag ? Short.toString(mag_Z) + Common.FIELD_SEPARATOR: "") +

					Float.toString(quat_W) + Common.FIELD_SEPARATOR +
					Float.toString(quat_X) + Common.FIELD_SEPARATOR +
					Float.toString(quat_Y) + Common.FIELD_SEPARATOR +
					Float.toString(quat_Z)
					
					);
		} else {
			
			ret = new String( 
					
					Integer.toString( satelliteID ) + Common.FIELD_SEPARATOR +

					"0" + Common.FIELD_SEPARATOR +
					"0" + Common.FIELD_SEPARATOR +
					"0" + Common.FIELD_SEPARATOR +

					"0" + Common.FIELD_SEPARATOR +
					"0" + Common.FIELD_SEPARATOR +
					"0" + Common.FIELD_SEPARATOR +
					
					(bUseMag ? "0" + Common.FIELD_SEPARATOR: "") +
					(bUseMag ? "0" + Common.FIELD_SEPARATOR: "") +
					(bUseMag ? "0" + Common.FIELD_SEPARATOR: "") +

					"0.000000000" + Common.FIELD_SEPARATOR +
					"0.000000000" + Common.FIELD_SEPARATOR +
					"0.000000000" + Common.FIELD_SEPARATOR +
					"0.000000000"
					
					);
		}
		
		return ret;
	}
	
	public byte[] toByteArray() {

		ByteBuffer buf = ByteBuffer.allocate( bUseMag ? SingleSensorData.BYTES_WITH_MAG : SingleSensorData.BYTES_WITHOUT_MAG );
		
		buf.order( ByteOrder.LITTLE_ENDIAN );
		
		buf.putInt(satelliteID);
		
		buf.putShort(accel_X);
		buf.putShort(accel_Y);
		buf.putShort(accel_Z);

		buf.putShort(gyro_X);
		buf.putShort(gyro_Y);
		buf.putShort(gyro_Z);

		if (bUseMag) {
			buf.putShort(mag_X);
			buf.putShort(mag_Y);
			buf.putShort(mag_Z);
			buf.put(unused_1);
			buf.put(unused_2);
		}
		
		buf.putFloat(quat_W);
		buf.putFloat(quat_X);
		buf.putFloat(quat_Y);
		buf.putFloat(quat_Z);

		return buf.array();
	}
	
	public boolean isSatelliteValid () { return bSatActive; }

	private static final Pattern nan = Pattern.compile( "nan", Pattern.CASE_INSENSITIVE );
	private static float nextFloat( Scanner inScanner ) {
	    if ( inScanner.hasNext(nan) ) {
	    	inScanner.next();
	        return Float.NaN;
	    }
	    return inScanner.nextFloat();
	}	
}

