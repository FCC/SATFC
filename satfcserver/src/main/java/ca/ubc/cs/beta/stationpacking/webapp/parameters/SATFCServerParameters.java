package ca.ubc.cs.beta.stationpacking.webapp.parameters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import lombok.Getter;
import lombok.ToString;

/**
* Created by newmanne on 30/06/15.
*/
@ToString
@Parameters(separators = "=")
@UsageTextField(title="SATFCServer Parameters",description="Parameters needed to build SATFCServer")
public class SATFCServerParameters extends AbstractOptions {

    @Parameter(names = "--redis.host", description = "host for redis")
    @Getter
    private String redisURL =  "localhost";

    @Parameter(names = "--redis.port", description = "port for redis")
    @Getter
    private int redisPort = 6379;

    @Parameter(names = "--constraint.folder", description = "Folder containing all of the station configuration folders", required = true)
    @Getter
    private String constraintFolder;

    @Parameter(names = "--seed", description = "Random seed")
    @Getter
    private long seed = 1;

    @Parameter(names = "--cache.permutations", description = "The number of permutations for the containment cache to use. Higher numbers yield better performance with large caches, but are more memory expensive")
    @Getter
    private int numPermutations = 1;

    @Parameter(names = "--cache.size.limit", description = "Only use the first limit entries from the cache", hidden = true)
    @Getter
    private long cacheSizeLimit = Long.MAX_VALUE;

    @Parameter(names = "--skipSAT", description = "Do not load SAT entries from redis")
    @Getter
    private boolean skipSAT = false;

    @Parameter(names = "--skipUNSAT", description = "Do not load UNSAT entries from redis; Do not cache UNSAT entries")
    @Getter
    private boolean skipUNSAT = true;

    @Parameter(names = "--validateSAT", description = "Validate all SAT entries upon startup (slow)")
    @Getter
    private boolean validateSAT = false;


    @Parameter(names = "--excludeSameAuction", description = "Do not count a solution if it is derived from the same auction as the problem", hidden = true)
    @Getter
    private boolean excludeSameAuction = false;

    @Parameter(names = "--badSetFile", description = "JSON file listing, for some auctions, which auctions solutions are not allowed to come from", hidden = true)
    private String badSetFilePath;

    @Getter
    private Map<String, Set<String>> badsets;

    @Parameter(names = "--cache.screener", description = "Determine what goes into the cache", hidden = true)
    @Getter
    private CACHE_SCREENER_CHOICE cacheScreenerChoice = CACHE_SCREENER_CHOICE.NEW_INFO;

    @Parameter(names = "--cache.UHF.only", description = "Only cache UHF portions of problems")
    @Getter
    private boolean cacheUHFOnly = true;


    public enum CACHE_SCREENER_CHOICE {
        NEW_INFO, ADD_EVERYTHING, ADD_NOTHING
    }

    public void validate() {
        Preconditions.checkArgument(new File(constraintFolder).isDirectory(), "Provided constraint folder is not a directory", constraintFolder);
        if (badSetFilePath != null) {
            final File badSetFile = new File(badSetFilePath);
            Preconditions.checkArgument(badSetFile.exists(), "Could not locate bad set file", badSetFilePath);
            TypeReference<Map<String, Set<String>>> typeRef = new TypeReference<Map<String,Set<String>>>() {};
            try {
                badsets = JSONUtils.getMapper().readValue(Files.toString(badSetFile, Charset.defaultCharset()), typeRef);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
