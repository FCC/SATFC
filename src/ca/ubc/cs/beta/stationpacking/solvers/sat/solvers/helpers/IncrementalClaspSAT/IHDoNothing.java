package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.helpers.IncrementalClaspSAT;

public class IHDoNothing implements InterruptHandler {

	@Override
	public void handle(InterruptedException e) throws InterruptedException {
		return;
	}

}
