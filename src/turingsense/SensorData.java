package turingsense;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * @author gianluca
 *
 * Data section of messages with msgType = 'sensorData'
 */
public class SensorData {
	
	final static int BYTES_WITH_MAG	= 
			Integer.BYTES + 										// hubID
			Integer.BYTES + 										// frameType
			Integer.BYTES + 										// timestamp
			Integer.BYTES +											// bitmap
			SingleSensorData.BYTES_WITH_MAG * Common.MAX_SENSORS;	// satellite_ids[ MAX_SAT_SENSORS ]

	final static int BYTES_WITHOUT_MAG = 
			Integer.BYTES + 										// hubID
			Integer.BYTES + 										// frameType
			Integer.BYTES + 										// timestamp
			Integer.BYTES +											// bitmap
			SingleSensorData.BYTES_WITHOUT_MAG * Common.MAX_SENSORS;// satellite_ids[ MAX_SAT_SENSORS ]

	/* 
	 * class variables: message components
	 */
	int 				hubID;      			//  0 - Unique hub's ID
	byte				frameType;  			//  4 - 0 - 10-sensor right handed 
												//      1 - 10-sensor left handed
												//      2 - 5-sensor upper body
												//      3 - 5-sensor lower body
	// ?????
	byte  				unused_1;
	byte  				unused_2;
	byte  				unused_3;
	
	int 				timestamp;  			//  8 - offset timestamp
	int 				bitmap;     			// 12 - bitmap indicating sensor 
	SingleSensorData[] 	objSingleSensorData;	// 16 - single sensor data (repeated "arraySize" times)
	
	// constructors
	public SensorData( byte[] p_frame, boolean p_bUseMag, Log p_log ) {
		
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

	public SensorData( byte[] p_frame, boolean p_bUseMag ) {
		this( p_frame, p_bUseMag, null );
	}

	@Override
	public String toString() {
		String ret = new String( 
				
				Integer.toString( hubID ) + Common.FIELD_SEPARATOR +
				Byte.toString( frameType ) + Common.FIELD_SEPARATOR +
				Integer.toString( timestamp ) + Common.FIELD_SEPARATOR +
				Integer.toString( bitmap )
				
				);

		for ( int i = 0; i < Common.MAX_SENSORS ; i++ ) {
			ret += (Common.FIELD_SEPARATOR + objSingleSensorData[i].toString());
		}

		return ret;
	}
	
	public boolean isSatelliteValid (int idx) { 
		if ((idx >= 0) & (idx < Common.MAX_SENSORS)) return objSingleSensorData[idx].isSatelliteValid();
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
	byte  unused_1;
	byte  unused_2;
	
	float quat_W;  			// Quarternion's: rotation degree around the vector
	float quat_X;  			// Quarternion's: X coordinate 
	float quat_Y;  			// Quarternion's: Y coordinate 
	float quat_Z;  			// Quarternion's: Z coordinate 

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
	
	public boolean isSatelliteValid () { return bSatActive; }

}

