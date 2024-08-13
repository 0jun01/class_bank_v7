package com.tenco.bank.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tenco.bank.handler.AuthInterceptor;

import lombok.RequiredArgsConstructor;

// @Component // 하나의 클래스롤 IoC 하고 싶다면 사용
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer{
	
	@Autowired // DI
	private final AuthInterceptor authInterceptor;

	// @RequiredArgsConstructor <-- 생성자 대신 사용 가능
	
//	public WebMvcConfig(AuthInterceptor authInterceptor) {
//		this.authInterceptor = authInterceptor;
//	}
	
	// 우리가 만들어 놓은 AuthInterceptor를 동록해야 함.
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor)
				.addPathPatterns("/account/**")
				.addPathPatterns("/auth/**");
			
	}
}
