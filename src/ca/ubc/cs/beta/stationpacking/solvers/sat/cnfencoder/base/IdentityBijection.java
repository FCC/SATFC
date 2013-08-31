package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base;

/**
 * The identity function.
 * @author afrechet
 * @param <X> - function arguments type.
 */
public class IdentityBijection<X> implements IBijection<X, X>{
	
	@Override
	public X map(X aDomainElement) {
		return aDomainElement;
	}

	@Override
	public X inversemap(X aImageElement) {
		return aImageElement;
	}
}
