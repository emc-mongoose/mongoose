package com.emc.mongoose.base.config.el;

import static java.lang.Long.reverse;
import static java.lang.Long.reverseBytes;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.commons.io.el.SynchronousExpressionInput;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ExpressionInputTest {

  @Test
  public void test() throws Exception {
    final var in =
        (AsyncExpressionInput<Long>)
            ExpressionInputBuilder.newInstance()
                .expression("#{time:millisSinceEpoch()}")
                .type(long.class)
                .<Long, ExpressionInput<Long>>build();
    assertNull(in.get());
    in.start();
    TimeUnit.MILLISECONDS.sleep(100);
    assertTrue(100 >= currentTimeMillis() - in.get());
    TimeUnit.MILLISECONDS.sleep(100);
    assertTrue(100 >= currentTimeMillis() - in.get());
    TimeUnit.MILLISECONDS.sleep(100);
    assertTrue(100 >= currentTimeMillis() - in.get());
    in.stop();
    final var last = in.get();
    TimeUnit.MILLISECONDS.sleep(100);
    assertEquals(last, in.get());
    TimeUnit.MILLISECONDS.sleep(100);
    assertEquals(last, in.get());
    in.close();
    assertNull(in.get());
  }

  @Test
  public void testRandomItemId() throws Exception {
    final var radix = 36;
    final var length = 10;
    final var offsetInput =
        ExpressionInputBuilder.newInstance()
            .expression("${math:absInt64(math:xorShift64(this.last()) % math:pow(radix, length))}")
            .type(long.class)
            .value("radix", radix, int.class)
            .value("length", length, int.class)
            .initial(Math.abs(reverse(currentTimeMillis()) ^ reverseBytes(nanoTime())))
            .<Long, SynchronousExpressionInput<Long>>build();
    final var itemNameInput =
        ExpressionInputBuilder.newInstance()
            .expression("${int64:toString(offsetInput.get(), radix)}")
            .type(String.class)
            .value("offsetInput", offsetInput, SynchronousExpressionInput.class)
            .value("radix", radix, int.class)
            .<String, SynchronousExpressionInput<String>>build();
    final var id = itemNameInput.get();
    assertEquals(length, id.length());
    final var offset = offsetInput.last();
    assertTrue(Long.toString(offset, radix).endsWith(id));
  }
}
