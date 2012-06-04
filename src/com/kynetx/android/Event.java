package com.kynetx.android;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

public class Event {

	private List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	private String domain, type;
	
	
	public Event(String _domain, String _type) {
		super();
		//add to pair list
		nameValuePairs.add(new BasicNameValuePair("_domain", _domain));
	    nameValuePairs.add(new BasicNameValuePair("_name", _type));
	    //record for local use
	    domain = _domain;
	    type = _type;
	}
	
	public String typeName() {
		return domain + ":" + type;
	}

	public void addAttribute(String attrname, String attrvalue){
		nameValuePairs.add(new BasicNameValuePair(attrname, attrvalue));
	}
	public void addAttribute(String attrname, double attrvalue){
		addAttribute(attrname, Double.toString(attrvalue));
	}
	public void addAttribute(String attrname, int attrvalue){
		addAttribute(attrname, Integer.toString(attrvalue));
	}
	
	/*
	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
    nameValuePairs.add(new BasicNameValuePair("_domain", "smartphone"));
    nameValuePairs.add(new BasicNameValuePair("_name", "location"));
    nameValuePairs.add(new BasicNameValuePair("latitude", Double.toString(loc.getLatitude())));
    nameValuePairs.add(new BasicNameValuePair("longitude", Double.toString(loc.getLongitude())));
    nameValuePairs.add(new BasicNameValuePair("accuracy", Double.toString(loc.getAccuracy())));
	*/
	
	public UrlEncodedFormEntity asEntity(){
		try {
			return new UrlEncodedFormEntity(nameValuePairs);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
