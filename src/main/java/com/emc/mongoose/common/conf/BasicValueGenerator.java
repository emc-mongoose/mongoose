package com.emc.mongoose.common.conf;
//
import com.emc.mongoose.common.log.LogUtil;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.Callable;
/**
 Created by kurila on 10.02.16.
 */
public class BasicValueGenerator<T>
implements ValueGenerator<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected T lastValue = null;
	protected Callable<T> updateAction;
	//
	public BasicValueGenerator(final T initialValue, final Callable<T> updateAction) {
		lastValue = initialValue;
		this.updateAction = updateAction;
	}
	//
	@Override
	public T get() {
		try {
			lastValue = updateAction.call();
		} catch(final Exception e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to execute the update action \"{}\"", updateAction
			);
		}
		return lastValue;
	}
}
