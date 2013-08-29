package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.helpers.IncrementalClaspSAT;

public interface InterruptHandler 
{

	void handle(InterruptedException e) throws InterruptedException;
	
}
