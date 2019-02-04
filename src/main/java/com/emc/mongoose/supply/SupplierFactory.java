package com.emc.mongoose.supply;

import static com.emc.mongoose.supply.RangeDefinedSupplier.RANGE_DELIMITER;

import com.emc.mongoose.exception.OmgShootMyFootException;
import java.util.regex.Pattern;

@FunctionalInterface
public interface SupplierFactory<T, G extends BatchSupplier<T>> {

  // pay attention to the matcher groups
  String DOUBLE_REG_EXP = "([-+]?\\d*\\.?\\d+)";
  String LONG_REG_EXP = "([-+]?\\d+)";
  String DATE_REG_EXP = "(((19|20)[0-9][0-9])/(1[012]|0?[1-9])/(3[01]|[12][0-9]|0?[1-9]))";

  // Pay attention to the escape symbols
  static String rangeRegExp(final String typeRegExp) {
    return typeRegExp + RANGE_DELIMITER + typeRegExp;
  }

  Pattern DOUBLE_PATTERN = Pattern.compile(rangeRegExp(DOUBLE_REG_EXP));
  Pattern LONG_PATTERN = Pattern.compile(rangeRegExp(LONG_REG_EXP));
  Pattern DATE_PATTERN = Pattern.compile(rangeRegExp(DATE_REG_EXP));

  String[] INPUT_DATE_FMT_STRINGS = new String[] {"yyyy/MM/dd", "yyyy/MM/dd'T'HH:mm:ss"};

  enum State {
    EMPTY,
    RANGE,
    FORMAT,
    FORMAT_RANGE
  }

  G createSupplier(
      final char type, final String seedStr, final String formatStr, final String rangeStr)
      throws OmgShootMyFootException;
}
