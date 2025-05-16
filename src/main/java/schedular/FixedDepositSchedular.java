package schedular;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crm.DealsService;
import dao.DAO;
import dao.DaoFactory;
import enums.Constants.AccountType;
import enums.Constants.DealsFields;
import enums.Constants.HttpStatusCodes;
import enums.Constants.TaskExecutor;
import model.Account;
import model.Criteria;
import model.FixedDeposit;
import model.ModuleLog;
import model.User;
import service.FixedDepositService;
import service.TransactionService;
import service.UserService;
import util.CRMQueueManager;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class FixedDepositSchedular {

	private static final Logger logger = LogManager.getLogger(FixedDepositSchedular.class);
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private final FixedDepositService fixedDepositService = FixedDepositService.getInstance();
	private final DAO<Account> accountDAO = DaoFactory.getDAO(Account.class);

	public void startScheduler() throws Exception {
		scheduler.scheduleAtFixedRate(this::processFixedDeposits, 0, 30, TimeUnit.MINUTES);
		logger.info("Fixed Deposit scheduler initialized: runs every 24 hours.");
	}

	public void stopScheduler() {
		logger.info("Shutting down Fixed Deposit scheduler...");
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
				logger.warn("Scheduler forced shutdown due to timeout.");
			} else {
				logger.info("Scheduler shut down successfully.");
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
			logger.error("Scheduler interrupted during shutdown: {}", e.getMessage(), e);
		}
	}

	private void processFixedDeposits() {
		logger.info("Fixed Deposit scheduler started...");
		try {
			List<FixedDeposit> fixedDeposits = fixedDepositService.getFixedDeposits(new HashMap<>());
			if (fixedDeposits.isEmpty()) {
				logger.info("No fixed deposits found.");
				return;
			}

			List<Object> maturedDepositIds = new ArrayList<>();
			logger.info("Found {} fixed deposits.", fixedDeposits.size());

			for (FixedDeposit deposit : fixedDeposits) {
				if (deposit.getMaturityDate() <= System.currentTimeMillis()) {
					logger.info("Matured: " + deposit.getId());
					logger.info(deposit.getMaturityDate() + " " + System.currentTimeMillis());
					processMaturedDeposit(deposit);
					maturedDepositIds.add(deposit.getId());
					
					Long moduleRecordId = fetchModuleRecordId(deposit.getId());
					processDealsUpdate(deposit, moduleRecordId);
				}
			}

			if (!maturedDepositIds.isEmpty()) {
				deleteMaturedDeposits(maturedDepositIds);
			}

		} catch (Exception e) {
			logger.error("Error processing fixed deposits: {}", e.getMessage(), e);
		}
		logger.info("Fixed Deposit processing completed.");
	}
	
	private Long fetchModuleRecordId(Long moduleId) throws Exception {
		Map<String, Object> moduleMap = new HashMap<>();
		moduleMap.put("moduleId", moduleId);
		
		DAO<ModuleLog> moduleLogDAO = DaoFactory.getDAO(ModuleLog.class);
		List<ModuleLog> moduleLogs = moduleLogDAO.get(moduleMap);
		
		return moduleLogs.get(0).getId();
	}

	private void processMaturedDeposit(FixedDeposit deposit) throws Exception {
		BigDecimal returnAmount = deposit.getInterestRate().add(deposit.getAmount());

		List<Account> accounts = getAccountsByAccountNumber(deposit.getAccountNumber());
		Account operationalAccount = getOperationalAccount(accounts);

		createTransaction(operationalAccount.getAccountNumber(), deposit.getAccountNumber(), returnAmount,
				operationalAccount.getBranchId());
	}

	private void processDealsUpdate(FixedDeposit deposit, Long moduleRecordId) throws Exception {

		BigDecimal returnAmount = deposit.getInterestRate().add(deposit.getAmount());
		Map<DealsFields, Object> dealsMap = new HashMap<>();
		dealsMap.put(DealsFields.AMOUNT, returnAmount.toString());
		dealsMap.put(DealsFields.STAGE, "Closed Won");

		TaskExecutor.CRM.submitTask(() -> {
			try {
				CRMQueueManager.addUpdateJsonToSortedSet(DealsService.CRM_MODULE_PK, moduleRecordId, dealsMap,
						DealsService.CRM_MODULE);
			} catch (Exception e) {
				logger.error("CRM Deals push failed: {}", e.getMessage(), e);
			}
		});

	}

	private List<Account> getAccountsByAccountNumber(Long accountNumber) throws Exception {
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", accountNumber);
		List<Account> accounts = accountDAO.get(accountMap);

		if (accounts.isEmpty()) {
			throw new CustomException("No account found for account number: " + accountNumber,
					HttpStatusCodes.BAD_REQUEST);
		}

		Long branchId = accounts.get(0).getBranchId();
		User user = UserService.getInstance().getUserById(accounts.get(0).getUserId());

		Map<String, Object> claimsMap = new HashMap<>();
		claimsMap.put("id", accounts.get(0).getUserId());
		claimsMap.put("role", user.getRole());

		Helper.setThreadLocalValue(claimsMap);

		Map<String, Object> fetchMap = new HashMap<>();
		fetchMap.put("fetch", true);
		fetchMap.put("branchId", branchId);

		return accountDAO.get(fetchMap);
	}

	private Account getOperationalAccount(List<Account> accounts) throws CustomException {
		return accounts.stream().filter(acc -> acc.getAccountTypeEnum().equals(AccountType.Operational)).findFirst()
				.orElseThrow(() -> new CustomException("Operational account not found.", HttpStatusCodes.BAD_REQUEST));
	}

	private void deleteMaturedDeposits(List<Object> depositIds) throws Exception {
		Criteria criteria = new Criteria().setClazz(FixedDeposit.class).setColumn(Collections.singletonList(""))
				.setOperator(Collections.singletonList("IN")).setValues(depositIds);

		SQLHelper.delete(criteria);
		logger.info("Deleted {} matured fixed deposits.", depositIds.size());
	}

	private void createTransaction(Long toAccount, Long fromAccount, BigDecimal amount, Long branchId)
			throws Exception {

		Map<String, Object> txMap = new HashMap<>();
		txMap.put("accountNumber", toAccount.toString());
		txMap.put("transactionAccountNumber", fromAccount.toString());
		txMap.put("amount", amount.toString());
		txMap.put("branchId", branchId.toString());
		txMap.put("remarks", "Fixed Deposit");
		txMap.put("bankName", "Horizon");
		txMap.put("transactionIfsc", "");
		txMap.put("transactionType", "FixedDeposit");

		long txId = TransactionService.getInstance().prepareTransaction(txMap, null);
		TransactionService.getInstance().updateTransaction(txId);
	}
}
