package com.emc.mongoose.supply.async;

import static org.apache.commons.lang.time.DateUtils.parseDate;

import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.supply.BatchSupplier;
import com.emc.mongoose.supply.SupplierFactory;
import com.github.akurilov.fiber4j.FibersExecutor;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;

public final class AsyncStringSupplierFactory<G extends BatchSupplier<String>>
    implements SupplierFactory<String, G> {

  private static final AsyncStringSupplierFactory<? extends BatchSupplier<String>> INSTANCE =
      new AsyncStringSupplierFactory<>();

  private FibersExecutor executor;

  private AsyncStringSupplierFactory() {}

  public static AsyncStringSupplierFactory<? extends BatchSupplier<String>> getInstance(
      final FibersExecutor executor) {
    return INSTANCE.setFibersExecutor(executor);
  }

  private AsyncStringSupplierFactory<? extends BatchSupplier<String>> setFibersExecutor(
      final FibersExecutor executor) {
    this.executor = executor;
    return this;
  }

  /**
   * @param type - a type of the generator
   * @return a suitable generator
   */
  @Override
  @SuppressWarnings("unchecked")
  public final G createSupplier(
      final char type, final String seedStr, final String formatStr, final String rangeStr)
      throws OmgShootMyFootException {
    long seed = System.nanoTime() ^ System.currentTimeMillis();
    if (seedStr != null && !seedStr.isEmpty()) {
      try {
        seed = Long.parseLong(seedStr);
      } catch (final NumberFormatException e) {
        throw new OmgShootMyFootException(
            "Seed value is not a 64 bit integer: \"" + seedStr + "\"");
      }
    }

    switch (type) {
      case 'd':
        {
          long min = Long.MIN_VALUE;
          long max = Long.MAX_VALUE;
          if (rangeStr != null && !rangeStr.isEmpty()) {
            final Matcher matcher = LONG_PATTERN.matcher(rangeStr);
            if (matcher.find()) {
              min = Long.parseLong(matcher.group(1));
              max = Long.parseLong(matcher.group(2));
            } else {
              throw new OmgShootMyFootException();
            }
          }
          return (G)
              new AsyncRangeDefinedLongFormattingSupplier(executor, seed, min, max, formatStr);
        }

      case 'f':
        {
          double min = 0;
          double max = 1;
          if (rangeStr != null && !rangeStr.isEmpty()) {
            final Matcher matcher = DOUBLE_PATTERN.matcher(rangeStr);
            if (matcher.find()) {
              min = Double.parseDouble(matcher.group(1));
              max = Double.parseDouble(matcher.group(2));
            } else {
              throw new OmgShootMyFootException();
            }
          }
          return (G)
              new AsyncRangeDefinedDoubleFormattingSupplier(executor, seed, min, max, formatStr);
        }

      case 'D':
        {
          Date min = new Date(0);
          Date max = new Date();
          if (rangeStr != null && !rangeStr.isEmpty()) {
            final Matcher matcher = DATE_PATTERN.matcher(rangeStr);
            if (matcher.find()) {
              try {
                min = parseDate(matcher.group(1), INPUT_DATE_FMT_STRINGS);
                max = parseDate(matcher.group(6), INPUT_DATE_FMT_STRINGS);
              } catch (final ParseException e) {
                throw new OmgShootMyFootException("Failed to parse the pattern");
              }
            } else {
              throw new OmgShootMyFootException();
            }
          }
          return (G)
              new AsyncRangeDefinedDateFormattingSupplier(executor, seed, min, max, formatStr);
        }

      default:
        throw new OmgShootMyFootException("Unknown format type: '" + type + "'");
    }
  }
}
