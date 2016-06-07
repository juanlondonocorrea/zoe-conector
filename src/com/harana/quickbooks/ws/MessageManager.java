package com.harana.quickbooks.ws;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

public class MessageManager {
	JAXBContext jbc;
	ConcurrentLinkedQueue<MessageInterchange> queueToProcessMessages;
	
	private final static Logger LOGGER = Logger.getLogger(MessageManager.class.getName()); 	
	static MessageManager instance;
	
	static ConcurrentHashMap<String,String> cacheM = new ConcurrentHashMap<String, String>();
	static ConcurrentHashMap<String,MessageInterchange> allMessages = new ConcurrentHashMap<String, MessageInterchange>();
	
	MessageManager(){
		try {
			jbc = JAXBContext.newInstance( MessageInterchange.class );
		} catch (JAXBException e) {
			LOGGER.throwing(MessageManager.class.getName(), "MessageManger()", e);
			e.printStackTrace();
		}
	}
	
	static public MessageManager getInstance(){
		if (instance==null){
			instance = new MessageManager();
		}
		return instance;
	}
	
	public void initCaches(){
		Set<String> keys = cacheM.keySet();
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			String cacheName = (String) iterator.next();
			cacheM.remove(cacheName);
		}
	}
	
	public void clearCache(String cache){
		cacheM.remove(cache);
	}
	
	public String addMessageToSend(String request, String format, String cache, String xpathExp){
		MessageInterchange message = new MessageInterchange();
		message.setRequest(request);
		message.setCreationTime(new java.util.Date());
		message.setStatus("CREATED");
		message.setResponseFormat(format);
		message.setCache(cache);
		message.setXpathExp(xpathExp);
		storeMessage(message);
		allMessages.put(message.getUuid(), message);
		return message.getUuid();
	}
	
	public MessageInterchange sendAndReceive(String request, String format, String cache, String xpathExp, String extraData){
		MessageInterchange message = new MessageInterchange();
		message.setRequest(request);
		message.setCreationTime(new java.util.Date());
		message.setStatus("RESPOND");
		message.setResponseFormat(format);
		message.setCache(cache);
		message.setXpathExp(xpathExp);
		message.setExtraData(extraData);
		
		if (extraData!=null && !"".equals(extraData)){
			processExtraData(extraData);
		}
		if ("flushallcache".equalsIgnoreCase(request)){
			this.initCaches();
			message.setResponse("Caches flushed");
			return message;
		}
		if ("flushcache".equalsIgnoreCase(request)){
			cacheM.remove(cache);
			message.setResponse("Cache " + cache + " flushed");
			return message;
		}
		if ("refillcache".equalsIgnoreCase(request)){
			CacheScheduler.fillCacheNow(cache);
			message.setResponse("Cache " + cache + " start filling");
			return message;
		}
		if ("scheduleallcaches".equalsIgnoreCase(request)){
			//this.initCaches();
			CacheScheduler.scheduleAutomaticCacheFill();
			message.setResponse("Caches scheduled");
			
			return message;
		}
		if ("pricelevel".equalsIgnoreCase(request)){
			if (cache!=null && !"".equals(cache) && cacheM.containsKey(cache)){
				message.response=cacheM.get(cache);
				return message;
			}else{
				message.response = csvToJSON("pricelevel.csv",cache);
				cacheM.put(cache, message.response);
				message.setStatus("RESPOND");
				this.storeMessage(message);
				return message;
			}
			
		}

		if (cache!=null && !"".equals(cache) && cacheM.containsKey(cache)){
			LOGGER.log(Level.INFO, "cache find " + cache );
			String messageToReturn = cacheM.get(cache);
			if (xpathExp!=null && !"".equalsIgnoreCase(xpathExp))
				try {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document document = builder.parse(new InputSource(new StringReader(messageToReturn)));
					LOGGER.log(Level.INFO, "XML Document ready");
		 
					XPathFactory xPathfactory = XPathFactory.newInstance();
					XPath xpath = xPathfactory.newXPath();
					XPathExpression expr = xpath
							.compile(xpathExp);
	
					String firstName = document.getFirstChild().getFirstChild().getNextSibling().getNodeName();
					String secondName = document.getFirstChild().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNodeName();
	
					Document newXmlDocument = DocumentBuilderFactory.newInstance()
			                .newDocumentBuilder().newDocument();
			        Element qbxml = newXmlDocument.createElement("QBXML");
			        Element QBXMLMsgsRs = newXmlDocument.createElement(firstName);
			        Element secondNode = newXmlDocument.createElement(secondName);
			        
			        newXmlDocument.appendChild(qbxml).appendChild(QBXMLMsgsRs).appendChild(secondNode);
					NodeList nodeList = (NodeList)expr.evaluate(document,  XPathConstants.NODESET);
					LOGGER.log(Level.INFO, "XPath expression applied nodeList.length=" + nodeList.getLength());
					for (int i = 0; i < nodeList.getLength(); i++) {
					   Node nNode = nodeList.item(i);
					   Node copyNode = newXmlDocument.importNode(nNode, true);
					   secondNode.appendChild(copyNode);
					}
					LOGGER.log(Level.INFO, "second document created");
					
					DOMImplementationLS domImplementationLS = 
					        (DOMImplementationLS) newXmlDocument.getImplementation();
					    LSSerializer lsSerializer = 
					        domImplementationLS.createLSSerializer();
					    messageToReturn = lsSerializer.writeToString(newXmlDocument);

					LOGGER.log(Level.INFO, "message extracted with xpathexp");
					LOGGER.log(Level.FINE, "message extracted with xpathexp " + messageToReturn );
				} catch (Exception e) {
					e.printStackTrace();
				}

			if ("json".equalsIgnoreCase(format)){
				messageToReturn = toJSON(messageToReturn);
				LOGGER.log(Level.INFO, "message converted to json");
			}
			message.setRequest(request);
			message.setCreationTime(new java.util.Date());
			message.setStatus("RESPOND");
			message.setResponseFormat(format);
			message.setResponse(messageToReturn);
			message.setCache(cache);
			message.setXpathExp(xpathExp);
			return message;
		}else if (request!=null && !"".equals(request)){
			String id = addMessageToSend(request,format, cache, xpathExp);
			return receive(id);
		}else{
			message.setResponse("");
			return message;
		}
		
	}
	
	
	//procesa la extra data (photo and signature
	private void processExtraData(String extraData) {
		try {
			JSONArray jsonArr= new JSONArray(extraData);
			System.out.println("processExtraData:" + jsonArr.toString());
			for (int i = 0; i< jsonArr.length();i++){
				JSONObject jobj = jsonArr.getJSONObject(i);
				if ("invoicePhoto".equalsIgnoreCase(jobj.getString("type"))){
					storeInvoicePhoto(jobj.getString("refNumber"), jobj.getString("companyName"), jobj.getString("date"), 
							jobj.getString("data"));
				}else if ("invoiceSignature".equalsIgnoreCase(jobj.getString("type"))){
					storeInvoiceSignature(jobj.getString("refNumber"), jobj.getString("companyName"), jobj.getString("date"), 
							jobj.getString("data"));
				}else if ("creditMemoPhoto".equalsIgnoreCase(jobj.getString("type"))){
					storeCreditMemoPhoto(jobj.getString("refNumber"), jobj.getString("companyName"), jobj.getString("date"), 
							jobj.getString("data"));
				}else if ("creditMemoSignature".equalsIgnoreCase(jobj.getString("type"))){
					storeCreditMemoSignature(jobj.getString("refNumber"), jobj.getString("companyName"), jobj.getString("date"), 
							jobj.getString("data"));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void storeInvoicePhoto(String refNumber, String companyName, String date,
			String data) {
		String fileDirectory = PropertiesManager.getProperty("resources_directory_path") + "/invoices";
		String fileName = fileDirectory + "/photo_" + fileNameEncoding(companyName) + "_" + refNumber + "_" + date +  ".jpg";
		File file = new File(fileDirectory);
		file.mkdirs();
		String realData = data;
		if (realData.indexOf("base64")>-1){
			realData = realData.substring(realData.indexOf(",")+1);
		}
		byte[] dataBinary = Base64.getDecoder().decode(realData);
		try {
			OutputStream stream = new FileOutputStream(fileName);
			stream.write(dataBinary);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void storeInvoiceSignature(String refNumber, String companyName, String date,
			String data) {
		String fileDirectory = PropertiesManager.getProperty("resources_directory_path") + "/invoices";
		String fileName = fileDirectory + "/signature_" +fileNameEncoding(companyName) + "_" + refNumber + "_" + date +  ".png";
		File file = new File(fileDirectory);
		file.mkdirs();
		String realData = data;
		if (realData.indexOf("base64")>-1){
			realData = realData.substring(realData.indexOf(",")+1);
		}
		byte[] dataBinary = Base64.getDecoder().decode(realData);
		try {
			OutputStream stream = new FileOutputStream(fileName);
			stream.write(dataBinary);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void storeCreditMemoPhoto(String refNumber, String companyName, String date,
			String data) {
		String fileDirectory = PropertiesManager.getProperty("resources_directory_path") + "/creditMemos";
		String fileName = fileDirectory + "/photo_" + fileNameEncoding(companyName) + "_" + refNumber + "_" + date +  ".jpg";
		File file = new File(fileDirectory);
		file.mkdirs();
		String realData = data;
		if (realData.indexOf("base64")>-1){
			realData = realData.substring(realData.indexOf(",")+1);
		}
		byte[] dataBinary = Base64.getDecoder().decode(realData);
		try {
			OutputStream stream = new FileOutputStream(fileName);
			stream.write(dataBinary);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void storeCreditMemoSignature(String refNumber, String companyName, String date,
			String data) {
		String fileDirectory = PropertiesManager.getProperty("resources_directory_path") + "/creditMemos";
		String fileName = fileDirectory + "/signature_" +fileNameEncoding(companyName) + "_" + refNumber + "_" + date +  ".png";
		File file = new File(fileDirectory);
		file.mkdirs();
		String realData = data;
		if (realData.indexOf("base64")>-1){
			realData = realData.substring(realData.indexOf(",")+1);
		}
		byte[] dataBinary = Base64.getDecoder().decode(realData);
		try {
			OutputStream stream = new FileOutputStream(fileName);
			stream.write(dataBinary);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String fileNameEncoding(String fileName){
		char fileSep = '/'; // ... or do this portably.
		char escape = '%';
		int len = fileName.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
		    char ch = fileName.charAt(i);
		    if (ch < ' ' || ch >= 0x7F || ch == fileSep || ch=='*' || (ch == '.' && i == 0) // we don't want to collide with "." or ".."!
		        || ch == escape) {
		        sb.append(escape);
		        if (ch < 0x10) {
		            sb.append('0');
		        }
		        sb.append(Integer.toHexString(ch));
		    } else {
		        sb.append(ch);
		    }
		}
		return sb.toString();
	}

	public MessageInterchange receive(String id){
		MessageInterchange message = null;
		File file;
		boolean fileExist = false;
		long initialTime = System.currentTimeMillis();
		long max_wait_response = Long.parseLong(PropertiesManager.getProperty("max_wait_response"));
		do{
			file = new File(PropertiesManager.getProperty("queue_directory_path")+"/PROCESSED/" + id + ".xml");
			fileExist = file.exists();
			if (fileExist){
				break;
			}else{
				try {
					Thread.sleep(Integer.parseInt(PropertiesManager.getProperty("polling_time")));
				} catch (InterruptedException e) {
				}
			}
		}while(System.currentTimeMillis()<initialTime+max_wait_response);
		if (fileExist){
			LOGGER.log(Level.INFO, "message received file " +  file.getAbsolutePath());
			try {
				Unmarshaller unmarshaller = jbc.createUnmarshaller();
				message = (MessageInterchange)unmarshaller.unmarshal(file);
				if (message.cache!=null && !"".equals(message.cache) && !"".equals(message.response)){
					cacheM.put(message.cache, message.response);
				}
				if ("JSON".equalsIgnoreCase(message.getResponseFormat()) && !"".equals(message.response)){
					message.response = toJSON(message.response);
				}

				this.changeMessageStatus(message, "RESPOND");
				allMessages.remove(message.getUuid());
			} catch (JAXBException e) {
				LOGGER.throwing(MessageManager.class.getName(), "receive(String id)", e);
				e.printStackTrace();
			}
		}else{
			message = allMessages.get(id);
			LOGGER.log(Level.INFO, "message timedout " + id );
			message.response = "TIMEDOUT " + max_wait_response;
			this.changeMessageStatus(message, "TIMEDOUT");
		}
		return message;
	}
	
	String toJSON(String text){
		JSONObject json;
		try {
			json = XML.toJSONObject(text);
			return json.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return text;
		}
	}
	
	String csvToJSON(String filecsv, String nameObject){
		@SuppressWarnings("resource")
		String csv = new Scanner(PropertiesManager.class.getClassLoader().getResourceAsStream(filecsv)).useDelimiter("\\A").next();
		StringBuffer json = new StringBuffer();
		json.append("{\"" + nameObject +"\":[");
		String[] lines = csv.split("\r\n");
		String[] headers = lines[0].split(",");
		for (int i = 1; i < lines.length; i++) {
			json.append("{");
			String[] values = lines[i].split(",");
			for (int j = 0; j < values.length; j++) {
				json.append("\"" + headers[j].trim() +"\":\"" + values[j].trim().replaceAll("\"", "\\\\\"") + "\"");
				if (j<values.length-1){
					json.append(",");
				}
			}
			json.append("}");
			if (i<lines.length-1){
				json.append(",");
			}
		}
		json.append("]}");
		return json.toString();
	}
	
	void storeMessage(MessageInterchange message){
		try {
			LOGGER.log(Level.FINE, "storeMessage ini " + message.uuid);
			File dir = new File(PropertiesManager.getProperty("queue_directory_path")+"/" + message.getStatus());
			dir.mkdirs();
			File file = new File(dir, message.getUuid() + ".xml");
			Marshaller marshaller = jbc.createMarshaller();
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
			marshaller.marshal( message, file );
		} catch (JAXBException e) {
			LOGGER.throwing(MessageManager.class.getName(), "void storeMessage(MessageInterchange message)", e);
			e.printStackTrace();
		}
	}
	void deleteMessageFromCurrentPersistence(MessageInterchange message){
		File file = new File(PropertiesManager.getProperty("queue_directory_path")+"/" + message.getStatus() + "/" + message.getUuid() + ".xml");
		file.delete();
		LOGGER.log(Level.FINE, "deleteMessage:"+message.uuid);
	}
	
	void changeMessageStatus(ArrayList<MessageInterchange> messages, String newStatus){
		for (MessageInterchange messageInterchange : messages) {
			changeMessageStatus(messageInterchange, newStatus);
		}
	}
	
	void changeMessageStatus(MessageInterchange message, String newStatus){
		LOGGER.log(Level.INFO, "message " + message.getUuid()  + " changing status from  " + message.getStatus() +  " to " + newStatus);
		deleteMessageFromCurrentPersistence(message);
		message.setStatus(newStatus);
		storeMessage(message);
		if (newStatus.equals("TIMEDOUT") || newStatus.equals("RESPOND") || newStatus.equals("DEADQUEUE")){
			allMessages.remove(message.getUuid());
		}
	}
	
	synchronized public MessageInterchange pollNextMessageToProcess(){
		if (queueToProcessMessages==null || queueToProcessMessages.isEmpty()){
			ArrayList<MessageInterchange> messages = this.getNextMessagesToProcess();
			if (messages!=null){
				queueToProcessMessages = new ConcurrentLinkedQueue<MessageInterchange>(messages);
			}else{
				queueToProcessMessages = new ConcurrentLinkedQueue<MessageInterchange>();

			}
		}
		if (!queueToProcessMessages.isEmpty()){
			return queueToProcessMessages.poll();
		}
		return null;
	}
	
	public ArrayList<MessageInterchange> getNextMessagesToProcess(){
		
		File dir = new File(PropertiesManager.getProperty("queue_directory_path")+"/CREATED");
		File deadQueue = new File(PropertiesManager.getProperty("queue_directory_path")+"/DEADQUEUE");
		deadQueue.mkdirs();
		File files[] = dir.listFiles();
		
		//sort por la fecha de modificación (para procesar primero los mas antiguos
		Arrays.sort(files, new Comparator<File>() {
		    public int compare(File f1, File f2) {
		        return Long.compare(f1.lastModified(), f2.lastModified());
		    }
		});
		
		ArrayList<MessageInterchange> toReturn = new ArrayList<MessageInterchange>();
		for (int i = 0; i < files.length; i++) {
			MessageInterchange message;
			try {
				Unmarshaller unmarshaller = jbc.createUnmarshaller();
				message = (MessageInterchange) unmarshaller.unmarshal(files[i]);
			} catch (JAXBException e) {
				e.printStackTrace();
				files[i].renameTo(new File(deadQueue,files[i].getName()) );
				return null;
			}
			toReturn.add(message);
		}
		return toReturn;
		
	}

}
