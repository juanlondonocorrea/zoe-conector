package com.harana.quickbooks.ws;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jws.WebService;

/*
 * http://developer.intuit.com/qbsdk-current/doc/pdf/qbwc_proguide.pdf
 */
@WebService(endpointInterface = "com.harana.quickbooks.ws.QBWebConnectorSvcSoap")
public class GeneralQueryRqSoapImpl implements QBWebConnectorSvcSoap {

	private final static Logger LOGGER = Logger.getLogger(GeneralQueryRqSoapImpl.class.getName()); 	
	static HashMap<String,MessageInterchange> ticketsPending = new HashMap<String, MessageInterchange>();
	static {
		Thread t = new Thread(new cleaner());
        t.start();
	}
	
	@Override
	public ArrayOfString authenticate(String strUserName, String strPassword) {
		UUID uuid = UUID.randomUUID();
		LOGGER.log(Level.FINE, "Authenticate strUserName=" + strUserName + ", uuid=" + uuid.toString());
		ArrayOfString arr = new ArrayOfString();
		arr.string = new ArrayList<String>();
		arr.string.add(uuid.toString());
		arr.string.add(""); //To use the currently open company, specify an empty string
		return arr;
	}

	@Override
	public String closeConnection(String ticket) {
		LOGGER.log(Level.FINE, "closeConnection ticket=" + ticket);
		return null;
	}

	@Override
	public String connectionError(String ticket, String hresult, String message) {
		LOGGER.log(Level.FINE, "connectionError ticket="+ticket +", hresult=" + hresult + ", message=" + message);
		return null;
	}

	@Override
	public String getLastError(String ticket) {
		return null;
	}

	/**
	 * @return A positive integer less than 100 represents the percentage of work completed. A value of 1 means one percent complete, a value of 100 means 100 percent complete--there is no more work. A negative value means an error has occurred and the Web Connector responds to this with a getLastError call. The negative value could be used as a custom error code.
	 */
	@Override
	public int receiveResponseXML(String ticket, 
			String response,
			String hresult, String message) {
		LOGGER.log(Level.INFO, "receiveResponseXML ticket="+ticket +", hresult=" + hresult + ", message=" + message);
		LOGGER.log(Level.FINE, "receiveResponseXML ticket="+ticket +", hresult=" + hresult + ", message=" + message + ", response=" + response);
		MessageInterchange messageI = GeneralQueryRqSoapImpl.ticketsPending.get(ticket);
		if (messageI!=null){
			LOGGER.log(Level.INFO, "message "+ messageI.getUuid() + " received from quickbooks");
			MessageManager mm = MessageManager.getInstance();
			GeneralQueryRqSoapImpl.ticketsPending.remove(ticketsPending);
			messageI.setResponseTime(new Date());
			if (response == null || "".equals(response)){
				messageI.setResponse(message);
			}else{
				messageI.setResponse(response);
			}
			
			mm.changeMessageStatus(messageI, "PROCESSED");
			return 1;
		}else{
			return 100;
		}
	}

	@Override
	public String sendRequestXML(String ticket, String strHCPResponse,
			String strCompanyFileName, String qbXMLCountry, int qbXMLMajorVers,
			int qbXMLMinorVers) {
		//Example qbXML to Query for an Item
		//http://www.consolibyte.com/wiki/doku.php/quickbooks_qbxml_itemquery

		MessageManager mm = MessageManager.getInstance();
		
		MessageInterchange message = mm.pollNextMessageToProcess();
		if (message!=null){
			
			String strMessage = message.getRequest();
			strMessage = processStr(strMessage);
		
			LOGGER.log(Level.INFO, "message "+ message.getUuid() + " will be send to quickbooks");
			StringBuffer query = new StringBuffer("<?xml version=\"1.0\" encoding=\"utf-8\"?><?qbxml version=\"12.0\"?><QBXML><QBXMLMsgsRq onError=\"continueOnError\">");
			query.append(strMessage);
			query.append("</QBXMLMsgsRq></QBXML>");
			
			mm.changeMessageStatus(message, "WAIT_RESPONSE");
			
			GeneralQueryRqSoapImpl.ticketsPending.put(ticket, message);
			
			LOGGER.log(Level.FINE, "receiveResponseXML ticket="+ticket + ", message=" + query);
			
			return query.toString();
		}
		return "";
	}
	
	private String processStr(String strMessage) {
		String toReturn = strMessage;
		Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
		Pattern patternOffset = Pattern.compile("today(.*)(\\+|\\-[0-9]*)");
		Matcher matcher = pattern.matcher(strMessage);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		while(matcher.find()){
			String group =matcher.group();
			Matcher offsetM = patternOffset.matcher(group);
			if (offsetM.find()){
				String offset = offsetM.group(2);
				if (offset!=null){
					Calendar today = Calendar.getInstance();
					today.add(Calendar.DAY_OF_MONTH, Integer.valueOf(offset));
					toReturn = toReturn.replace(group,sdf.format(today.getTime()));
				}else{
					toReturn = toReturn.replace(group,sdf.format(new Date()));
				}
			}else{
				toReturn = toReturn.replace(group,sdf.format(new Date()));
			}
			
			System.out.println("encontrado patron: " + group);
			
		}
		
		return toReturn;
	}

	private static class cleaner implements Runnable{
		
		@Override
		public void run() {
			long timeToClean = Long.parseLong(PropertiesManager.getProperty("max_wait_response"))/4;
			while (true){
				purgeOldMessage();
				try {
					Thread.sleep(timeToClean);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}
		
		public void purgeOldMessage(){
			LOGGER.log(Level.INFO, "purgeOldMessages");
			if (ticketsPending == null){
				return;
			}
			String[] ticketsPendingKey = (String[]) ticketsPending.keySet().toArray(new String[ticketsPending.keySet().size()]);
			for (int i = 0; i < ticketsPendingKey.length; i++) {
				String ticketPending = ticketsPendingKey[i];
				MessageInterchange m = ticketsPending.get(ticketPending);
				long max_wait_response = Long.parseLong(PropertiesManager.getProperty("max_wait_response"));
				if (System.currentTimeMillis() >  m.getCreationTime().getTime()+ (max_wait_response*1.5)){
					ticketsPending.remove(ticketPending);
					LOGGER.log(Level.INFO, "purgeOldMessages cleaning ticketPending=" + ticketPending);
				}
			}
		}
	}

}
