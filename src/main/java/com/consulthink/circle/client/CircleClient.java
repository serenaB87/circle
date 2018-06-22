package com.consulthink.circle.client;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;

public class CircleClient {

	private String service = null;
	private String path = null;
	private Integer port = null;
	private String url = null;

	public CircleClient(String service, String path, Integer port) {
		this.service = service;
		this.path = path;
		this.port = port;
		this.url = "http://" + service + ":" + port + "/" + path;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public HttpResponse<String> post(String args) {
		HttpResponse<String> response = null;
		
        try {
        	HttpRequestWithBody req = Unirest.post(this.url);
    		req.header("Content-type", "application/json");
            req.body(args);
			response = req.asString();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
        
        return response;
	}
	
	public HttpResponse<String> put(String args) {
		HttpResponse<String> response = null;
		
        try {
        		HttpRequestWithBody req = Unirest.put(this.url);
        		req.header("Content-type", "application/json");
        		if (args != null)
        			req.body(args);
			response = req.asString();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
        
        return response;
	}
	
	public HttpResponse<String> get(String param) {
		HttpResponse<String> response = null;
		String uri = param == null ? this.url : this.url + "/" + param;
		
        try {
        		GetRequest req = Unirest.get(uri);
    			req.header("Content-type", "application/json");
			response = req.asString();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
        
        return response;
	}

	public HttpResponse<String> delete(String filename) {
		HttpResponse<String> response = null;
		String uri = filename == null ? this.url : this.url + "/" + filename;
		
        try {
        		HttpRequestWithBody req = Unirest.delete(uri);
        		req.header("Content-type", "application/json");
			response = req.asString();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
        
        return response;
	}
	
}
