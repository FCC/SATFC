package ca.ubc.cs.beta.stationpacking.utils;

import java.util.concurrent.ExecutorService;
import java.lang.Thread.UncaughtExceptionHandler;

public class RunnableUtils{
	
	/**
	 * Submit a runnable to an executor service so that any uncaught exception will be processed by the uncaught exception handler.
	 * @param aExecutorService - an executor service to run a runnable.
	 * @param aUncaughtExceptionHandler - an uncaught exception handler to manager uncaught exceptions in the runnable's executions.
	 * @param r - a runnable to run.
	 */
	public static void submitRunnable(final ExecutorService aExecutorService, final  UncaughtExceptionHandler aUncaughtExceptionHandler, final Runnable r)
	{
		aExecutorService.submit(
				new Runnable() {
					@Override
					public void run() {
						try {
							r.run();
						}  catch(Throwable t)
						{
							aUncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
						}
					}
							
				});
	}

}
