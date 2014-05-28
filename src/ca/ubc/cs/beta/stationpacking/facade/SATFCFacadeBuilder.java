package ca.ubc.cs.beta.stationpacking.facade;

import java.io.File;

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
	}
	
	private String findSATFCLibrary()
	{
		String relativeLibPath;
		String osName = System.getProperty("os.name").toLowerCase();
		boolean isMacOs = osName.startsWith("mac os x");
		if(isMacOs)
		{
			relativeLibPath = "SATsolvers"+File.separator+"clasp"+File.separator+"jna"+File.separator+"libjnaclasp.dylib";
		}
		else
		{
			relativeLibPath = "SATsolvers"+File.separator+"clasp"+File.separator+"jna"+File.separator+"libjnaclasp.so";
		}
		
		String currentLocation = SATFCFacadeBuilder.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		currentLocation.replaceFirst(File.separator+"bin"+File.separator, "");
		
		File file = new File(currentLocation + File.separator + relativeLibPath);
		if(file.exists())
		{
			return file.getAbsolutePath();
		}
		else
		{
			System.out.println("Did not find SATFC library at "+file.getAbsolutePath());
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
