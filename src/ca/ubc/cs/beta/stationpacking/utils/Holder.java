package ca.ubc.cs.beta.stationpacking.utils;

/**
 * Classes that holds an object that can be set & get.
 * @author gsauln
 *
 * @param <T> Type of object contained
 */
public class Holder<T>
{

	private T fObject;
	
	/**
	 * Creates a new empty holder.
	 */
	public Holder()
	{
		fObject = null;
	}
	
	/**
	 * Creates an Holder containing this initial object.
	 * @param obj an Holder containing this initial object.
	 */
	public Holder(T obj)
	{
		fObject = obj;
	}
	
	/**
	 * Returns the object contained in the holder, if the holder is empty null is returned.
	 * @return the object contained in the holder, if the holder is empty null is returned.
	 */
	public synchronized T get()
	{
		return fObject;
	}
	
	/**
	 * Returns the object contained in the holder and empties it.
	 * @return the object contained in the holder and empties it.
	 */
	public synchronized T pop()
	{
		T temp = fObject;
		fObject = null;
		return temp;
	}
	
	/**
	 * Set the  object contained in the holder to the given object.
	 * @param obj object to be contained in the holder.
	 */
	public synchronized void set(T obj)
	{
		fObject = obj;
	}
	
	/**
	 * Return true if the holder is empty, false otherwise.
	 * @return true if the holder is empty, false otherwise.
	 */
	public synchronized boolean isEmpty()
	{
		return (fObject == null);
	}
	
	/**
	 * Empties the holder.
	 */
	public synchronized void clear()
	{
		fObject = null;
	}
}
