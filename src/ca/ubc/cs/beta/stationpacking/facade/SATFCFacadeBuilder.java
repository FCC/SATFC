package ca.ubc.cs.beta.stationpacking.facade;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Builder in charge of creating a SATFC facade, feeding it the necessary options.
 * @author afrechet
 */
public class SATFCFacadeBuilder {
	
	private boolean fInitializeLogging;
	private String fLibrary;
	
	public SATFCFacadeBuilder()
	{
		fInitializeLogging = false;
		fLibrary = findSATFCLibrary();
		if(fLibrary != null)
		{
			System.out.println("Found default library "+fLibrary);
		}
	}
	
	private String findSATFCLibrary()
	{
		
		String relativeLibPath = "clasp"+File.separator+"jna"+File.separator+"libjnaclasp";
		
		String osName = System.getProperty("os.name").toLowerCase();
		boolean isMacOs = osName.startsWith("mac os x");
		if(isMacOs)
		{
			relativeLibPath+=".dylib";
		}
		else
		{
			relativeLibPath+=".so";
		}
		
		//SATFCFacadeBuilder.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		
		URL url = SATFCFacadeBuilder.class.getProtectionDomain().getCodeSource().getLocation();
		File f;
		try {
		  f = new File(url.toURI());
		} catch(URISyntaxException e) {
		  f = new File(url.getPath());
		}
		
		String currentLocation = f.isDirectory() ? f.getAbsolutePath() : f.getAbsoluteFile().getParentFile().getAbsolutePath(); 
		
		File file = new File(currentLocation + File.separator + relativeLibPath);
		if(file.exists())
		{
			return file.getAbsolutePath();
		}
		else
		{
			System.err.println("Did not find SATFC library at "+file.getAbsolutePath());
		}
		
		
		return null;
	}
	
	/**
	 * Build a SATFC facade with the builder's options. These are either the default options (if available), or the ones provided with the
	 * builder's setters.
	 * @return a SATFC facade configured according to the builder's options.
	 */
	public SATFCFacade build()
	{
		if(fLibrary == null)
		{
			throw new IllegalArgumentException("Facade builder did not auto-detect default library, and no other library was provided.");
		}
		
		return new SATFCFacade(fLibrary, fInitializeLogging);
	}
	
	public void setInitializeLogging(boolean aInitializeLogging)
	{
		fInitializeLogging = aInitializeLogging;
	}
	
	public void setLibrary(String aLibrary)
	{
		if(aLibrary == null)
		{
			throw new IllegalArgumentException("Cannot provide a null library.");
		}
		
		fLibrary = aLibrary;
	}
	
	
	
	
}
