package com.harana.quickbooks.ws;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public class PropertiesManager {
	Properties properties;
	static PropertiesManager instance;
	
	PropertiesManager(){
		loadProperties();
	}
	void loadProperties(){
		 try{
			 	properties = new Properties();
		        properties.load(PropertiesManager.class.getClassLoader().getResourceAsStream("haranaqbconnector.properties"));
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
	}
	
	public static String getProperty(String key){
		if (PropertiesManager.instance==null){
			PropertiesManager.instance = new PropertiesManager();
		}
		return PropertiesManager.instance.properties.getProperty(key);
	}
	
	public static Enumeration<Object> getPropertiesKeys(){
		return PropertiesManager.instance.properties.keys();
	}

}
