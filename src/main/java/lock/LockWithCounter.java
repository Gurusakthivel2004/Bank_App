package lock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class LockWithCounter {
	public ReentrantLock getLock() {
		return lock;
	}

	public AtomicInteger getCount() {
		return count;
	}

	private final ReentrantLock lock;
	private final AtomicInteger count;

	public LockWithCounter(ReentrantLock lock) {
		this.lock = lock;
		this.count = new AtomicInteger(0);
	}

	public void lock() {
		count.incrementAndGet();
		lock.lock();
	}

	public void unlock() {
		lock.unlock();
	}
}
