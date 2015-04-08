package ca.ubc.cs.beta.stationpacking.cache;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
* Created by newmanne on 25/03/15.
*/
public class StationPackingInstanceHasher {

    // hashing function
    private static final HashFunction fHashFuction = Hashing.murmur3_32();

    public static HashCode hash(StationPackingInstance aInstance) {
        final HashCode hash = fHashFuction.newHasher()
                .putString(aInstance.toString(), Charsets.UTF_8)
                .hash();
        return hash;
    }

}
