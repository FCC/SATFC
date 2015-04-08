package ca.ubc.cs.beta.stationpacking.cache.containment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
* Created by newmanne on 25/03/15.
*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainmentCacheUNSATResult {
    // the redis key of the problem whose solution "solves" this problem
    private String key;

    /** true if a solution was found */
    @JsonIgnore
    public boolean isValid() {
        return key != null;
    }

    // return an empty or failed result, that represents an error or that the problem was not solvable via the cache
    public static ContainmentCacheUNSATResult failure() {
        return new ContainmentCacheUNSATResult();
    }
}
