package com.tenco.bank.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tenco.bank.dto.DepositDTO;
import com.tenco.bank.dto.SaveDTO;
import com.tenco.bank.dto.TransferDTO;
import com.tenco.bank.dto.WithdrawalDTO;
import com.tenco.bank.handler.exception.DataDeliveryException;
import com.tenco.bank.handler.exception.RedirectException;
import com.tenco.bank.repository.interfaces.AccountRepository;
import com.tenco.bank.repository.interfaces.HistoryRepository;
import com.tenco.bank.repository.model.Account;
import com.tenco.bank.repository.model.History;
import com.tenco.bank.repository.model.User;
import com.tenco.bank.utils.Define;

@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final HistoryRepository historyRepository;

	@Autowired // 생략 가능 - DI 처리
	public AccountService(AccountRepository accountRepository, HistoryRepository historyRepository) {
		this.accountRepository = accountRepository;
		this.historyRepository = historyRepository;
	}

	/**
	 * 계좌 생성 기능
	 * 
	 * @param dto
	 * @param user
	 */
	@Transactional
	public void createAccount(SaveDTO dto, Integer principalId) {

		int result = 0;
		try {
			result = accountRepository.insert(dto.toAccount(principalId));
		} catch (DataAccessException e) {
			throw new DataDeliveryException("잘못된 요청입니다.", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			throw new RedirectException("알 수 없는 오류", HttpStatus.SERVICE_UNAVAILABLE);
		}
		if (result == 0) {
			throw new DataDeliveryException("정상 처리 되지 않았습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	public List<Account> readAccountListByUserId(Integer userId) {
		List<Account> accountListEntity = null;

		try {
			accountListEntity = accountRepository.findByUserId(userId);
		} catch (DataAccessException e) {
			throw new DataDeliveryException("잘못된 처리 입니다.", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			throw new RedirectException("알 수 없는 오류", HttpStatus.SERVICE_UNAVAILABLE);
		}

		return accountListEntity;
	}

	// 한번에 모든 기능을 생각 힘듬
	// 1. 계좌 존재 여부를 확인 -- select
	// 2. 본인 계좌 여부를 확인 -- 객체 상태값에서 비교
	// 3. 계좌 비번 확인 -- 객체 상태값에서 일치 여부 확인
	// 4. 잔액 여부 확인 -- 객체 상태 값에서 확인
	// 5. 출금 처리 -- update
	// 6. 거래 내역 등록 -- insert(history)
	// 7. 트랜잭션 처리
	@Transactional
	public void updateAccountWithdraw(WithdrawalDTO dto, Integer principalId) {
		// 1.
		// 퍼시스턴스 계층에서 긁어 냈기 때문에 Entity를 붙임
		Account accountEntity = accountRepository.findByNumber(dto.getWAccountNumber());
		if (accountEntity == null) {
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.BAD_REQUEST);
		}

		// 2.
		accountEntity.checkOwner(principalId);

		// 3.
		accountEntity.checkPassword(dto.getWAccountPassword());

		// 4.
		accountEntity.checkBalance(dto.getAmount());

		// 5. 출금 기능
		// accountEntity 객체의 잔액을 변경하고 업데이트 처리해야 한다.
		accountEntity.withdraw(dto.getAmount());
		// update 처리
		accountRepository.updateById(accountEntity);

		// 6 - 거래 내역 등록
		History history = new History();
		history.setAmount(dto.getAmount());
		history.setWBalance(accountEntity.getBalance());
		history.setDBalance(null);
		history.setWAccountId(accountEntity.getId());
		history.setDAccountId(null);

		int rowResultCount = historyRepository.insert(history);
		if (rowResultCount != 1) {
			throw new DataDeliveryException(Define.FAILED_PROCESSING, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// 입금 기능 만들기
	// 1. 계좌 존재 여부 확인
	// 2. 본인 계좌 여부 확인
	// 3. 입금 처리 -- update
	// 4. 거래 내역 등록 -- insert(history)
	@Transactional
	public void updateAccountDeposit(DepositDTO dto, Integer principalId) {
		Account accountEntity = accountRepository.findByNumber(dto.getDAccountNumber());

		if (accountEntity == null) {
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.BAD_REQUEST);
		}
		accountEntity.checkBalance(dto.getAmount());
		accountEntity.deposit(dto.getAmount());
		accountRepository.updateById(accountEntity);

		History history = new History();
		history.setAmount(dto.getAmount());
		history.setWBalance(null);
		history.setDBalance(accountEntity.getBalance());
		history.setWAccountId(null);
		history.setDAccountId(accountEntity.getId());
		int rowResultCount = historyRepository.insert(history);
		if (rowResultCount != 1) {
			throw new DataDeliveryException(Define.FAILED_PROCESSING, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// 이체 기능 만들기
	// 1. 출금 계좌 존재 여부 확인 -- select (객체 리턴 받은 상태)
	// 2. 입금 계좌 존재 여부 확인 -- select (객체 리턴 받은 상태)
	// 3. 출금 계좌 본인 소유 확인 -- 객체 상태값과 세션 아이디 비교
	// 4. 출금 계좌 비밀 번호 확인 -- 객체 상태값과 dto 비밀번호 비교
	// 5. 출금 계좌 잔액 여부 확인 -- 객체 상태값 확인, dto와 비교
	// 6. 입금 계좌 객체 상태값 변경 처리 (거래금액 증가)
	// 7. 입금 계좌 -- update
	// 8. 출금 계좌 객체 상태값 변경 처리 (잔액 - 거래금액)
	// 9. 출금 계좌 -- update
	// 10. 거래 내역 등록 처리
	// 11. 트랜잭션 처리
	@Transactional
	public void updateAccountTransfer(TransferDTO dto, Integer pricipalId) {
		// 출금 계좌
		Account wAccountEntity = accountRepository.findByNumber(dto.getWAccountNumber());
		// 입금 계좌
		Account dAccountEntity = accountRepository.findByNumber(dto.getDAccountNumber());

		// 1.
		if (wAccountEntity == null) {
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.BAD_REQUEST);
		}
		// 2.
		if (dAccountEntity == null) {
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.BAD_REQUEST);
		}
		// 3. 본인 소유 확인
		wAccountEntity.checkOwner(pricipalId);
		// 4. 비밀 번호 확인
		wAccountEntity.checkPassword(dto.getPassword());
		
		// 5. 잔액 여부 확인
		if (wAccountEntity.getBalance() < dto.getAmount()) {
			throw new DataDeliveryException(Define.LACK_Of_BALANCE, HttpStatus.BAD_REQUEST);
		}
		// 6. 입금 계좌 상태값 변경
		dAccountEntity.deposit(dto.getAmount());
		// 7 입금 계좌 update
		accountRepository.updateById(dAccountEntity);
		// 8 출금 계좌 상태값 변경
		wAccountEntity.withdraw(dto.getAmount());
		// 9 출금 계좌 update
		accountRepository.updateById(wAccountEntity);
		// 10 
		History history = History.builder()
							.amount(dto.getAmount())
							.wAccountId(wAccountEntity.getId())
							.dAccountId(dAccountEntity.getId())
							.wBalance(wAccountEntity.getBalance())
							.dBalance(dAccountEntity.getBalance())
							.build();
		int rowResultCount = historyRepository.insert(history);
		if (rowResultCount != 1) {
			throw new DataDeliveryException(Define.FAILED_PROCESSING, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
