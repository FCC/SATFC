package ca.ubc.cs.beta.stationpacking.solver.reporters;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solver.SolverResult;

public class AsynchronousLocalExperimentReporter implements IExperimentReporter {
	
	public class AsynchronousResult
	{	
		private StationPackingInstance fInstance;
		private SolverResult fSolverResult;
		
		public AsynchronousResult(StationPackingInstance aInstance, SolverResult aSolverResult)
		{
			fInstance = aInstance;
			fSolverResult = aSolverResult;
		}
		
		public StationPackingInstance getInstance()
		{
			return fInstance;
		}
		
		public SolverResult getResult()
		{
			return fSolverResult;
		}	
		
		@Override
		public String toString()
		{
			return fInstance.toString()+" = "+fSolverResult.toString();
		}
	}
	
	//Poison pill queue objects serves as a signal to stop the report writing thread.
	private final AsynchronousResult POISON_PILL = new AsynchronousResult(null, null);
	
	private final LinkedBlockingQueue<AsynchronousResult> fReportQueue;
	private final ExecutorService fThreadPool;
	
	private String fReportDirectory;
	private String fExperimentName;
	
	private static Logger log = LoggerFactory.getLogger(AsynchronousLocalExperimentReporter.class);
	
	public AsynchronousLocalExperimentReporter(String aReportDirectory, String aExperimentName)
	{
		fReportDirectory = aReportDirectory;
		fExperimentName = aExperimentName;
		
		fReportQueue = new LinkedBlockingQueue<AsynchronousResult>();
		fThreadPool = Executors.newSingleThreadExecutor(new SequentiallyNamedThreadFactory("Report Writer"));
	}

	@Override
	public void report(StationPackingInstance aInstance, SolverResult aRunResult){
		
		
		for(int interruptcount=0;interruptcount<10;interruptcount++)
		{
			try
			{
				fReportQueue.put(new AsynchronousResult(aInstance, aRunResult));
				return;
			}
			catch(InterruptedException e)
			{
				log.warn("Asynchronous reporter was interrupted when queuing result {} for instance {}. Trying again... ",aRunResult,aInstance);
			}
		}	
		throw new IllegalStateException("Asynchronous reporter was interrupted too many times when trying to queue a result.");
	}
	
	public void stopWritingReport()
	{
		log.info("Signaling the report should stop being written...");
		try {
			fReportQueue.put(POISON_PILL);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		
		return;
	}
	
	public void startWritingReport()
	{
		log.info("Starting to write the report.");
		fThreadPool.execute(new Runnable(){
			
			@Override
			public void run() {
				
				try {
					while(true)
					{
						try 
						{
							AsynchronousResult aResult = fReportQueue.take();
							//Checking if we're pulling the poison pill and must die.
							if(aResult == POISON_PILL)
							{
								log.info("Report writing halt signal caught, stopping.");
								fThreadPool.shutdown();
								return;
							}
							log.debug("Got result to write to disk {}",aResult);
							//Write object to file 
							String aLine = aResult.getInstance().toString()+","+aResult.getResult().toString()+"\n";
							
							try 
							{
								FileUtils.writeStringToFile(new File(fReportDirectory+File.separatorChar+fExperimentName+".csv"),aLine, true);
							} 
							catch (IOException e) 
							{
								log.error("Exception",e);
								throw new IllegalStateException("Writing experiment reporter string to file failed for some reason.",e);
							}
						} 
						catch (InterruptedException e) 
						{
							Thread.currentThread().interrupt();
							return;
						}
					}
				} finally
				{
					if (!fThreadPool.isShutdown())
					{	
						//This actually is only maybe a bug, if the thread got shutdown some other way. Ask Steve or read JCIP probably.
						System.err.print("Thread pool not shutdown but Report Writer is terminating, this is most likely a bug of some kind.");
						log.error("Thread pool not shutdown but Report Writer is terminating, this is most likely a bug of some kind.");
					}	
					
		
				}
			}
			
		});
	}
	
}
