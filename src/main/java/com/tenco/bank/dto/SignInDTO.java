package com.tenco.bank.dto;

import com.tenco.bank.repository.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Builder
@ToString
@AllArgsConstructor
public class SignInDTO {

	private String username;
	private String password;
	
	// 2단계 로직 - User object 반환
	public User toUser() {
		return User.builder()
				.username(this.getUsername())
				.password(this.getPassword())
				.build();
	}
	
	// TODO - 추후 사진 업로드 기능 추가 예정
}
