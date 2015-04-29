package com.emc.mongoose.persist;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by olga on 29.04.15.
 */
public class RunHolder {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final BlockingQueue<PersistEvent> queue = new ArrayBlockingQueue<PersistEvent>(
		RunTimeConfig.getContext().getPersistExecutorQueueSize()
	);
	//
	private final Lock lock = new ReentrantLock();
	private final Condition persistRun = lock.newCondition();
	//private final Condition emptyQueue = lock.newCondition();
	//
	private final String runName;
	//
	public RunHolder(final String runName){
		this.runName = runName;
	}

	public void addEvent(final PersistEvent event){
		try {
			queue.put(event);
		} catch (InterruptedException e) {
			LogUtil.failure(LOG, Level.ERROR, e, "Interrupted");
		}
	}

	public void initRun(){
		try {
			lock.tryLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			System.out.println("init run");
			persistRun.signalAll();
		} catch (InterruptedException e) {
			LogUtil.failure(LOG, Level.ERROR, e, "Interrupted");
		}finally {
			lock.unlock();
		}
	}

	public PersistEvent getEvent(){
		try {
			lock.tryLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			System.out.println("wait run mode");
			persistRun.await();
			return queue.take();
		} catch (InterruptedException e) {
			LogUtil.failure(LOG, Level.ERROR, e, "Interrupted");
		}finally {
			lock.unlock();
		}
		return null;
	}
	//
	public String getRunName(){
		return runName;
	}
}
