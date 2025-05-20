package runner;

import java.io.File;

import crm.CRMHttpService;
import initializer.Initializer;
import util.FileUtils;

public class BulkUploadRunner {

	private static final String UPLOAD_URL = "https://content.zohoapis.com/crm/v6/upload"; 
	
	public static void main(String[] args) {
		try {
			
			Initializer.setDataSource();
			String filePath = "bulk_csv/contact_bulk.csv";
			File csvFile = new File(filePath);
			File zippedFile = FileUtils.zipCsvFile(csvFile);

			if (!csvFile.exists() || !csvFile.isFile()) {
				System.err.println("CSV file does not exist: " + filePath);
				return;
			}
			

			CRMHttpService crmService = CRMHttpService.getInstance();

			String fileId = crmService.uploadFileToCrm(zippedFile, UPLOAD_URL);

			System.out.println("File uploaded successfully! File ID: " + fileId);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Upload failed: " + e.getMessage());
		}
	}
}
