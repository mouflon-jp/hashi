package com.github.mouflon_jp.jfiap.ieee1888;

import java.io.IOException;

public class FIAPException extends IOException {

	public FIAPException() {
		super();
	}

	public FIAPException(String message, Throwable cause) {
		super(message, cause);
	}

	public FIAPException(String message) {
		super(message);
	}

	public FIAPException(Throwable cause) {
		super(cause);
	}

}
