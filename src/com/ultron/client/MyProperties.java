package com.ultron.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ultron.general.Common;

class MyProperties {

	public String 	LOG_LEVEL;
	public String 	LOG_FILE;
	public String 	MAGNETOMETER;
	public String 	HUB_IP;
	public String 	HUB_PORT;
	public String[]	SATELLITES_LIST_ARRAY;
	public String 	DUMP_FILE;

	public MyProperties( ) {		
	}
	
	public MyProperties( String p_propFile ) throws Exception {

		String 	SATELLITES_LIST;
		Properties prop = new Properties();
		
		// open and read property file
		try {

			prop.loadFromXML(new FileInputStream(p_propFile));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Error in opening propertis file: file not found!");
            throw e;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error in opening propertis file!");
			throw e;
		} 
		
		LOG_LEVEL = prop.getProperty("LOG_LEVEL");
		if (LOG_LEVEL.equals( "NONE" ))			LOG_LEVEL = "0";
		if (LOG_LEVEL.equals( "ERROR" ))		LOG_LEVEL = "1";
		if (LOG_LEVEL.equals( "WARNING" ))		LOG_LEVEL = "2";
		if (LOG_LEVEL.equals( "INFORMATION" ))	LOG_LEVEL = "3";
		if (LOG_LEVEL.equals( "DEBUG" ))		LOG_LEVEL = "4";
		if ( !LOG_LEVEL.equals("0") && !LOG_LEVEL.equals("1") && !LOG_LEVEL.equals("2") && !LOG_LEVEL.equals("3") && !LOG_LEVEL.equals("4") && 
				!LOG_LEVEL.equals("NONE") && !LOG_LEVEL.equals("ERROR") && !LOG_LEVEL.equals("WARNING") && !LOG_LEVEL.equals("INFORMATION") && !LOG_LEVEL.equals("DEBUG") ) {
			LOG_LEVEL = Integer.toString(Common.DEFAULT_LOG_LEVEL);
		}
		LOG_FILE = prop.getProperty("LOG_FILE");
		MAGNETOMETER = prop.getProperty("MAGNETOMETER");
		HUB_IP = prop.getProperty("HUB_IP");
		HUB_PORT = prop.getProperty("HUB_PORT");
		SATELLITES_LIST = prop.getProperty( "SATELLITES_LIST" );
		SATELLITES_LIST_ARRAY = SATELLITES_LIST.split("\\s*,\\s*");		
		DUMP_FILE = prop.getProperty("DUMP_FILE");
	}

	public boolean openAlePropertyFile ( String p_str ) {
		
	    String OUTFILENAME = null;
	    String OUTPATH = null;

	    //Get Document Builder
		DocumentBuilder db = null;
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error in opening propertis file!");
			return false;
		}

	    //Build Document
	    Document document = null;
		try {
			document = db.parse(new File( p_str ));
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			System.err.println("Error in parsing propertis file!");
			return false;
		}
	    
	   //Normalize the XML Structure; It's just too important !!
	   document.getDocumentElement().normalize();
	    
	   //Get all OPTIONS
	   NodeList nList = document.getElementsByTagName("option");
	    
	   for (int temp = 0; temp < nList.getLength(); temp++)   {
		    Node node = nList.item(temp);
		    System.out.println("");    //Just a separator
		    if (node.getNodeType() == Node.ELEMENT_NODE)    {
		       Element eElement = (Element) node;
		       String sNodeName = eElement.getAttribute("name");
		       String sNodeValue = eElement.getElementsByTagName("value").item(0).getTextContent();

		       //PROC_FOLDER
		       //IDPROC
		       if (sNodeName.equals("OUTPATH"))	{
		    	   OUTPATH = sNodeValue;
		    	   if ( !OUTPATH.endsWith("\\") && !OUTPATH.endsWith("/") ) OUTPATH.concat("/");
		       }
		       if (sNodeName.equals("OUTFILENAME"))	{
		    	   OUTFILENAME = sNodeValue;
		    	   while ( OUTFILENAME.startsWith("\\") || OUTFILENAME.startsWith("/") ) OUTFILENAME.substring(1);
		       }
		       if (sNodeName.equals("HUB_IP"))				HUB_IP = sNodeValue;
		       if (sNodeName.equals("HUB_PORT"))			HUB_PORT = sNodeValue;
		       if (sNodeName.equals("LOG_LEVEL")) {
		    	   LOG_LEVEL = sNodeValue;
		    	   if (LOG_LEVEL.equals( "NONE" ))			LOG_LEVEL = "0";
		    	   if (LOG_LEVEL.equals( "ERROR" ))			LOG_LEVEL = "1";
		    	   if (LOG_LEVEL.equals( "WARNING" ))		LOG_LEVEL = "2";
		    	   if (LOG_LEVEL.equals( "INFORMATION" ))	LOG_LEVEL = "3";
		    	   if (LOG_LEVEL.equals( "DEBUG" ))			LOG_LEVEL = "4";
		    	   if ( !LOG_LEVEL.equals("0") && 
		    			   !LOG_LEVEL.equals("1") && 
		    			   !LOG_LEVEL.equals("2") && 
		    			   !LOG_LEVEL.equals("3") && 
		    			   !LOG_LEVEL.equals("4")  ) {
		    	   	LOG_LEVEL = Integer.toString(Common.DEFAULT_LOG_LEVEL);
		    	   }
		       }
		       if (sNodeName.equals("LOG_FILE")) 			LOG_FILE = sNodeValue;
		       if (sNodeName.equals("SATELLITES_LIST"))		SATELLITES_LIST_ARRAY = sNodeValue.split("\\s*,\\s*");		
		       if (sNodeName.equals("MAGNETOMETER"))		MAGNETOMETER = sNodeValue;

		    }
	   }
	   
	   DUMP_FILE = OUTPATH + OUTFILENAME;
	   
	   return true;
		
	}
}