package ca.ubc.cs.beta.stationpacking.utils;

import java.util.Arrays;
import java.util.HashSet;

public class StationPackingUtils {

	private StationPackingUtils()
	{
		//Cannot construct a utils class.
	}
	
	public static final Integer LVHFmin = 2, LVHFmax = 6, UVHFmin=7, UVHFmax = 13, UHFmin = 14, UHFmax = 51;
	public static final HashSet<Integer> LVHF_CHANNELS = new HashSet<Integer>(Arrays.asList(2,3,4,5,6));
	public static final HashSet<Integer> HVHF_CHANNELS = new HashSet<Integer>(Arrays.asList(7,8,9,10,11,12,13));
	public static final HashSet<Integer> UHF_CHANNELS = new HashSet<Integer>(Arrays.asList(14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,38,39,40,41,42,43,44,45,46,47,48,49,50,51));

}
