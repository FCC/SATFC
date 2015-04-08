/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.utils;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ExecutorService;

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
