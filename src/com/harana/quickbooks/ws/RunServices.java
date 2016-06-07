package com.harana.quickbooks.ws;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.xml.ws.Endpoint;

public class RunServices {
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger LOGGER = Logger.getLogger(RunServices.class.getName());

	static{
        try {
            logManager.readConfiguration(RunServices.class.getClassLoader().getResourceAsStream("log.properties"));
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Error in loading configuration",exception);
        }
    }

	public static void main(String[] args) {
		String port = PropertiesManager.getProperty("service_port");
		String host = PropertiesManager.getProperty("service_host");
		String generalQueryServiceURL ="http://" + host + ":" + port + "/GeneralQueryRqSoapImpl";
		String synchServiceURL ="http://" + host + ":" + port + "/SyncService";
		System.out.println(System.getProperty("java.class.path"));
		Endpoint.publish(generalQueryServiceURL,
				new GeneralQueryRqSoapImpl());
		System.out.println("Web connector webservice integrator is running in " +  generalQueryServiceURL);
		
		Endpoint.publish(synchServiceURL,
				new SynchronizerService());
		System.out.println("Mobile Synchronizer Service  is running in " + synchServiceURL);
		
		CacheScheduler.scheduleAutomaticCacheFill();
		
		
	}
	
}
