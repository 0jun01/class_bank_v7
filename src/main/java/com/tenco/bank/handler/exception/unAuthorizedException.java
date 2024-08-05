package com.tenco.bank.handler.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class unAuthorizedException extends RuntimeException{

	private HttpStatus status;
	
	// throw new UnAuthorizedException( , )
	public unAuthorizedException(String message, HttpStatus status) {
		super(message);
		this.status = status;
	}
	
	
}
