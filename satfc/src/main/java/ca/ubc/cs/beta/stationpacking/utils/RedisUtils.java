package ca.ubc.cs.beta.stationpacking.utils;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Created by newmanne on 11/05/15.
 */
public class RedisUtils {

    public static final String TIMEOUTS_QUEUE = "_TIMEOUTS";
    public static final String PROCESSING_QUEUE = "_PROCESSING";
    public static final String CNF_QUEUE = "CNF";
    public static final String CNF_INDEX_QUEUE = "CNFIndex";
    public static final String CNF_ASSIGNMENT_QUEUE = "CNFAssignment";

    public static String makeKey(String... args) {
        return Joiner.on(':').join(args);
    }

}
