package com.bobgettys.jsh;

public class ShellException extends Exception {

	private static final long serialVersionUID = 1L;

	public ShellException() {
	}

	public ShellException(String message) {
		super(message);
	}

	public ShellException(Throwable cause) {
		super(cause);
	}

	public ShellException(String message, Throwable cause) {
		super(message, cause);
	}

}
