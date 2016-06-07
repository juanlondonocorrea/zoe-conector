
package com.harana.quickbooks.ws.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "synch", namespace = "http://ws.quickbooks.harana.com/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "synch", namespace = "http://ws.quickbooks.harana.com/", propOrder = {
    "uploadOperations",
    "responseFormat",
    "cache",
    "xpathExp",
    "extraData"
})
public class Synch {

    @XmlElement(name = "uploadOperations", namespace = "")
    private String uploadOperations;
    @XmlElement(name = "responseFormat", namespace = "")
    private String responseFormat;
    @XmlElement(name = "cache", namespace = "")
    private String cache;
    @XmlElement(name = "xpathExp", namespace = "")
    private String xpathExp;
    @XmlElement(name = "extraData", namespace = "")
    private String extraData;

    /**
     * 
     * @return
     *     returns String
     */
    public String getUploadOperations() {
        return this.uploadOperations;
    }

    /**
     * 
     * @param uploadOperations
     *     the value for the uploadOperations property
     */
    public void setUploadOperations(String uploadOperations) {
        this.uploadOperations = uploadOperations;
    }

    /**
     * 
     * @return
     *     returns String
     */
    public String getResponseFormat() {
        return this.responseFormat;
    }

    /**
     * 
     * @param responseFormat
     *     the value for the responseFormat property
     */
    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

	public String getCache() {
		return cache;
	}

	public String getXpathExp() {
		return xpathExp;
	}

	public void setCache(String cache) {
		this.cache = cache;
	}

	public void setXpathExp(String xpathExp) {
		this.xpathExp = xpathExp;
	}

	public String getExtraData() {
		return extraData;
	}

	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}
	
}
