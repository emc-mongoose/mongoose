package com.emc.mongoose.base.config.el;

import static com.emc.mongoose.base.config.el.ExpressionInputBuilderImpl.INITIAL_VALUE_PATTERN;
import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.commons.io.el.SynchronousExpressionInput;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ExpressionInputTest {

  @Test
  public void testBuiltinCall() throws Exception {
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
    final var init =
        "%{math:absInt64(int64:xor(int64:reverse(time:millisSinceEpoch()), int64:reverseBytes(time:nanos())))}";
    final var expr = "${math:absInt64(math:xorShift64(this.last()) % math:pow(radix, length))}";
    final var offsetInput =
        ExpressionInputBuilder.newInstance()
            .expression(init + expr)
            .type(long.class)
            .value("radix", radix, int.class)
            .value("length", length, int.class)
            .<Long, SynchronousExpressionInput<Long>>build();
    final var itemNameInput =
        ExpressionInputBuilder.newInstance()
            .expression("${int64:toString(offsetInput.get(), radix)}")
            .type(String.class)
            .value("offsetInput", offsetInput, SynchronousExpressionInput.class)
            .value("radix", radix, int.class)
            .<String, SynchronousExpressionInput<String>>build();
    final var id = itemNameInput.get();
    assertTrue(length >= id.length());
    final var offset = offsetInput.last();
    assertTrue(Long.toString(offset, radix).endsWith(id));
  }

  @Test
  public void testVararg() throws Exception {
    final var inputBuilder = ExpressionInputBuilder.newInstance().type(String.class);
    var in =
        inputBuilder
            .expression("${string:join('_', 'a')}")
            .<String, ExpressionInput<String>>build();
    assertEquals("a", in.get());
    in.close();
    in = inputBuilder.expression("${string:join('_', 'a', 'b')}").build();
    assertEquals("a_b", in.get());
    in.close();
  }

  @Test
  public void testPaths() throws Exception {
    final var exprPathInput =
        ExpressionInputBuilder.newInstance()
            .type(String.class)
            .expression("/${path:random(16, 2)}")
            .build();
    for (var i = 0; i < 100; i++) {
      System.out.println(exprPathInput.get());
    }
  }

  @Test
  public void testInitialPattern() throws Exception {
    final var withInitVal = "prefix_%{-1}${this.last() + 1}suffix";
    var m = INITIAL_VALUE_PATTERN.matcher(withInitVal);
    assertTrue(m.find());
    assertEquals("-1", m.group(1));
    assertEquals("${this.last() + 1}", m.group(2));
    final var noInitVal = "prefix_${this.last() + 1}suffix";
    m = INITIAL_VALUE_PATTERN.matcher(noInitVal);
    assertFalse(m.find());
  }
}
