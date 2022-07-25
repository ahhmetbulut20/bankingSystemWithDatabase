package com.ahhmet.bankingSystem.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ahhmet.bankingSystem.accountModel.LogModel;
import com.ahhmet.bankingSystem.accountModel.AccountModel;
import com.ahhmet.bankingSystem.messages.AccountCreateFailedResponse;
import com.ahhmet.bankingSystem.messages.AccountCreateSuccessResponse;
import com.ahhmet.bankingSystem.messages.AccountTransferResponse;
import com.ahhmet.bankingSystem.repositories.RepositoryInterface;
import com.ahhmet.bankingSystem.requestsForAccounts.AccountCreateRequest;
import com.ahhmet.bankingSystem.requestsForAccounts.AddingBalanceRequest;
import com.ahhmet.bankingSystem.requestsForAccounts.TransferredBalanceRequest;
import com.ahhmet.bankingSystem.service.IAccountService;

@RestController
@RequestMapping("aBank")
public class BankingAccountController {
	
	@Autowired
	private IAccountService service;
	
	@Autowired
	private KafkaTemplate<String,String>producer;
	
	@PostMapping(path="/account")
	public ResponseEntity<?> createAccount(@RequestBody AccountCreateRequest account) throws IOException{
		AccountModel createResult=service.create(account.getName(), account.getSurname(), account.getEmail(), account.getTc(), account.getType());
		if(createResult!=null) {
			AccountCreateSuccessResponse response=new AccountCreateSuccessResponse();
			response.setMessage("Account Created");
			response.setAccountNumber(createResult.getAccountNumber());
			return ResponseEntity.ok().body(response);
		}
		else {
			AccountCreateFailedResponse response=new AccountCreateFailedResponse();
			response.setMessage("Invalid Account Type : "+ account.getType());
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	@GetMapping(path="/account/{id}")
	public ResponseEntity<?> detailOfAccount(@PathVariable int id){
		AccountModel account=service.findByAccountNumber(id);
		if(account!=null) {
			final ZoneId zoneId=ZoneId.systemDefault();
			System.out.println(ZonedDateTime.ofInstant(account.getDate().toInstant(), zoneId));
			return ResponseEntity.ok().lastModified(ZonedDateTime.ofInstant(account.getDate().toInstant(), zoneId)).body(account);
		}

		else
			return ResponseEntity.badRequest().body(id + " not found");
	}
	
	
	@PatchMapping(path="/account/{id}")
	public ResponseEntity<AccountModel> addingBalance(@RequestBody AddingBalanceRequest request, @PathVariable int id) throws IOException{
		AccountModel account=service.increaseBalance(request.getAmount(),id);
		String message=id+" deposit amount: "+request.getAmount()+ " "+account.getType();
		producer.send("logs",message);
		return ResponseEntity.ok().body(account);
	}
	
	
	@PatchMapping(path="/account/transfer/{id}")
	@Transactional
	public ResponseEntity<?> transfer(@RequestBody TransferredBalanceRequest request, @PathVariable int id ) throws IOException{
		AccountModel account=service.findByAccountNumber(id);
		boolean transferResult=service.transferBalance(request.getAmount(),id,request.getTransferredAccountNumber());
		if(transferResult) {
			AccountTransferResponse response = new AccountTransferResponse();
			response.setMessage("Transferred Succesfully");
			String message=id+" transfer amount: "+request.getAmount()+" "+account.getType()+", transferred account: "+request.getTransferredAccountNumber();
			producer.send("logs",message);
			return ResponseEntity.ok().body(response);
		}
		else {
			AccountTransferResponse response = new AccountTransferResponse();
			response.setMessage("Insufficient Balance");
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	@DeleteMapping(path="/account/{id}")
	public ResponseEntity<?>delete(@PathVariable int id){
		AccountModel account=service.delete(id);
		return ResponseEntity.ok().body(account);
	}
	
	@GetMapping(path="/logs/{id}")
	public ResponseEntity<List<LogModel>> getLogsAccount(@PathVariable int id) throws FileNotFoundException, IOException{
		return ResponseEntity.ok().body(service.accountLogs(id));
	}
	
}
