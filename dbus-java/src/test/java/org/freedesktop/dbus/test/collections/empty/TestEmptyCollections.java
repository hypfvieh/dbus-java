/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution BV

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/
package org.freedesktop.dbus.test.collections.empty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.test.collections.empty.structs.ArrayStructIntStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ArrayStructPrimitive;
import org.freedesktop.dbus.test.collections.empty.structs.DeepArrayStruct;
import org.freedesktop.dbus.test.collections.empty.structs.DeepListStruct;
import org.freedesktop.dbus.test.collections.empty.structs.DeepMapStruct;
import org.freedesktop.dbus.test.collections.empty.structs.IEmptyCollectionStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ListMapStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ListStructPrimitive;
import org.freedesktop.dbus.test.collections.empty.structs.ListStructStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapArrayStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapStructIntStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapStructPrimitive;
import org.freedesktop.dbus.test.helper.structs.IntStruct;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

/**
 * 
 * The test structure is a bit of a complex constructions. However the goal is very simple
 * 
 * The class tests all structs implementing IEmptyCollectionStruct
 * 
 * There are two tests:
 * 
 * Empty test:
 * 
 * The test creates an empty object with emptyFactory function and the testString and sends the object to the {@link ISampleCollectionInterface}. 
 * This interface returns the {@link IEmptyCollectionStruct#getValidationValue()} that value can be 
 * used to determine whether (de)serialization of object with an empty collection is executed correctly.
 *
 * Non Empty test:
 * 
 * The test creates an non empty object with nonEmptyFactory function and the testString and sends the object to the {@link ISampleCollectionInterface}. 
 * This interface returns the {@link IEmptyCollectionStruct#getStringTestValue()} that value can be 
 * used to determine whether (de)serialization of non empty collection is executed correctly.
 *
 */
class TestEmptyCollections {

	private DBusConnection serverconn;
	private DBusConnection clientconn;
	private ISampleCollectionInterface clientObj;

	@BeforeEach
	public void setUp()  {
		try {
			serverconn = DBusConnection.getConnection(DBusBusType.SESSION);
			clientconn = DBusConnection.getConnection(DBusBusType.SESSION);
			serverconn.setWeakReferences(true);
			clientconn.setWeakReferences(true);

			/** This exports an instance of the test class as the object /Test. */
			ISampleCollectionInterface serverImpl = new SampleCollectionImpl();
			serverconn.exportObject(serverImpl.getObjectPath(), serverImpl);

			clientObj = clientconn.getRemoteObject(serverconn.getUniqueName(), serverImpl.getObjectPath(),
					ISampleCollectionInterface.class);
		} catch (DBusException _ex) {
			LoggerFactory.getLogger(TestEmptyCollections.class).error("Failed to initiate dbus.", _ex);
		}

	}

	@AfterEach
	public void tearDown() throws InterruptedException {
		DBusExecutionException dbee = serverconn.getError();
		if (null != dbee) {
			throw dbee;
		}
		dbee = clientconn.getError();
		if (null != dbee) {
			throw dbee;
		}
		clientconn.disconnect();
		serverconn.disconnect();
		
		// give the dbus daemon some time to unregister our calls before restarting test
		Thread.sleep(500L);
	}

	/**
	 * Parameterized test that collection will still know the next value. The Server
	 * will throw error or return wrong string if the test fails
	 * 
	 * @param arguments this contains the information required to build and call a
	 *                  function
	 * @param name      the name is used for validation and naming purpose should
	 *                  described the structure
	 */
	@DisplayName("testCollectionsEmpty")
	@ParameterizedTest(name = "{1}")
	@MethodSource("scenarios")
	<T extends IEmptyCollectionStruct<?>> void testEmpty(ArgumentObj<T> arguments, String name) {
		T object = arguments.factoryEmpty.apply(name);
		String result = arguments.function.apply(clientObj, object);
		assertEquals(object.getValidationValue(), result);
	}

	/**
	 * Parameterized test that collection will still know the next value. The Server
	 * will throw error or return wrong string if the test fails
	 * 
	 * @param arguments this contains the information required to build and call a
	 *                  function
	 * @param name      the name is used for validation and naming purpose should
	 *                  described the structure
	 */
	@DisplayName("testCollectionsNotEmpty")
	@ParameterizedTest(name = "{1}")
	@MethodSource("scenarios")
	<T extends IEmptyCollectionStruct<?>> void testNonEmpty(ArgumentObj<T> arguments, String name, String validationValue) {
		T object = arguments.factoryNonEmpty.apply(name);
		String result = arguments.function.apply(clientObj, object);
		assertEquals(validationValue, result);
	}
	
