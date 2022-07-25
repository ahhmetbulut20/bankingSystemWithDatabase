package com.ahhmet.bankingSystem.service;

import java.io.IOException;
import java.util.List;

import com.ahhmet.bankingSystem.accountModel.LogModel;
import com.ahhmet.bankingSystem.accountModel.AccountModel;

public interface IAccountService {
	public AccountModel create(String name, String surname, String email, String tc, String type) throws IOException;
	public AccountModel findByAccountNumber(int accountNumber);
	public AccountModel increaseBalance(double amount, int accountNumber);
	public boolean transferBalance(double amount, int ownerAccountNumber, int transferredAccountNumber);
	public AccountModel delete(int accountNumber);
	public LogModel saveLogs(String message);
	public List<LogModel> accountLogs(int accountNumber); 
}
