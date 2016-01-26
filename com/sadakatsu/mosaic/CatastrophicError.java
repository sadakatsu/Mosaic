package com.sadakatsu.mosaic;

public class CatastrophicError extends Error {
	private static final long serialVersionUID = -799841096412479687L;

	public CatastrophicError() {}
	
	public CatastrophicError(String message) {
		super(message);
	}
	
	public CatastrophicError(Throwable cause) {
		super(cause);
	}
}
