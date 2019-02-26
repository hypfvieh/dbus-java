package org.freedesktop.dbus.test.collections.empty.structs;

/**
 * Interface is used to make the TestEmptyCollection usable for parameterized testing  
 */
public interface IEmptyCollectionStruct<T> {

	/**
	 * Return the collection structure
	 */
	T getValue();
	
	/**
	 * The validation String value, which is used to determine if next value is also still intact with empty collection
	 */
	String getValidationValue();
	
	/**
	 * A to string value of the collection to determine a correct value
	 */
	String getStringTestValue();

	/**
	 * Return true when collection part of the structure is empty
	 */
	boolean isEmpty();
}