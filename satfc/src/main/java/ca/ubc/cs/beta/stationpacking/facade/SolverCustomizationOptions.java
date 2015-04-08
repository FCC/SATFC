package ca.ubc.cs.beta.stationpacking.facade;

import lombok.Data;

/**
* Created by newmanne on 25/03/15.
*/
@Data
public class SolverCustomizationOptions {
    private boolean presolve = true;
    private boolean underconstrained = true;
    private boolean decompose = true;

    // caching params
    private String serverURL;

    public boolean isCache() {
        return serverURL != null;
    }
}
