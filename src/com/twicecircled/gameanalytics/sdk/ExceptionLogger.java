package com.twicecircled.gameanalytics.sdk;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

public class ExceptionLogger implements UncaughtExceptionHandler {

	private final UncaughtExceptionHandler defaultHandler;

	public ExceptionLogger() {
		defaultHandler = Thread.currentThread().getUncaughtExceptionHandler();
	}

	public void uncaughtException(Thread thread, Throwable ex) {
		// Get stack trace associated with exceptions as String
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);

		// Find root cause
		Throwable cause = ex.getCause();
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}

		// Log the error
		GameAnalytics.newQualityEvent(
				"Exception:" + cause.getClass().getName(), ex.getMessage(),
				sw.toString(), 0, 0, 0);

		// Pass to default exception handler once data has been sent.
		defaultHandler.uncaughtException(thread, ex);
	}
}
