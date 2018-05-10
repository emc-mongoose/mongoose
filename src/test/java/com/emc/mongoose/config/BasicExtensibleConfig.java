package com.emc.mongoose.config;

import com.github.akurilov.commons.collection.InvalidRangeException;
import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.system.SizeInBytes;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public final class BasicExtensibleConfig
implements Externalizable, ExtensibleConfig {

	private final char pathSep;
	private final Map<String, Object> node;

	public BasicExtensibleConfig(final char pathSep, final Map<String, Object> node) {
		this.pathSep = pathSep;
		this.node = node;
	}

	public BasicExtensibleConfig(final BasicExtensibleConfig other) {
		this.pathSep = other.pathSep;
		this.node = other.node
			.entrySet()
			.stream()
			.collect(
				Collectors.toMap(
					Map.Entry::getKey,
					entry -> {
						final Object value = entry.getValue();
						if(value instanceof BasicExtensibleConfig) {
							return new BasicExtensibleConfig((BasicExtensibleConfig) value);
						} else {
							return value;
						}
					}
				)
			);
	}

	@Override
	public final Object val(final String path)
	throws InvalidValuePathException, NoSuchElementException {
		final int sepPos = path.indexOf(pathSep);
		if(sepPos == 0 || sepPos == path.length() - 1) {
			throw new InvalidValuePathException(path);
		}
		if(sepPos > 0) {
			final String key = path.substring(0, sepPos);
			final String subPath = path.substring(sepPos + 1);
			final Object child = node.get(key);
			if(child instanceof ExtensibleConfig) {
				try {
					return ((ExtensibleConfig) child).val(subPath);
				} catch(final InvalidValuePathException e) {
					throw new InvalidValuePathException(key + pathSep + e.getMessage());
				} catch(final NoSuchElementException e) {
					throw new NoSuchElementException(key + pathSep + e.getMessage());
				}
			} else {
				throw new NoSuchElementException(path);
			}
		} else {
			return node.get(path);
		}
	}

	@Override
	public final String stringVal(final String path)
	throws InvalidValuePathException, NoSuchElementException {
		return val(path).toString();
	}

	@Override
	public final boolean boolVal(final String path)
	throws InvalidValuePathException, NoSuchElementException {
		final Object v = val(path);
		if(v instanceof String) {
			return Boolean.parseBoolean((String) v);
		} else {
			try {
				return (boolean) v;
			} catch(final ClassCastException e) {
				throw new InvalidValueTypeException(path, Boolean.TYPE, v.getClass());
			}
		}
	}

	@Override
	public final int intVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, NumberFormatException {
		final Object v = val(path);
		if(v instanceof String) {
			return Integer.parseInt((String) v);
		} else {
			try {
				return (int) v;
			} catch(final ClassCastException e) {
				throw new InvalidValueTypeException(path, Integer.TYPE, v.getClass());
			}
		}
	}

	@Override
	public final long longVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, NumberFormatException {
		final Object v = val(path);
		if(v instanceof String) {
			return Long.parseLong((String) v);
		} else {
			try {
				return (long) v;
			} catch(final ClassCastException e) {
				throw new InvalidValueTypeException(path, Long.TYPE, v.getClass());
			}
		}
	}

	@Override
	public final double doubleVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, NumberFormatException {
		final Object v = val(path);
		if(v instanceof String) {
			return Double.parseDouble((String) v);
		} else {
			try {
				return (double) v;
			} catch(final ClassCastException e) {
				throw new InvalidValueTypeException(path, Double.TYPE, v.getClass());
			}
		}
	}

	@Override @SuppressWarnings("unchecked")
	public final <V> Map<String, V> mapVal(final String path)
	throws InvalidValuePathException, NoSuchElementException {
		final Object v = val(path);
		try {
			return (Map<String, V>) v;
		} catch(final ClassCastException e) {
			throw new InvalidValueTypeException(path, Map.class, v.getClass());
		}
	}

	@Override @SuppressWarnings("unchecked")
	public final <E> List<E> listVal(final String path)
	throws InvalidValuePathException, NoSuchElementException {
		final Object v = val(path);
		try {
			return (List<E>) v;
		} catch(final ClassCastException e) {
			throw new InvalidValueTypeException(path, List.class, v.getClass());
		}
	}

	@Override
	public final Range rangeVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, InvalidRangeException {
		final Object v = val(path);
		if(v instanceof String) {
			return new Range((String) v);
		} else {
			try {
				return (Range) v;
			} catch(final ClassCastException e) {
				throw new InvalidValueTypeException(path, Range.class, v.getClass());
			}
		}
	}

	@Override
	public final SizeInBytes sizeVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, NumberFormatException,
		IllegalArgumentException {
		final Object v = val(path);
		if(v instanceof String) {
			return new SizeInBytes((String) v);
		} else if(v instanceof Long || v instanceof Integer || v instanceof Short) {
			return new SizeInBytes((long) v);
		} else {
			try {
				return (SizeInBytes) v;
			} catch(final ClassCastException e) {
				throw new InvalidValueTypeException(path, SizeInBytes.class, v.getClass());
			}
		}
	}

	@Override
	public final long secondsVal(final String path)
	throws InvalidValuePathException, NoSuchElementException, NumberFormatException,
		IllegalArgumentException {
		final Object v = val(path);
		if(v instanceof String) {
			return TimeUtil.getTimeInSeconds((String) v);
		} else {
			try {
				return (long) v;
			} catch(final ClassCastException e) {
				throw new InvalidValueTypeException(path, Long.class, v.getClass());
			}
		}
	}

	@Override
	public final String toString() {

	}

	private void fromString(final String data) {

	}

	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		final byte[] bytes = toString().getBytes();
		final int length = bytes.length;
		out.writeInt(length);
		out.write(bytes);
	}

	@Override
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		final int length = in.readInt();
		final byte[] bytes = new byte[length];
		int offset = 0;
		while(offset < length) {
			offset += in.read(bytes, offset, length - offset);
		}
		final String data = new String(bytes);
		fromString(data);
	}
}
