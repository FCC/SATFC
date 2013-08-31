package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base;

/**
 * Interface for a bijective function.
 * @author afrechet
 */
public interface IBijection<X,Y> {

	public Y map(X aDomainElement);
	
	public X inversemap(Y aImageElement);
	
	
}
