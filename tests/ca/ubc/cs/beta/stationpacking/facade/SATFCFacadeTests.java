package ca.ubc.cs.beta.stationpacking.facade;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.execution.parameters.converters.PreviousAssignmentConverter;
import ca.ubc.cs.beta.stationpacking.execution.parameters.converters.StationDomainsConverter;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;

import com.beust.jcommander.IStringConverter;

public class SATFCFacadeTests {
	
	//-DOMAINS "2149:14"
	
	private static final String LIBRARY = "/ubc/cs/project/arrow/afrechet/git/fcc-station-packing/SATsolvers/clasp/jna/libjnaclasp.so";
	private static final String DATA_FOLDERNAME = "/ubc/cs/home/a/afrechet/arrow-space/experiments/fcc-station-packing/webinar/Constraints02052014";
	
	private static final long SEED = 0;
	
	@Test
	public void testPreviousAssignment() throws Exception {
		
		IStringConverter<HashMap<Integer,Integer>> prevAssignmentConverter = new PreviousAssignmentConverter();
		IStringConverter<HashMap<Integer,Set<Integer>>> domainsConverter = new StationDomainsConverter();
		
		SATFCFacadeBuilder satfcBuilder = new SATFCFacadeBuilder();
		satfcBuilder.setLibrary(LIBRARY);
		satfcBuilder.setInitializeLogging(true);
		
		try(SATFCFacade satfc = satfcBuilder.build())
		{
			Collection<HashMap<Integer,Integer>> previousAssignments = Arrays.asList(
					new HashMap<Integer,Integer>(),
					prevAssignmentConverter.convert("2149:7;1184:15;82:20;232:9;1506:14;783:16;196:15;1276:12;1437:18;896:13")
					);
			
			
			Map<Integer,Set<Integer>> domains = domainsConverter.convert("2149:7,8,9,10,11,12,13,14,15,16,17,18,19,20;783:7,8,9,10,11,12,13,14,15,16,17,18,19,20;232:7,8,9,10,11,12,13,14,15,16,17,18,19,20;196:7,8,9,10,11,12,13,14,15,16,17,18,19,20;82:7,8,9,10,11,12,13,14,15,16,17,18,19,20;1437:7,8,9,10,11,12,13,14,15,16,17,18,19,20;1276:7,8,9,10,11,12,13,14,15,16,17,18,19,20;1184:7,8,9,10,11,12,13,14,15,16,17,18,19,20;1506:7,8,9,10,11,12,13,14,15,16,17,18,19,20;896:7,8,9,10,11,12,13,14,15,16,17,18,19,20");
			
			Set<Integer> stations = domains.keySet();
			Set<Integer> channels = new HashSet<Integer>();
			for(Set<Integer> domain : domains.values())
			{
				channels.addAll(domain);
			}
			
			for(HashMap<Integer,Integer> previousAssignment : previousAssignments)
			{
				SATFCResult result = satfc.solve(
						stations,
						channels,
						domains,
						previousAssignment,
						60.0,
						SEED,
						DATA_FOLDERNAME);
				
				assertTrue(result.getResult().equals(SATResult.SAT));
			}
			
		}
	}

}
