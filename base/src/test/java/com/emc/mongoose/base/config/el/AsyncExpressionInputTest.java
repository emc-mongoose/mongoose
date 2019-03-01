package com.emc.mongoose.base.config.el;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.akurilov.commons.io.el.ExpressionInput;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class AsyncExpressionInputTest {

  @Test
  public void test() throws Exception {
    final var in =
        ExpressionInput.<Long>builder()
            .expr("#{time:millisSinceEpoch()}")
            .type(long.class)
            .func("time", "millisSinceEpoch", System.class.getMethod("currentTimeMillis"))
            .build();
    final var asyncInput = new AsyncExpressionInputImpl<>(in);
    assertNull(in.get());
    asyncInput.start();
    TimeUnit.MILLISECONDS.sleep(100);
    assertTrue(100 >= System.currentTimeMillis() - in.get());
    TimeUnit.MILLISECONDS.sleep(100);
    assertTrue(100 >= System.currentTimeMillis() - in.get());
    TimeUnit.MILLISECONDS.sleep(100);
    assertTrue(100 >= System.currentTimeMillis() - in.get());
    asyncInput.stop();
    final var last = in.get();
    TimeUnit.MILLISECONDS.sleep(100);
    assertEquals(last, in.get());
    TimeUnit.MILLISECONDS.sleep(100);
    assertEquals(last, in.get());
    asyncInput.close();
    assertNull(in.get());
  }
}
