package experiment.instanceencoder.cnfencoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;




import data.Constraint;
import data.Station;

/**
 * A static CNF encoder which builds encoding on the fly for each encode query.
 * @author afrechet
 *
 */
public class StaticCNFEncoder implements ICNFEncoder{

	
	private HashMap<Station,Collection<String>> fStationtoDACClause;
	private HashMap<Pair<Station,Station>,Collection<String>> fStationPairtoDACClause;
	private HashMap<Station,Integer> fMaxVariable;
	
	/**
	 * @param aStationChannelDomain - a map taking a station to its the list of valid channels it can be on.
	 * @param aConstraints - a set of pairwise constraints.
	 */
	public StaticCNFEncoder(Map<Station,Set<Integer>> aStationChannelDomain, Set<Constraint> aConstraints)
	{
		fMaxVariable = new HashMap<Station,Integer>();
		fStationtoDACClause = new HashMap<Station,Collection<String>>();
		HashMap<Pair<Station,Integer>,Integer> aVariables = new HashMap<Pair<Station,Integer>,Integer>();
		Integer aVariable = 1;
		
		//Encode variables and base clauses.
	
		for(Station aStation : aStationChannelDomain.keySet())
		{	
			//Get the variables associated with the station.
			ArrayList<Integer> aStationVariables = new ArrayList<Integer>(); 
			for(Integer aChannel : aStationChannelDomain.get(aStation))
			{
				aVariables.put(new Pair<Station,Integer>(aStation, aChannel), aVariable);
				aStationVariables.add(aVariable);
				fMaxVariable.put(aStation, aVariable);
				
				aVariable++;
			}
			//Define all the base clauses for that station.
			LinkedList<String> aStationBaseClauses = new LinkedList<String>();
			
			//Station must be at least on one channel.
			aStationBaseClauses.add(StringUtils.join(aStationVariables," "));
			
			//Station cannot be on two channels at the same time.
			for(int i=0;i<aStationVariables.size();i++)
			{
				for(int j=i+1;j<aStationVariables.size();j++)
				{
					aStationBaseClauses.add("-"+aStationVariables.get(i)+" "+"-"+aStationVariables.get(j));
				}
			}
			
			fStationtoDACClause.put(aStation, aStationBaseClauses);
			
		}
		//Encode other pairwise clauses.
		fStationPairtoDACClause = new HashMap<Pair<Station,Station>,Collection<String>>();
		for(Constraint aConstraint : aConstraints)
		{
			Station aStation1 = aConstraint.getProtectedPair().getKey();
			Station aStation2 = aConstraint.getInterferingPair().getKey();
			
			Station aStationSmall = aStation1;
			Station aStationBig = aStation2;
			if(aStation1.getID()>aStation2.getID())
			{
				aStationSmall = aStation2;
				aStationBig = aStation1;
			}
			
			Pair<Station,Station> aStationPairKey = new Pair<Station,Station>(aStationSmall,aStationBig);
			
			
			Integer aVariable1 = aVariables.get(aConstraint.getProtectedPair());
			Integer aVariable2 = aVariables.get(aConstraint.getInterferingPair());
			
			String aDACClause = "-"+aVariable1+" "+"-"+aVariable2;
			
			if(!fStationPairtoDACClause.containsKey(aStationPairKey))
			{
				LinkedList<String> aDACClauses = new LinkedList<String>();
				aDACClauses.add(aDACClause);
				fStationPairtoDACClause.put(aStationPairKey, aDACClauses);
			}
			else
			{
				fStationPairtoDACClause.get(aStationPairKey).add(aDACClause);
			}	
			
		}
		
	}
	
	@Override
	public String encode(Set<Station> aStations) {
		
		ArrayList<Station> aStationsList = new ArrayList<Station>(aStations);
		
		String aResult = "";
		
		int aMaxVariable = 0;
		int aNumberClauses = 0;
		
		for(int i=0;i<aStationsList.size();i++)
		{
			Station aStation1 = aStationsList.get(i);
			
			Integer aStationMaxVariable = fMaxVariable.get(aStation1);
			if(aStationMaxVariable>aMaxVariable)
			{
				aMaxVariable = aStationMaxVariable;
			}
			
			for(String aDACClause : fStationtoDACClause.get(aStation1)){
				
				aResult += aDACClause+" 0\n";
				aNumberClauses++;
			}
			
			for(int j=i+1;j<aStationsList.size();j++)
			{
				Station aStation2 = aStationsList.get(j);
				
				Station aStationSmall = aStation1;
				Station aStationBig = aStation2;
				if(aStation1.getID()>aStation2.getID())
				{
					aStationSmall = aStation2;
					aStationBig = aStation1;
				}
				
				Pair<Station,Station> aStationPairKey = new Pair<Station,Station>(aStationSmall,aStationBig);
				
				if(fStationPairtoDACClause.containsKey(aStationPairKey))
				{
					for(String aDACClause : fStationPairtoDACClause.get(aStationPairKey)){
						aResult += aDACClause+" 0\n";
						aNumberClauses++;
					}
				}
			}
			
		}
		
		aResult = "c CNF file corresponding to a (partial) station packing problem.\n"+"p cnf "+aMaxVariable+" "+aNumberClauses+"\n"+aResult;
		
		return aResult;
		

		
		
		
		
	}

}
