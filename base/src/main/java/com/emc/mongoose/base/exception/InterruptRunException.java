package com.emc.mongoose.base.exception;

/**
 * Should be thrown in case of catching {@code java.lang.InterruptedException}. Extends the {@code
 * java.lang.RuntimeException} in order to be passed on the uppermost level of the stacktrace.
 */
public class InterruptRunException extends RuntimeException {
  public InterruptRunException(final Throwable cause) {
    super(cause);
  }
}
