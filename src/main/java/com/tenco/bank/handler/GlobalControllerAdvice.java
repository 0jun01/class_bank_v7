package com.tenco.bank.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice // Ioc 대상  (싱글톤 패턴) 자동으로 메모리에 올라감
public class GlobalControllerAdvice {

	@ExceptionHandler(value = Exception.class)
	@ResponseBody /// 데이터를 반환 
	public ResponseEntity<Object> handleResourceNotFoundException(Exception e){
		System.out.println("GlobalControllerAdvice 오류 확인 : ");
		return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
	}
	
}
