package com.sadakatsu.mosaic;

public class NotASliceException extends Exception {
	private static final long serialVersionUID = 2414699086911954863L;

	public NotASliceException() {
		super(
			"The passed Region was not an iteration slice.  add() and " +
			"subtract() require slices for arguments."
		);
	}
}
