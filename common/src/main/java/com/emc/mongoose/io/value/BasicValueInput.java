package com.emc.mongoose.io.value;
//
import com.emc.mongoose.io.Input;
import com.emc.mongoose.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
/**
 Created by kurila on 10.02.16.
 */
public class BasicValueInput<T>
implements Input<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final T initialValue;
	protected volatile T lastValue = null;
	private Callable<T> updateAction;
	//
	public BasicValueInput(final T initialValue, final Callable<T> updateAction) {
		this.initialValue = initialValue;
		this.updateAction = updateAction;
		try {
			reset();
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to reset the input {}", this.toString());
		}
	}

	//
	@Override
	public T get() {
		final T prevValue = lastValue;
		try {
			lastValue = updateAction.call();
		} catch(final Exception e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to execute the update action \"{}\"", updateAction
			);
		}
		return prevValue ;
	}
	//
	@Override
	public int get(final List<T> buffer, final int limit)
	throws IOException {
		int count = 0;
		try {
			for(; count < limit; count ++) {
				buffer.add(lastValue);
				lastValue = updateAction.call();
			}
		} catch(final Exception e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to execute the update action \"{}\"", updateAction
			);
		}
		return count;
	}
	//
	@Override
	public void skip(final long count)
	throws IOException {
		try {
			for(int i = 0; i < count; i++) {
				lastValue = updateAction.call();
			}
		} catch(final Exception e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to execute the update action \"{}\"", updateAction
			);
		}
	}
	//
	@Override
	public void reset()
	throws IOException {
		lastValue = initialValue;
	}
	//
	@Override
	public void close()
	throws IOException {
		// just free the memory
		lastValue = null;
		updateAction = null;
	}
}