	/**
	 * List of arguments for each scenario:
	 * 1: Interface function to use for to test this struct
	 * 2: Factory to create an empty structure
	 * 3: Factory to create an non empty structure
	 * 4: Name, which is also used for validating empty collections
	 * 5: Expected to string value for non empty collection test
	 */
	static Stream<Arguments> scenarios() {
		return Stream.of(
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testListPrimitive,
						s -> new ListStructPrimitive(Collections.emptyList(), s),
						s -> new ListStructPrimitive(Arrays.asList(1, 2), s)), "ListPrimative", "1,2"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testListIntStruct,
						s -> new ListStructStruct(Collections.emptyList(), s),
						s -> new ListStructStruct(Arrays.asList(new IntStruct(5, 6)), s)), "ListStruct", "(5,6)"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testArrayPrimitive,
						s -> new ArrayStructPrimitive(new int[0], s),
						s -> new ArrayStructPrimitive(new int[] {4,5}, s)), "ArrayPrimative", "4,5"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testArrayIntStruct,
						s -> new ArrayStructIntStruct(new IntStruct[0], s),
						s -> new ArrayStructIntStruct(new IntStruct[] { new IntStruct(9, 12)}, s)),
						"ArrayIntStruct", "(9,12)"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testMapPrimitive,
						s -> new MapStructPrimitive(Collections.emptyMap(), s),
						s -> new MapStructPrimitive(getIntHashMap(), s)), "MapPrimative", "{test:8}"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testMapIntStruct,
						s -> new MapStructIntStruct(Collections.emptyMap(), s),
						s -> new MapStructIntStruct(getIntStructHashMap(), s)), "MapIntStruct", "{other:(12,17)}"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testDeepList,
						s -> new DeepListStruct(Collections.emptyList(), s),
						s -> new DeepListStruct(getDeepList(), s)), "DeepListStruct", "[[[(111,44)]]]"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testDeepArray,
						s -> new DeepArrayStruct(new IntStruct[0][][], s),
						s -> new DeepArrayStruct(getDeepArrayStruct(), s)), "DeepArrayStruct", "[[[(131,145)]]]"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testDeepMap,
						s -> new DeepMapStruct(Collections.emptyMap(), s),
						s -> new DeepMapStruct(getDeepMapStruct(), s)), "DeepMapStruct",
						"{complete:{inbetween:{test:(42,19)}}}"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testMixedListMap,
						s -> new ListMapStruct(Collections.emptyList(), s),
						s -> new ListMapStruct(Arrays.asList(getIntStructHashMap()), s)), "mixedListMapStruct",
						"[{other=(12,17)}]"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testMixedMapArray,
						s -> new MapArrayStruct(Collections.emptyMap(), s),
						s -> new MapArrayStruct(getMapArray(), s)), "mixedMapArrayStruct", "{other=[(99,33)]}")
				);
	}

	private static Map<String, IntStruct[]> getMapArray() {
		Map<String, IntStruct[]> map = new HashMap<>();
		map.put("other", new IntStruct[] {new IntStruct(99, 33)});
		return map;
	}

	private static Map<String, Map<String, Map<String, IntStruct>>> getDeepMapStruct() {
		Map<String, Map<String, Map<String, IntStruct>>> outerMap = new HashMap<>();
		Map<String, Map<String, IntStruct>> midlleMap = new HashMap<>();
		Map<String, IntStruct> innerMap = new HashMap<>();
		innerMap.put("test", new IntStruct(42, 19));
		midlleMap.put("inbetween", innerMap);
		outerMap.put("complete", midlleMap);
		return outerMap;
	}

	private static IntStruct[][][] getDeepArrayStruct() {
		IntStruct[][][] array = new IntStruct[1][1][1];
		array[0][0][0] = new IntStruct(131, 145);
		return array;
	}

	private static List<List<List<IntStruct>>> getDeepList() {
		return Arrays.asList(Arrays.asList(Arrays.asList(new IntStruct(111, 44))));
	}
	
	private static Map<String, IntStruct> getIntStructHashMap() {
		Map<String, IntStruct> map = new HashMap<>();
		map.put("other", new IntStruct(12, 17));
		return map;
	}

	private static Map<String, Integer> getIntHashMap() {
		Map<String, Integer> map = new HashMap<>();
		map.put("test", 8);
		return map;
	}

	/**
	 * Wrapper object for first three arguments
	 */
	private final static class ArgumentObj<T> {
		private final BiFunction<ISampleCollectionInterface, T, String> function;
		private final Function<String, T> factoryEmpty;
		private Function<String, T> factoryNonEmpty;

		public ArgumentObj(BiFunction<ISampleCollectionInterface, T, String> function, Function<String, T> factoryEmpty,
				Function<String, T> factoryNonEmpty) {
			this.function = function;
			this.factoryEmpty = factoryEmpty;
			this.factoryNonEmpty = factoryNonEmpty;
		}
	}
}
