package runner;

import enums.Constants.TransactionStatus;
import service.TransactionService;

public class Runner {

	public static void main(String[] args) {
//		NotificationService.getInstance().sendEmail("subi", "asd", "asd");
		try {
			TransactionService.getInstance().updateTransactionStatus(5519l, TransactionStatus.Completed);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
