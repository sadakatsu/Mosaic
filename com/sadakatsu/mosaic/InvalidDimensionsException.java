package com.sadakatsu.mosaic;

public class InvalidDimensionsException extends Exception {
	private static final long serialVersionUID = -867711546734711882L;

	public InvalidDimensionsException(
		int width,
		int height,
		int ul,
		int ur,
		int ll,
		int lr
	) {
		super(
			String.format(
				"The passed dimensions do not represent a valid Region: %d x " +
				"%d - (%d, %d, %d, %d).",
					width,
					height,
					ul,
					ur,
					ll,
					lr
			)
		);
	}
}
