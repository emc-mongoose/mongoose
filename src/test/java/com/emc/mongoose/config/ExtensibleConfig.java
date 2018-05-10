package com.emc.mongoose.config;

import com.github.akurilov.commons.collection.InvalidRangeException;
import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.system.SizeInBytes;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public interface ExtensibleConfig {

	Object val(final String path)
	throws InvalidValuePathException, NoSuchElementException;

	String stringVal(final String path)
	throws InvalidValuePathException, NoSuchElementException;

	boolean boolVal(final String path)
	throws InvalidValuePathException, NoSuchElementException;

	int intVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, NumberFormatException;

	long longVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, NumberFormatException;

	double doubleVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, NumberFormatException;

	<V> Map<String, V> mapVal(final String path)
	throws InvalidValuePathException, NoSuchElementException;

	<E> List<E> listVal(final String path)
	throws InvalidValuePathException, NoSuchElementException;

	Range rangeVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, InvalidRangeException;

	SizeInBytes sizeVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, NumberFormatException,
		IllegalArgumentException;

	long secondsVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, NumberFormatException,
		IllegalArgumentException;
}
