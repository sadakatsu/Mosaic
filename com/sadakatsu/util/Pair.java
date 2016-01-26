package com.sadakatsu.util;

// This is a modified form of a class I saw at http://goo.gl/ryy0Wl .  I had
// written most of the class myself when I saw - and liked - that class's
// "createPair" method.  I like the idea of saying "Pair.make(a, b)" as opposed
// to "new Pair<>(a, b)".
public class Pair<T, U> {
	private final T first;
	private final U second;
	
	public static <T, U> Pair<T, U> make(T first, U second) {
        return new Pair<>(first, second);
    }
	
	public Pair(T first, U second) {
		this.first = first;
		this.second = second;
	}
	
	public T getFirst() {
		return first;
	}
	
	public U getSecond() {
		return second;
	}
	
	public String toString() {
		return String.format("(%s, %s)", first, second);
	}
}
