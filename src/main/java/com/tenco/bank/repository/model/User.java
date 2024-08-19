package com.tenco.bank.repository.model;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

public class User {

	private Integer id;
	private String username;
	private String password;
	private String fullname;
	private String originFileName;
	private String uploadFileName;
	private Timestamp createdAt;
	
	public String setUpUserImage() {
		String img = "";
		if(uploadFileName == null) {
			img ="https://picsum.photos/id/1/350"; 
		} else if(fullname.contains("OAuth_")) {
			img = uploadFileName;
		} else {
			img = "/images/uploads/" + uploadFileName;
		}
		return img;
	}
	
}
