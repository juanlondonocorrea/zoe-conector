package com.harana.quickbooks.ws;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement( name = "MessageInterchange" )
public class MessageInterchange implements Serializable{
	private static final long serialVersionUID = -7930425595490737463L;
	String uuid;
	String request;
	String response;
	String status; //CREATED, WAIT_RESPONSE, PROCESSED, RESPONDED;
	String responseFormat; //XML or JSON
	Date creationTime;
	Date responseTime;
	String cache;
	String xpathExp;
	String extraData;
	
	public MessageInterchange(){
		this.setUuid(UUID.randomUUID().toString());
	}

	@XmlElement (name = "uuid")
	public String getUuid() {
		return uuid;
	}
	
	 @XmlElement (name="request")
	public String getRequest() {
		return request;
	}
	 @XmlElement (name="response")
	public String getResponse() {
		return response;
	}
	@XmlElement (name = "status")
	public String getStatus() {
		return status;
	}
	@XmlElement (name = "creationTime")
	public Date getCreationTime() {
		return creationTime;
	}
	@XmlElement (name = "responseTime")
	public Date getResponseTime() {
		return responseTime;
	}
	
	@XmlElement (name = "responseFormat")
	public String getResponseFormat() {
		return responseFormat;
	}

	@XmlElement (name="cache")
	public String getCache() {
		return cache;
	}

	@XmlElement (name="xpathExp")
	public String getXpathExp() {
		return xpathExp;
	}

	@XmlElement (name="extraData")
	public String getExtraData() {
		return extraData;
	}
	
	private void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public void setRequest(String request) {
		this.request = request;
	}
	public void setResponse(String response) {
		this.response = response;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}
	public void setResponseTime(Date responseTime) {
		this.responseTime = responseTime;
	}

	public void setResponseFormat(String responseFormat) {
		this.responseFormat = responseFormat;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MessageInterchange [");
		if (uuid != null) {
			builder.append("uuid=");
			builder.append(uuid);
			builder.append(", ");
		}
		if (request != null) {
			builder.append("request=");
			builder.append(request);
			builder.append(", ");
		}
		if (response != null) {
			builder.append("response=");
			builder.append(response);
			builder.append(", ");
		}
		if (status != null) {
			builder.append("status=");
			builder.append(status);
			builder.append(", ");
		}
		if (creationTime != null) {
			builder.append("creationTime=");
			builder.append(creationTime);
			builder.append(", ");
		}
		if (responseTime != null) {
			builder.append("responseTime=");
			builder.append(responseTime);
		}
		builder.append("]");
		return builder.toString();
	}

	public void setCache(String cache) {
		this.cache = cache;
	}

	public void setXpathExp(String xpathExp) {
		this.xpathExp = xpathExp;
	}
	
	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}
	
}
