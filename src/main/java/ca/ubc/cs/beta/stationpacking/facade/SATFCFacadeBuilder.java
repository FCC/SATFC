/**
 * Copyright 2014, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.facade;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import lombok.NonNull;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter.SolverChoice;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter.SolverCustomizationOptions;

/**
 * Builder in charge of creating a SATFC facade, feeding it the necessary options.
 * @author afrechet
 */
public class SATFCFacadeBuilder {
	
	private boolean fInitializeLogging;
	private String fLibrary;
	private String fCNFDirectory;
	private String fResultFile;
	private SATFCFacadeParameter.SolverChoice fSolverChoice;
	private SATFCFacadeParameter.SolverCustomizationOptions fCustomizationOptions;

	/**
	 * Create a SATFCFacadeBuilder with the default parameters - no logging initialized, autodetected clasp library, no saving of CNFs and results.
	 */
	public SATFCFacadeBuilder()
	{
		fInitializeLogging = false;
		fLibrary = findSATFCLibrary();
		fCNFDirectory = null;
		fResultFile = null;
		fSolverChoice = SolverChoice.SATFC;
		fCustomizationOptions = new SolverCustomizationOptions();
	}
	
	/**
	 * Some autodetection magic to find clasp library.
	 * @return the path to the detected clasp library, null if none found.
	 */
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
		String currentLocation;
		
		if(f.isDirectory())
		{
			if(f.getName().equals("bin")) {
				// eclipse
				currentLocation = new File(f.getParentFile(),"src"+File.separator+"dist").getAbsolutePath();
			} else {
				// intellij
				currentLocation = new File(f.getParentFile().getParentFile().getParentFile(), "src"+File.separator+"dist").getAbsolutePath();
			}

		}
		else
		{
			//Deployed, probably under the gradle install build structure.
			currentLocation = f.getParentFile().getParentFile().getAbsolutePath();
		}
		
		File file = new File(currentLocation + File.separator + relativeLibPath);
		if(file.exists())
		{
			System.out.println("Found default library "+file.getAbsolutePath()+".");
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

		return new SATFCFacade(new SATFCFacadeParameter(fLibrary, fInitializeLogging, fCNFDirectory, fResultFile, fSolverChoice, fCustomizationOptions));
	}
	
	/**
	 * Set whether SATFC should initialize the logging on construction.
	 * @param aInitializeLogging
	 */
	public void setInitializeLogging(boolean aInitializeLogging)
	{
		fInitializeLogging = aInitializeLogging;
	}
	
	/**
	 * Set the (clasp) library SATFC should use.
	 * @param aLibrary
	 */
	public void setLibrary(String aLibrary)
	{
		if(aLibrary == null)
		{
			throw new IllegalArgumentException("Cannot provide a null library.");
		}
		
		fLibrary = aLibrary;
	}
	
	/**
	 * Set the directory where SATFC should save encountered CNFs. Setting this to non-null will incur a performance penalty.
	 * @param aCNFDirectory
	 */
	public void setCNFDirectory(String aCNFDirectory)
	{
		fCNFDirectory = aCNFDirectory;
	}
	
	/**
	 * Set the file in which SATFC writes encountered problem/results pairs.
	 * @param aResultFile
	 */
	public void setResultFile(String aResultFile)
	{
		fResultFile = aResultFile;
	}
	
	/**
	 * Set the type of solver choice to use in SATFC.
	 * @param aSolverChoice
	 */
	public void setSolverChoice(SATFCFacadeParameter.SolverChoice aSolverChoice)
	{
		fSolverChoice = aSolverChoice;
	}
	
	public void setCustomizationOptions(@NonNull SolverCustomizationOptions aOptions) {
		fCustomizationOptions = aOptions;
	}


}
