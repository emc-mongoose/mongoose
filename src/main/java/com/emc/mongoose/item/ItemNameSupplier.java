package com.emc.mongoose.item;

import static java.lang.Long.reverse;
import static java.lang.Long.reverseBytes;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;

import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.supply.ValueUpdatingSupplier;
import com.github.akurilov.commons.math.MathUtil;

/** Created by kurila on 18.12.15. */
public class ItemNameSupplier extends ValueUpdatingSupplier<String> implements IdStringInput {
  //
  protected final ItemNamingType namingType;
  protected final int length, prefixLength, radix;
  protected final long maxValue;
  protected final StringBuilder strb = new StringBuilder();
  //
  protected volatile long lastValue;
  //
  public ItemNameSupplier(
      final ItemNamingType namingType,
      final String prefix,
      final int length,
      final int radix,
      final long offset)
      throws OmgShootMyFootException {
    //
    super(null, null);
    //
    if (namingType != null) {
      this.namingType = namingType;
    } else {
      this.namingType = ItemNamingType.RANDOM;
    }
    //
    if (prefix != null) {
      strb.append(prefix);
      this.prefixLength = prefix.length();
      if (length > prefix.length()) {
        this.length = length;
      } else {
        throw new OmgShootMyFootException("Id length should be more than prefix length");
      }
    } else {
      prefixLength = 0;
      if (length > 0) {
        this.length = length;
      } else {
        throw new OmgShootMyFootException("Id length should be more than 0");
      }
    }
    //
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      throw new OmgShootMyFootException("Invalid radix: " + radix);
    }
    this.radix = radix;
    final double _maxValue = Math.pow(this.radix, this.length) - 1;
    if (Long.MAX_VALUE < _maxValue) {
      this.maxValue = Long.MAX_VALUE;
    } else {
      this.maxValue = (long) _maxValue;
    }
    // xorShift(0) = 0, so override this behaviour (which is by default)
    if (ItemNamingType.RANDOM.equals(namingType) && offset == 0) {
      this.lastValue = reverse(currentTimeMillis()) ^ reverseBytes(nanoTime()) % (maxValue + 1);
    } else {
      this.lastValue = offset % (maxValue + 1);
    }
  }
  //
  /**
   * Generate the next item name. Not thread safe.
   *
   * @return next name
   */
  @Override
  public String get() {
    // reset the string buffer
    strb.setLength(prefixLength);
    // calc next number
    switch (namingType) {
      case RANDOM:
        lastValue = Math.abs(MathUtil.xorShift(lastValue) % (maxValue + 1));
        break;
      case ASC:
        if (lastValue < maxValue) {
          lastValue++;
        } else {
          lastValue = 0;
        }
        break;
      case DESC:
        if (lastValue > 0) {
          lastValue--;
        } else {
          lastValue = maxValue;
        }
        break;
    }
    //
    final String numStr = Long.toString(lastValue, radix);
    final int nZeros = length - prefixLength - numStr.length();
    if (nZeros > 0) {
      for (int i = 0; i < nZeros; i++) {
        strb.append('0');
      }
      strb.append(numStr);
    } else if (nZeros < 0) {
      strb.append(numStr.substring(-nZeros));
    } else {
      strb.append(numStr);
    }
    return strb.toString();
  }

  @Override
  public final long getAsLong() {
    return lastValue;
  }
}
