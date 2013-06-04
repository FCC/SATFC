package data;

import java.util.HashSet;

import junit.framework.TestCase;

public class THashSet extends TestCase {
	
	public void testPermutation()
	{
		Integer x = 1;
		Integer y = 2;
		
		HashSet<Integer> S = new HashSet<Integer>();
		S.add(x);
		S.add(y);
		
		HashSet<Integer> T = new HashSet<Integer>();
		T.add(y);
		T.add(x);
		
		assertTrue(S.equals(T));
		assertTrue(S.hashCode() == T.hashCode());
	}
	
}
