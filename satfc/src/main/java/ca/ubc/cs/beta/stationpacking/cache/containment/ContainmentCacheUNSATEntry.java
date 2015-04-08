package ca.ubc.cs.beta.stationpacking.cache.containment;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

/**
* Created by newmanne on 25/03/15.
*/
@Data
public class ContainmentCacheUNSATEntry {
    final BitSet bitSet;
    Map<Station, Set<Integer>> domains;
    String key;

    // "fake" constructor used for comparator purposes only
    public ContainmentCacheUNSATEntry(BitSet bitSet) {
        this.bitSet = bitSet;
    }

    public ContainmentCacheUNSATEntry(final Map<Station, Set<Integer>> domains, final String key) {
        this.key = key;
        this.domains = domains;
        this.bitSet = new BitSet(StationPackingUtils.N_STATIONS);
        domains.keySet().forEach(station -> bitSet.set(station.getID()));
    }

}
