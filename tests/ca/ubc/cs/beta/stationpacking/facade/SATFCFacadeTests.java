package ca.ubc.cs.beta.stationpacking.facade;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.execution.parameters.converters.PreviousAssignmentConverter;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;

import com.beust.jcommander.IStringConverter;

public class SATFCFacadeTests {
	
	//-DOMAINS "2149:14"
	
	private static final String LIBRARY = "/ubc/cs/project/arrow/afrechet/git/fcc-station-packing/SATsolvers/clasp/jna/libjnaclasp.so";
	private static final String DATA_FOLDERNAME = "/ubc/cs/home/a/afrechet/arrow-space/experiments/fcc-station-packing/webinar/Constraints02052014";
	
	private static final long SEED = 0;
	
	@Test
	public void testPreviousAssignment() {
		
		IStringConverter<HashMap<Integer,Integer>> converter = new PreviousAssignmentConverter();
		
		try(SATFCFacade satfc = new SATFCFacade(LIBRARY))
		{
			Collection<HashMap<Integer,Integer>> previousAssignments = Arrays.asList(
					new HashMap<Integer,Integer>(),
					converter.convert("2149:7;1184:15;82:20;232:9;1506:14;783:16;196:15;1276:12;1437:18;896:13")
					);
			
			
			for(HashMap<Integer,Integer> previousAssignment : previousAssignments)
			{
				SATFCResult result = satfc.solve(new HashSet<Integer>(Arrays.asList(2149,783,232,196,82,1437,1276,1184,1506,896)),
						new HashSet<Integer>(Arrays.asList(7,8,9,10,11,12,13,14,15,16,17,18,19,20)),
						new HashMap<Integer,Set<Integer>>(),
						previousAssignment,
						60.0,
						SEED,
						DATA_FOLDERNAME);
				
				assertTrue(result.getResult().equals(SATResult.SAT));
			}
			
		}
	}

}
