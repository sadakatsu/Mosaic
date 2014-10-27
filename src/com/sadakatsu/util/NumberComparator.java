package com.sadakatsu.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;

public class NumberComparator<T extends Number> implements Comparator<T> {
	public int compare(T a, T b) {
		return performComparison(a, b);
	}
	
	public static <T extends Number> int performComparison(T a, T b) {
		if (a instanceof BigDecimal) {
			return ((BigDecimal) a).compareTo((BigDecimal) b);
		} else if (a instanceof BigInteger) {
			return ((BigInteger) a).compareTo((BigInteger) b);
		} else if (a instanceof Byte) {
			return ((Byte) a).compareTo((Byte) b);
		} else if (a instanceof Double) {
			return ((Double) a).compareTo((Double) b);
		} else if (a instanceof Float) {
			return ((Float) a).compareTo((Float) b);
		} else if (a instanceof Integer) {
			return ((Integer) a).compareTo((Integer) b);
		} else if (a instanceof Long) {
			return ((Long) a).compareTo((Long) b);
		} else if (a instanceof Short) {
			return ((Short) a).compareTo((Short) b);
		}
		
		throw new UnsupportedOperationException();
	}
}
