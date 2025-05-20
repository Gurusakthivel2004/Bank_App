package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVWriter;

public class FileUtils {
	private static final Logger logger = LogManager.getLogger(FileUtils.class);
	private static final String BASE_PATH = "bulk_csv";

	public static File generateCsvFiles(String moduleName, List<Map<String, Object>> records) {
		try {
			File csvFile = writeCsvForModule(moduleName, records);
			return zipCsvFile(csvFile);
		} catch (Exception e) {
			logger.error("Failed to write bulk CSV for module {}: {}", moduleName, e.getMessage(), e);
		}
		return null;
	}

	public static File zipCsvFile(File csvFile) throws IOException {
		String zipFilePath = csvFile.getAbsolutePath().replace(".csv", ".zip");
		File zipFile = new File(zipFilePath);

		try (FileOutputStream fos = new FileOutputStream(zipFile);
				ZipOutputStream zos = new ZipOutputStream(fos);
				FileInputStream fis = new FileInputStream(csvFile)) {

			ZipEntry zipEntry = new ZipEntry(csvFile.getName());
			zos.putNextEntry(zipEntry);

			byte[] buffer = new byte[1024];
			int len;
			while ((len = fis.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}

			zos.closeEntry();
		}

		return zipFile;
	}

	private static File writeCsvForModule(String module, List<Map<String, Object>> records) throws Exception {
		if (records.isEmpty())
			return null;

		File dir = new File(BASE_PATH);
		if (!dir.exists())
			dir.mkdirs();

		String filePath = BASE_PATH + File.separator + module.toLowerCase() + "_bulk.csv";
		File csvFile = new File(filePath);

		try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
			Set<String> allHeaders = new LinkedHashSet<>();
			for (Map<String, Object> record : records) {
				allHeaders.addAll(record.keySet());
			}
			writer.writeNext(allHeaders.toArray(new String[0]));

			for (Map<String, Object> record : records) {
				String[] row = allHeaders.stream().map(key -> record.getOrDefault(key, "")).toArray(String[]::new);
				writer.writeNext(row);
			}

			logger.info("CSV file created for module {}: {}", module, filePath);
		}

		return csvFile;
	}
}
