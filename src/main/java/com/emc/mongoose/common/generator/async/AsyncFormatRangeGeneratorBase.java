package com.emc.mongoose.common.generator.async;

import java.text.Format;

/**
 * This class extends AsyncRangeGeneratorBase
 * because there are values that need a specified format to be converted to String.
 * Generators of values that do not need a format should extend AsyncRangeGeneratorBase.
 * For details see AsyncRangeGeneratorBase class.
 * @param <T> - type of value that is produced by the generator
 */
public abstract class AsyncFormatRangeGeneratorBase<T>
extends AsyncRangeGeneratorBase<T> {

	private final Format outputFormat;

	/**
	 *
	 * @param minValue - a left border of the range
	 * @param maxValue - a right border of the range
	 * @param formatString - a format-containing string that is different for different implementations.
	 *                     To get examples see finished implementations (e.g. AsyncDoubleGenerator)
	 */
	protected AsyncFormatRangeGeneratorBase(final T minValue, final T maxValue, final String formatString) {
		super(minValue, maxValue);
		outputFormat = getFormatterInstance(formatString);
	}

	/**
	 *
	 * @param initialValue - an initial value of the generator
	 * @param formatString - a format-containing string that is different for different implementations.
	 *                     To get examples see finished implementations (e.g. AsyncDoubleGenerator)
	 */
	protected AsyncFormatRangeGeneratorBase(final T initialValue, final String formatString) {
		super(initialValue);
		outputFormat = getFormatterInstance(formatString);
	}

	/**
	 * An implementation of this method should specify
	 * how to get a formatter instance with the format string.
	 * The formatter is needed to produce a String presentation of a generator value.
	 * @param formatString - a format-containing string that is different for different implementations.
	 *                     To get examples see finished implementations (e.g. AsyncDoubleGenerator)
	 * @return - the formatter that help to produce a String presentation of a generator value.
	 */
	protected abstract Format getFormatterInstance(final String formatString);

	protected Format outputFormat() {
		return outputFormat;
	}
}
