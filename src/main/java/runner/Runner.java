package runner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dblayer.model.Branch;
import dblayer.model.Criteria;
import util.CustomException;
import util.SQLHelper;

public class Runner {
	public static void main() {
		// Create Criteria object
		Criteria criteria = new Criteria();

		// Set the table and columns
		criteria.setTableName("branches");
		criteria.setSelectColumn(new ArrayList<>(Arrays.asList("branch_id", "branch_name"))); // Example: Select columns

		// Set filtering conditions
		criteria.setColumn(new ArrayList<>(Arrays.asList("branch_id", "branch_id")));
		criteria.setOperator(new ArrayList<>(Arrays.asList("=", "LIKE")));
		criteria.setValue(new ArrayList<>(Arrays.asList(1, "1"))); // Example: 1 for =, "1" for LIKE
		criteria.setLogicalOperator("OR"); // Logical operator: OR

		// Call the get method to fetch data
		try {
			List<Branch> branches = SQLHelper.get(criteria); // Replace Branch with the class you're expecting
			branches.forEach(branch -> System.out.println(branch)); // Process the results
		} catch (CustomException e) {
			e.printStackTrace(); // Handle exception
		}

	}
}
