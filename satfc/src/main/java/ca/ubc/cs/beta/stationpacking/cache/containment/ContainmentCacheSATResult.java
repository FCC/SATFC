package ca.ubc.cs.beta.stationpacking.cache.containment;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ca.ubc.cs.beta.stationpacking.base.Station;

/**
* Created by newmanne on 25/03/15.
*/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContainmentCacheSATResult {

    private Map<Integer, Set<Station>> result;
    // the redis key of the problem whose solution "solves" this problem
    private String key;

    /** true if a solution was found */
    @JsonIgnore
    public boolean isValid() {
        return result != null && key != null;
    }

    // return an empty or failed result, that represents an error or that the problem was not solvable via the cache
    public static ContainmentCacheSATResult failure() {
        return new ContainmentCacheSATResult();
    }
}
