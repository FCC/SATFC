package ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver;

import java.net.InetAddress;

import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Container class for a solving job.
 * @author afrechet
 */
public class SolvingJob {

	private final String fID;
	
	private final String fDataFolderName;
	private final String fInstanceString;
	private final ITerminationCriterion fTerminationCriterion;
	
	private final long fSeed;
	
	private final InetAddress fSendAddress;
	private final int fSendPort;
	
	public SolvingJob(String aID, String aDataFolderName, String aInstanceString, ITerminationCriterion aTerminationCriterion, long aSeed, InetAddress aSendAddress, int aSendPort)
	{
		fID = aID;
		fDataFolderName = aDataFolderName;
		fInstanceString = aInstanceString;
		fTerminationCriterion = aTerminationCriterion;
		fSeed = aSeed;
		fSendAddress = aSendAddress;
		fSendPort = aSendPort;
	}

	public String getID() {
		return fID;
	}

	public String getDataFolderName() {
		return fDataFolderName;
	}

	public String getInstanceString() {
		return fInstanceString;
	}

	public ITerminationCriterion getTerminationCriterion() {
		return fTerminationCriterion;
	}

	public long getSeed() {
		return fSeed;
	}

	public InetAddress getSendAddress() {
		return fSendAddress;
	}

	public int getSendPort() {
		return fSendPort;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fDataFolderName == null) ? 0 : fDataFolderName.hashCode());
		result = prime * result + ((fID == null) ? 0 : fID.hashCode());
		result = prime * result
				+ ((fInstanceString == null) ? 0 : fInstanceString.hashCode());
		result = prime * result + (int) (fSeed ^ (fSeed >>> 32));
		result = prime * result
				+ ((fSendAddress == null) ? 0 : fSendAddress.hashCode());
		result = prime * result + fSendPort;
		result = prime
				* result
				+ ((fTerminationCriterion == null) ? 0 : fTerminationCriterion
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SolvingJob other = (SolvingJob) obj;
		if (fDataFolderName == null) {
			if (other.fDataFolderName != null)
				return false;
		} else if (!fDataFolderName.equals(other.fDataFolderName))
			return false;
		if (fID == null) {
			if (other.fID != null)
				return false;
		} else if (!fID.equals(other.fID))
			return false;
		if (fInstanceString == null) {
			if (other.fInstanceString != null)
				return false;
		} else if (!fInstanceString.equals(other.fInstanceString))
			return false;
		if (fSeed != other.fSeed)
			return false;
		if (fSendAddress == null) {
			if (other.fSendAddress != null)
				return false;
		} else if (!fSendAddress.equals(other.fSendAddress))
			return false;
		if (fSendPort != other.fSendPort)
			return false;
		if (fTerminationCriterion == null) {
			if (other.fTerminationCriterion != null)
				return false;
		} else if (!fTerminationCriterion.equals(other.fTerminationCriterion))
			return false;
		return true;
	}
	
	

}
