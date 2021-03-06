package org.bluedb.api;

import java.io.Serializable;

/**
 * Function to mutate value objects in a collection
 * @param <V> the class of objects stored in collection as values
 */
@FunctionalInterface
public interface Updater<V extends Serializable> {

	/**
	 * Function to mutate a value in a collection.
	 * @param value collection value to be mutated
	 */
	public void update(V value);
}
