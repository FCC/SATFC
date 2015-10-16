package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import ca.ubc.cs.beta.stationpacking.cache.ICacher;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;

/**
 * Created by emily404 on 6/5/15.
 */
public class StationSizeSquaredProblemSampler implements IProblemSampler {

    private WeightedCollection<String> stationSizeMap = new WeightedCollection<>();

    public StationSizeSquaredProblemSampler (Jedis jedis) {
        String startAndEndCursor = "0";
        ScanParams params = new ScanParams();
        params.match("*:SAT*");

        // iterate through all keys
        ScanResult<String> scanResult = jedis.scan(startAndEndCursor, params);
        while(!scanResult.getStringCursor().equals(startAndEndCursor)){
            scanResult.getResult().forEach(key -> {
                // convert to containment cache object
                ICacher.SATCacheEntry satEntry = getSATCacheEntry(jedis, key);

                // TODO: newly solved problem is not captured here
                // get size of station list and use size SQUARED as weight
                stationSizeMap.add(Math.pow(satEntry.getStations().size(), 2), key);
            });
            scanResult = jedis.scan(scanResult.getStringCursor(), params);
        }

    }
    /**
     * Sample problems based on station size of each problem
     * station size is squared to skew probability towards larger problems
     * problems with larger station size are more likely to be selected
     * @param count size of return list
     * @return list of cache entry keys to be added to keyQueue, may contain duplicates
     */
    @Override
    public List<String> sample(int count) {
        int localCounter = count; // make a copy of count to ensure that the original is not modified
        List<String> keys = new ArrayList<>();

        for(int i = 0; i < localCounter; i++){
            keys.add(stationSizeMap.next());
        }

        return keys;
    }

    /**
     * Sample one problem based on station size of each problem
     * station size is squared to skew probability towards larger problems
     * problems with larger station size are more likely to be selected
     * @return one cache entry key
     */
    @Override
    public String sample() {
       return stationSizeMap.next();
    }

    /**
     * convert entry string to containment cache entry object
     * @return a ContainmentCacheSATEntry with key
     */
    private static ICacher.SATCacheEntry getSATCacheEntry(Jedis jedis, String key) {
        String entry = jedis.get(key);
        return JSONUtils.toObject(entry, ICacher.SATCacheEntry.class);
    }
}
