package com.consulthink.circle.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.uniquid.register.guest.GuestChannel;

@SuppressWarnings("serial")
public class CircleObj implements Serializable{
	
	private Integer id;
	private String name;
	private List<GuestChannel> guests = new ArrayList<GuestChannel>();
	
	public CircleObj() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CircleObj(Integer id, String name, List<GuestChannel> guests) {
		super();
		this.id = id;
		this.name = name;
		this.guests = guests;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<GuestChannel> getGuests() {
		return guests;
	}

	public void setGuests(List<GuestChannel> guests) {
		this.guests = guests;
	}

	@Override
	public String toString() {
		return "CircleObj [id=" + id + ", name=" + name + ", guests=" + guests + "]";
	}

}
