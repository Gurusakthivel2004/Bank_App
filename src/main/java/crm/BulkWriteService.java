package crm;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BulkWriteService {

    private static final Logger logger = LogManager.getLogger(BulkWriteService.class);
    private static final String BASE_PATH = "bulk_csv"; 

    public static void generateCsvFiles(String moduleName, List<Map<String, String>> records) {
         try {
             writeCsvForModule(moduleName, records);
         } catch (Exception e) {
             logger.error("Failed to write bulk CSV for module {}: {}", moduleName, e.getMessage(), e);
         }
    }

    private static void writeCsvForModule(String module, List<Map<String, String>> records) throws Exception {
        if (records.isEmpty()) return;

        File dir = new File(BASE_PATH);
        if (!dir.exists()) dir.mkdirs();

        String filePath = BASE_PATH + File.separator + module.toLowerCase() + "_bulk.csv";

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {

            Set<String> allHeaders = new LinkedHashSet<>();
            for (Map<String, String> record : records) {
                allHeaders.addAll(record.keySet());
            }
            writer.writeNext(allHeaders.toArray(new String[0]));

            for (Map<String, String> record : records) {
                String[] row = allHeaders.stream()
                        .map(key -> record.getOrDefault(key, ""))
                        .toArray(String[]::new);
                writer.writeNext(row);
            }

            logger.info("CSV file created for module {}: {}", module, filePath);
        }
    }
}
