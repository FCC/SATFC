package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
* Created by newmanne on 08/07/15.
*/
public class TestConstraintManager extends AMapBasedConstraintManager {

	public TestConstraintManager(List<TestConstraint> constraints) throws FileNotFoundException {
        super(null, "");
        constraints.stream().forEach(c -> {
            final Map<Station, Map<Integer, Set<Station>>> map = c.getKey().equals(ConstraintKey.CO) ? fCOConstraints : c.getKey().equals(ConstraintKey.ADJp1) ? fADJp1Constraints : fADJp2Constraints;
            map.putIfAbsent(c.getReference(), new HashMap<>());
            map.get(c.getReference()).putIfAbsent(c.getChannel(), new HashSet<>());
            map.get(c.getReference()).get(c.getChannel()).addAll(c.getInterfering());
        });
    }

	@Override
	protected void loadConstraints(IStationManager stationManager, String fileName) throws FileNotFoundException {
	}

}
