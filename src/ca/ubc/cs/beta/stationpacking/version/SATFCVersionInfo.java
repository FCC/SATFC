package ca.ubc.cs.beta.stationpacking.version;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aeatk.misc.version.AbstractVersionInfo;
import ca.ubc.cs.beta.aeatk.misc.version.VersionInfo;

@ProviderFor(VersionInfo.class)
public class SATFCVersionInfo extends AbstractVersionInfo {

	public SATFCVersionInfo() {
		super("SATFC", "satfc-version.txt", true);
	}

}
