package com.sadakatsu.mosaic;

@FunctionalInterface
public interface PerPixel {
	abstract void perform(Integer x, Integer y);
}
