package ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver;

import java.net.InetAddress;

public class SolvingJob {

	private final String fID;
	
	private final String fDataFolderName;
	private final String fInstanceString;
	private final double fCutOff;
	
	private final long fSeed;
	
	private final InetAddress fSendAddress;
	private final int fSendPort;
	
	public SolvingJob(String aID, String aDataFolderName, String aInstanceString, double aCutoff, long aSeed, InetAddress aSendAddress, int aSendPort)
	{
		fID = aID;
		fDataFolderName = aDataFolderName;
		fInstanceString = aInstanceString;
		fCutOff = aCutoff;
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

	public double getCutOff() {
		return fCutOff;
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
		long temp;
		temp = Double.doubleToLongBits(fCutOff);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((fDataFolderName == null) ? 0 : fDataFolderName.hashCode());
		result = prime * result + ((fID == null) ? 0 : fID.hashCode());
		result = prime * result
				+ ((fInstanceString == null) ? 0 : fInstanceString.hashCode());
		result = prime * result + (int) (fSeed ^ (fSeed >>> 32));
		result = prime * result
				+ ((fSendAddress == null) ? 0 : fSendAddress.hashCode());
		result = prime * result + fSendPort;
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
		if (Double.doubleToLongBits(fCutOff) != Double
				.doubleToLongBits(other.fCutOff))
			return false;
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
		return true;
	}

}
