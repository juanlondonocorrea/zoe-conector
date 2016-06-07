package com.harana.quickbooks.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.jvnet.jax_ws_commons.json.JSONBindingID;

@WebService
@BindingType(JSONBindingID.JSON_BINDING)
public class SynchronizerService {
    @Resource
    WebServiceContext ctxt;

	@WebMethod
	public String synch(@WebParam(name="uploadOperations") String uploadOperations,
			@WebParam(name="responseFormat") String responseFormat, 
			@WebParam(name="cache") String cache,
			@WebParam(name="xpathExp") String xpathExp, 
			@WebParam(name="extraData") String extraData){
		try{
			System.out.println("UploadOperations="+uploadOperations);
			System.out.println("responseFormat="+responseFormat);
			System.out.println("cache="+cache);
			System.out.println("xpathExp="+xpathExp);
			MessageContext msc = ctxt.getMessageContext();
			@SuppressWarnings("unchecked")
			Map<String, List<String>>  headers = (Map<String, List<String>>) msc.get(MessageContext.HTTP_RESPONSE_HEADERS);
			List<String> value = new ArrayList<String>();
			value.add("*");
			headers.put("Access-Control-Allow-Origin", value);
		}catch(Exception e){
			
		}
		
		MessageInterchange received = MessageManager.getInstance().sendAndReceive(uploadOperations,responseFormat, cache, xpathExp, extraData);
		if (received!=null){
			return received.getResponse();
		}else{
			return null;
		}
		
	}

}
