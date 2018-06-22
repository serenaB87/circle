package com.consulthink.circle.model;

import java.io.Serializable;

public class MessageObj implements Serializable{
	
	private Integer code;
	
	private String message;

	public MessageObj() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MessageObj(Integer code, String message) {
		super();
		this.code = code;
		this.message = message;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "MessageObj [code=" + code + ", message=" + message + "]";
	}
	
}
