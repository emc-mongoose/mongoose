package com.emc.mongoose.common.supply.async;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;

import java.text.Format;

/**
 * This class extends AsyncRangeGeneratorBase
 * because there are values that need a specified format to be converted to String.
 * Generators of values that do not need a format should extend AsyncRangeGeneratorBase.
 * For details see AsyncRangeGeneratorBase class.
 * @param <T> - type of value that is produced by the generator
 */
public abstract class AsyncFormattingRangeDefinedSupplierBase<T>
extends AsyncRangeDefinedSupplierBase<T> {

	private final Format outputFormat;

	/**
	 *
	 * @param minValue - a left border of the range
	 * @param maxValue - a right border of the range
	 * @param formatString - a format-containing string that is different for different implementations.
	 *                     To get examples see finished implementations (e.g. AsyncDoubleGenerator)
	 */
	protected AsyncFormattingRangeDefinedSupplierBase(
		final T minValue, final T maxValue, final String formatString
	) throws OmgDoesNotPerformException {
		super(minValue, maxValue);
		outputFormat = getFormatterInstance(formatString);
	}

	/**
	 *
	 * @param initialValue - an initial value of the generator
	 * @param formatString - a format-containing string that is different for different implementations.
	 *                     To get examples see finished implementations (e.g. AsyncDoubleGenerator)
	 */
	protected AsyncFormattingRangeDefinedSupplierBase(final T initialValue, final String formatString)
	throws OmgDoesNotPerformException {
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
