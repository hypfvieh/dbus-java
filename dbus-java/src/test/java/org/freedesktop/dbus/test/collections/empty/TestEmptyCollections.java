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
import org.freedesktop.dbus.test.collections.empty.structs.ArrayStructPrimative;
import org.freedesktop.dbus.test.collections.empty.structs.DeepArrayStruct;
import org.freedesktop.dbus.test.collections.empty.structs.DeepListStruct;
import org.freedesktop.dbus.test.collections.empty.structs.DeepMapStruct;
import org.freedesktop.dbus.test.collections.empty.structs.IEmptyCollectionStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ListMapStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ListStructPrimative;
import org.freedesktop.dbus.test.collections.empty.structs.ListStructStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapArrayStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapStructIntStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapStructPrimative;
import org.freedesktop.dbus.test.helper.structs.IntStruct;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TestEmptyCollections {

	private DBusConnection serverconn;
	private DBusConnection clientconn;
	private ISampleCollectionInterface clientObj;

	@BeforeEach
	public void setUp() throws DBusException {
		serverconn = DBusConnection.getConnection(DBusBusType.SESSION);
		clientconn = DBusConnection.getConnection(DBusBusType.SESSION);
		serverconn.setWeakReferences(true);
		clientconn.setWeakReferences(true);

		/** This exports an instance of the test class as the object /Test. */
		ISampleCollectionInterface serverImpl = new SampleCollectionImpl();
		serverconn.exportObject(serverImpl.getObjectPath(), serverImpl);

		clientObj = clientconn.getRemoteObject(serverconn.getUniqueName(), serverImpl.getObjectPath(),
				ISampleCollectionInterface.class);

	}

	@AfterEach
	public void tearDown() {
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
	
	static Stream<Arguments> scenarios() {
		return Stream.of(
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testListPrimative,
						s -> new ListStructPrimative(Collections.emptyList(), s),
						s -> new ListStructPrimative(Arrays.asList(1, 2), s)), "ListPrimative", "1,2"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testListIntStruct,
						s -> new ListStructStruct(Collections.emptyList(), s),
						s -> new ListStructStruct(Arrays.asList(new IntStruct(5, 6)), s)), "ListStruct", "(5,6)"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testArrayPrimative,
						s -> new ArrayStructPrimative(new int[0], s),
						s -> new ArrayStructPrimative(new int[] {4,5}, s)), "ArrayPrimative", "4,5"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testArrayIntStruct,
						s -> new ArrayStructIntStruct(new IntStruct[0], s),
						s -> new ArrayStructIntStruct(new IntStruct[] { new IntStruct(9, 12)}, s)),
						"ArrayIntStruct", "(9,12)"),
				Arguments.of(new ArgumentObj<>(ISampleCollectionInterface::testMapPrimative,
						s -> new MapStructPrimative(Collections.emptyMap(), s),
						s -> new MapStructPrimative(getIntHashMap(), s)), "MapPrimative", "{test:8}"),
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
