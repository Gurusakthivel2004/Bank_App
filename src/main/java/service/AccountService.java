package service;

import java.util.Map;
import java.math.BigDecimal;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dblayer.dao.AccountDAO;
import dblayer.model.Account;
import util.CustomException;
import util.Helper;

/**
 * Service layer for handling account-related operations.
 * Provides methods for retrieving, creating, and deleting accounts.
 */

public class AccountService {

    private final Logger logger = LogManager.getLogger(AccountService.class);
    private AccountDAO accountDAO = new AccountDAO();

    /**
     * Fetches account details based on input parameters.
     * 
     * @param customerId    The ID of the customer.
     * @param accountNumber The account number (0 if not used).
     * @param branchId      The ID of the branch (0 if not used).
     * @param accountCreated Timestamp representing when the account was created.
     * @return List of accounts matching the search criteria.
     * @throws CustomException If an error occurs during account retrieval.
     */
    
    public List<Account> getAccountDetails(Long customerId, Long accountNumber, Long branchId,
    		Long accountCreated) throws CustomException {
        logger.info("Fetching account details for customerId: {}, accountNumber: {}, branchId: {}, accountCreated: {}",
                    customerId, accountNumber, branchId, accountCreated);

        try {
            if (accountCreated > 0) {
                List<Account> accounts = accountDAO.getAccounts(customerId, accountNumber, branchId, accountCreated, 8L);
                if (Helper.getThreadLocalValue().get("branchId") != null) {
                    logger.debug("Filtering accounts based on branch ID");
                    return accountDAO.checkAccountBranchId(accounts);
                }
            }
            logger.debug("Fetching accounts without branch-based filtering");
            return accountDAO.getAccounts(customerId, accountNumber, branchId, accountCreated, 0L);
        } catch (Exception e) {
            logger.error("Error fetching account details: {}", e.getMessage());
            throw new CustomException("Unable to fetch account details. Please try again later.", e);
        }
    }

    /**
     * Creates a new account using the provided account data.
     * 
     * @param accountMap A map containing the account details.
     * @throws CustomException If the account creation process fails.
     */
    
    public void createAccount(Map<String, Object> accountMap) throws CustomException {
        logger.info("Creating a new account with data: {}", accountMap);

        try {
            accountMap.put("minBalance", new BigDecimal(500));
            accountMap.put("branchId", Helper.getThreadLocalValue().get("branchId"));
            accountMap.put("accountNumber", 7018120L);
            accountMap.put("status", "Active");
            accountMap.put("performedBy", Helper.getThreadLocalValue().get("id"));

            long userId = Long.parseLong((String) accountMap.get("userId"));
            boolean isPrimary = getAccountDetails(userId, 0l, 0l, 0l).isEmpty();
            accountMap.put("isPrimary", isPrimary);

            logger.debug("Populated accountMap with additional data: {}", accountMap);

            Account account = Helper.createPojoFromMap(accountMap, Account.class);
            logger.debug("Converted accountMap to Account object: {}", account);

            accountDAO.createAccount(account);
            logger.info("Account successfully created with accountNumber: {}", account.getAccountNumber());
        } catch (CustomException e) {
            logger.error("Error creating account: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during account creation: {}", e.getMessage());
            throw new CustomException("Account creation failed. Please contact support.", e);
        }
    }

    /**
     * Deletes an account based on the provided account ID.
     * 
     * @param accountId The ID of the account to be deleted.
     * @throws CustomException If the account cannot be found or deletion fails.
     */
    
    public void deleteAccount(Long accountId) throws CustomException {
        logger.info("Attempting to delete account with ID: {}", accountId);

        try {
            List<Account> accounts = accountDAO.getAccounts(accountId, 0l, 0l, 0l, 0l);
            if (accounts.isEmpty()) {
                logger.error("No acc"
                		+ "ount found with ID: {}", accountId);
                throw new CustomException("Account not found for ID: " + accountId);
            }

            accountDAO.removeAccount(accountId);
            logger.info("Account successfully deleted with ID: {}", accountId);
        } catch (CustomException e) {
            logger.error("Error deleting account with ID {}: {}", accountId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during account deletion: {}", e.getMessage());
            throw new CustomException("Account deletion failed. Please contact support.", e);
        }
    }
}