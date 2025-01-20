package service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskExecutorService {
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public void submitTask(Runnable task) {
		executor.submit(task); 
	}

	public void shutdown() {
		executor.shutdown();
	}
	
	
}
