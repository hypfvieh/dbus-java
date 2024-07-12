package org.freedesktop.dbus.test.helper.cross;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.*;
import org.freedesktop.dbus.test.helper.interfaces.Binding;
import org.freedesktop.dbus.test.helper.interfaces.Binding.CrossSampleStruct;
import org.freedesktop.dbus.test.helper.interfaces.SamplesInterface;
import org.freedesktop.dbus.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

public class CrossTestClient implements Binding.SampleClient, DBusSigHandler<Binding.SampleSignals.Triggered> {

    private static final Logger            LOGGER = LoggerFactory.getLogger(CrossTestClient.class);

    private final DBusConnection           conn;
    //CHECKSTYLE:OFF
    public final Set<String>               passed = new TreeSet<>();
    public final Map<String, List<String>> failed = new HashMap<>();
    //CHECKSTYLE:ON

    public CrossTestClient(DBusConnection _conn) {
        this.conn = _conn;
        List<String> l = new ArrayList<>();
        l.add("Signal never arrived");
        failed.put("org.freedesktop.DBus.Binding.TestSignals.Triggered", l);
        l = new ArrayList<>();
        l.add("Method never called");
        failed.put("org.freedesktop.DBus.Binding.TestClient.Response", l);

    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return null;
    }

    @Override
    public void handle(Binding.SampleSignals.Triggered _t) {
        failed.remove("org.freedesktop.DBus.Binding.TestSignals.Triggered");
        if (new UInt64(21389479283L).equals(_t.getSampleUint64()) && "/Test".equals(_t.getPath())) {
            pass("org.freedesktop.DBus.Binding.TestSignals.Triggered");
        } else if (!new UInt64(21389479283L).equals(_t.getSampleUint64())) {
            fail("org.freedesktop.DBus.Binding.TestSignals.Triggered", "Incorrect signal content; expected 21389479283 got " + _t.getSampleUint64());
        } else if (!"/Test".equals(_t.getPath())) {
            fail("org.freedesktop.DBus.Binding.TestSignals.Triggered", "Incorrect signal source object; expected /Test got " + _t.getPath());
        }
    }

    @Override
    public void Response(UInt16 _a, double _b) {
        failed.remove("org.freedesktop.DBus.Binding.TestClient.Response");
        if (_a.equals(new UInt16(15)) && _b == 12.5) {
            pass("org.freedesktop.DBus.Binding.TestClient.Response");
        } else {
            fail("org.freedesktop.DBus.Binding.TestClient.Response", "Incorrect parameters; expected 15, 12.5 got " + _a + ", " + _b);
        }
    }

    public void pass(String _test) {
        passed.add(_test.replaceAll("[$]", "."));
    }

    public void fail(String _test, String _reason) {
        String test = _test.replaceAll("[$]", ".");
        List<String> reasons = failed.computeIfAbsent(test, k -> new ArrayList<>());
        reasons.add(_reason);
    }

    @SuppressWarnings("unchecked")
    public void test(Class<? extends DBusInterface> _iface, Object _proxy, String _method, Object _rv, Object... _parameters) {
        try {
            Method[] ms = _iface.getMethods();
            Method m = null;
            for (Method t : ms) {
                if (t.getName().equals(_method)) {
                    m = t;
                    break;
                }
            }
            Object o = m.invoke(_proxy, _parameters);

            String msg = "Incorrect return value; sent ( ";
            if (null != _parameters) {
                for (Object po : _parameters) {
                    if (null != po) {
                        msg += collapseArray(po) + ",";
                    }
                }
            }
            msg = msg.replaceAll(".$", ");");
            msg += " expected " + collapseArray(_rv) + " got " + collapseArray(o);

            if (null != _rv && _rv.getClass().isArray()) {
                compareArray(_iface.getName() + "." + _method, _rv, o);
            } else if (_rv instanceof Map) {
                if (o instanceof Map) {
                    Map<Object, Object> a = (Map<Object, Object>) o;
                    Map<Object, Object> b = (Map<Object, Object>) _rv;
                    if (a.keySet().size() != b.keySet().size()) {
                        fail(_iface.getName() + "." + _method, msg);
                    } else {
                        for (Object k : a.keySet()) {
                            if (a.get(k) instanceof List) {
                                if (b.get(k) instanceof List) {
                                    if (!setCompareLists((List<Object>) a.get(k), (List<Object>) b.get(k))) {
                                        fail(_iface.getName() + "." + _method, msg);
                                    }
                                } else {
                                    fail(_iface.getName() + "." + _method, msg);
                                }
                            } else if (!a.get(k).equals(b.get(k))) {
                                fail(_iface.getName() + "." + _method, msg);
                                return;
                            }
                        }
                    }
                    pass(_iface.getName() + "." + _method);
                } else {
                    fail(_iface.getName() + "." + _method, msg);
                }
            } else {
                if (Objects.equals(o, _rv)) {
                    pass(_iface.getName() + "." + _method);
                } else {
                    fail(_iface.getName() + "." + _method, msg);
                }
            }
        } catch (InvocationTargetException _exIt) {
            _exIt.printStackTrace();
            fail(_iface.getName() + "." + _method, "Error occurred during execution: " + _exIt.getCause().getClass().getName() + " " + _exIt.getCause().getMessage());
        } catch (Exception _ex) {
            _ex.printStackTrace();
            fail(_iface.getName() + "." + _method, "Error occurred during execution: " + _ex.getClass().getName() + " " + _ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static String collapseArray(Object _array) {
        if (null == _array) {
            return "null";
        }
        if (_array.getClass().isArray()) {
            String s = "{ ";
            for (int i = 0; i < Array.getLength(_array); i++) {
                s += collapseArray(Array.get(_array, i)) + ",";
            }
            s = s.replaceAll(".$", " }");
            return s;
        } else if (_array instanceof List) {
            String s = "{ ";
            for (Object o : (List<Object>) _array) {
                s += collapseArray(o) + ",";
            }
            s = s.replaceAll(".$", " }");
            return s;
        } else if (_array instanceof Map) {
            String s = "{ ";
            for (Object o : ((Map<Object, Object>) _array).keySet()) {
                s += collapseArray(o) + " => " + collapseArray(((Map<Object, Object>) _array).get(o)) + ",";
            }
            s = s.replaceAll(".$", " }");
            return s;
        } else {
            return _array.toString();
        }
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    public static <T> boolean setCompareLists(List<T> _a, List<T> _b) {
        if (_a.size() != _b.size()) {
            return false;
        }
        for (Object v : _a) {
            if (!_b.contains(v)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static List<Variant<Object>> primitizeRecurse(Object _a, Type _t) {
        List<Variant<Object>> vs = new ArrayList<>();
        if (_t instanceof ParameterizedType) {
            Class<Object> c = (Class<Object>) ((ParameterizedType) _t).getRawType();
            if (List.class.isAssignableFrom(c)) {
                Object os;
                if (_a instanceof List) {
                    os = ((List<Object>) _a).toArray();
                } else {
                    os = _a;
                }
                Type[] ts = ((ParameterizedType) _t).getActualTypeArguments();
                for (int i = 0; i < Array.getLength(os); i++) {
                    vs.addAll(primitizeRecurse(Array.get(os, i), ts[0]));
                }
            } else if (Map.class.isAssignableFrom(c)) {
                Object[] os = ((Map<?, ?>) _a).keySet().toArray();
                Object[] ks = ((Map<?, ?>) _a).values().toArray();
                Type[] ts = ((ParameterizedType) _t).getActualTypeArguments();
                for (Object element : ks) {
                    vs.addAll(primitizeRecurse(element, ts[0]));
                }
                for (Object element : os) {
                    vs.addAll(primitizeRecurse(element, ts[1]));
                }
            } else if (Struct.class.isAssignableFrom(c)) {
                Object[] os = ((Struct) _a).getParameters();
                Type[] ts = ((ParameterizedType) _t).getActualTypeArguments();
                for (int i = 0; i < os.length; i++) {
                    vs.addAll(primitizeRecurse(os[i], ts[i]));
                }

            } else if (Variant.class.isAssignableFrom(c)) {
                vs.addAll(primitizeRecurse(((Variant<?>) _a).getValue(), ((Variant<?>) _a).getType()));
            }
        } else if (Variant.class.isAssignableFrom((Class<?>) _t)) {
            vs.addAll(primitizeRecurse(((Variant<?>) _a).getValue(), ((Variant<?>) _a).getType()));
        } else if (_t instanceof Class && ((Class<?>) _t).isArray()) {
            Type t2 = ((Class<?>) _t).getComponentType();
            for (int i = 0; i < Array.getLength(_a); i++) {
                vs.addAll(primitizeRecurse(Array.get(_a, i), t2));
            }
        } else {
            vs.add(new Variant<>(_a));
        }

        return vs;
    }

    public List<Variant<Object>> primitize(Variant<Object> _a) {
        return primitizeRecurse(_a.getValue(), _a.getType());
    }

    public void primitizeTest(SamplesInterface _tests, Object _input) {
        Variant<Object> in = new Variant<>(_input);
        List<Variant<Object>> vs = primitize(in);
        List<Variant<Object>> res;

        try {

            res = _tests.Primitize(in);
            if (setCompareLists(res, vs)) {
                pass("org.freedesktop.DBus.Binding.Tests.Primitize");
            } else {
                fail("org.freedesktop.DBus.Binding.Tests.Primitize", "Wrong Return Value; expected " + collapseArray(vs) + " got " + collapseArray(res));
            }

        } catch (Exception _ex) {
            LOGGER.debug("", _ex);
            fail("org.freedesktop.DBus.Binding.Tests.Primitize", "Exception occurred during test: (" + _ex.getClass().getName() + ") " + _ex.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void doTests(Peer _peer, Introspectable _intro, Introspectable _rootintro, SamplesInterface _tests, Binding.SingleSample _singletests) {
        final Random r = new Random();

        test(Peer.class, _peer, "Ping", null);

        try {
            if (_intro.Introspect().startsWith("<!DOCTYPE")) {
                pass("org.freedesktop.DBus.Introspectable.Introspect");
            } else {
                fail("org.freedesktop.DBus.Introspectable.Introspect", "Didn't get valid xml data back when introspecting /Test");
            }
        } catch (DBusExecutionException _ex) {

            LOGGER.debug("", _ex);

            fail("org.freedesktop.DBus.Introspectable.Introspect", "Got exception during introspection on /Test (" + _ex.getClass().getName() + "): " + _ex.getMessage());
        }

        try {
            if (_rootintro.Introspect().startsWith("<!DOCTYPE")) {
                pass("org.freedesktop.DBus.Introspectable.Introspect");
            } else {
                fail("org.freedesktop.DBus.Introspectable.Introspect", "Didn't get valid xml data back when introspecting /");
            }
        } catch (DBusExecutionException _ex) {

            LOGGER.debug("", _ex);

            fail("org.freedesktop.DBus.Introspectable.Introspect", "Got exception during introspection on / (" + _ex.getClass().getName() + "): " + _ex.getMessage());
        }

        test(SamplesInterface.class, _tests, "Identity", new Variant<>(1), new Variant<>(1));
        test(SamplesInterface.class, _tests, "Identity", new Variant<>("Hello"), new Variant<>("Hello"));

        test(SamplesInterface.class, _tests, "IdentityBool", false, false);
        test(SamplesInterface.class, _tests, "IdentityBool", true, true);

        test(SamplesInterface.class, _tests, "Invert", false, true);
        test(SamplesInterface.class, _tests, "Invert", true, false);

        test(SamplesInterface.class, _tests, "IdentityByte", (byte) 0, (byte) 0);
        test(SamplesInterface.class, _tests, "IdentityByte", (byte) 1, (byte) 1);
        test(SamplesInterface.class, _tests, "IdentityByte", (byte) -1, (byte) -1);
        test(SamplesInterface.class, _tests, "IdentityByte", Byte.MAX_VALUE, Byte.MAX_VALUE);
        test(SamplesInterface.class, _tests, "IdentityByte", Byte.MIN_VALUE, Byte.MIN_VALUE);
        int i = r.nextInt();
        test(SamplesInterface.class, _tests, "IdentityByte", (byte) i, (byte) i);

        test(SamplesInterface.class, _tests, "IdentityInt16", (short) 0, (short) 0);
        test(SamplesInterface.class, _tests, "IdentityInt16", (short) 1, (short) 1);
        test(SamplesInterface.class, _tests, "IdentityInt16", (short) -1, (short) -1);
        test(SamplesInterface.class, _tests, "IdentityInt16", Short.MAX_VALUE, Short.MAX_VALUE);
        test(SamplesInterface.class, _tests, "IdentityInt16", Short.MIN_VALUE, Short.MIN_VALUE);
        i = r.nextInt();
        test(SamplesInterface.class, _tests, "IdentityInt16", (short) i, (short) i);

        test(SamplesInterface.class, _tests, "IdentityInt32", 0, 0);
        test(SamplesInterface.class, _tests, "IdentityInt32", 1, 1);
        test(SamplesInterface.class, _tests, "IdentityInt32", -1, -1);
        test(SamplesInterface.class, _tests, "IdentityInt32", Integer.MAX_VALUE, Integer.MAX_VALUE);
        test(SamplesInterface.class, _tests, "IdentityInt32", Integer.MIN_VALUE, Integer.MIN_VALUE);
        i = r.nextInt();
        test(SamplesInterface.class, _tests, "IdentityInt32", i, i);

        test(SamplesInterface.class, _tests, "IdentityInt64", (long) 0, (long) 0);
        test(SamplesInterface.class, _tests, "IdentityInt64", (long) 1, (long) 1);
        test(SamplesInterface.class, _tests, "IdentityInt64", (long) -1, (long) -1);
        test(SamplesInterface.class, _tests, "IdentityInt64", Long.MAX_VALUE, Long.MAX_VALUE);
        test(SamplesInterface.class, _tests, "IdentityInt64", Long.MIN_VALUE, Long.MIN_VALUE);
        i = r.nextInt();
        test(SamplesInterface.class, _tests, "IdentityInt64", (long) i, (long) i);

        test(SamplesInterface.class, _tests, "IdentityUInt16", new UInt16(0), new UInt16(0));
        test(SamplesInterface.class, _tests, "IdentityUInt16", new UInt16(1), new UInt16(1));
        test(SamplesInterface.class, _tests, "IdentityUInt16", new UInt16(UInt16.MAX_VALUE), new UInt16(UInt16.MAX_VALUE));
        test(SamplesInterface.class, _tests, "IdentityUInt16", new UInt16(UInt16.MIN_VALUE), new UInt16(UInt16.MIN_VALUE));
        i = r.nextInt();
        i = i > 0 ? i : -i;
        test(SamplesInterface.class, _tests, "IdentityUInt16", new UInt16(i % UInt16.MAX_VALUE), new UInt16(i % UInt16.MAX_VALUE));

        test(SamplesInterface.class, _tests, "IdentityUInt32", new UInt32(0), new UInt32(0));
        test(SamplesInterface.class, _tests, "IdentityUInt32", new UInt32(1), new UInt32(1));
        test(SamplesInterface.class, _tests, "IdentityUInt32", new UInt32(UInt32.MAX_VALUE), new UInt32(UInt32.MAX_VALUE));
        test(SamplesInterface.class, _tests, "IdentityUInt32", new UInt32(UInt32.MIN_VALUE), new UInt32(UInt32.MIN_VALUE));
        i = r.nextInt();
        i = i > 0 ? i : -i;
        test(SamplesInterface.class, _tests, "IdentityUInt32", new UInt32(i % UInt32.MAX_VALUE), new UInt32(i % UInt32.MAX_VALUE));

        test(SamplesInterface.class, _tests, "IdentityUInt64", new UInt64(0), new UInt64(0));
        test(SamplesInterface.class, _tests, "IdentityUInt64", new UInt64(1), new UInt64(1));
        test(SamplesInterface.class, _tests, "IdentityUInt64", new UInt64(UInt64.MAX_LONG_VALUE), new UInt64(UInt64.MAX_LONG_VALUE));
        test(SamplesInterface.class, _tests, "IdentityUInt64", new UInt64(UInt64.MAX_BIG_VALUE), new UInt64(UInt64.MAX_BIG_VALUE));
        test(SamplesInterface.class, _tests, "IdentityUInt64", new UInt64(UInt64.MIN_VALUE), new UInt64(UInt64.MIN_VALUE));
        i = r.nextInt();
        i = i > 0 ? i : -i;
        test(SamplesInterface.class, _tests, "IdentityUInt64", new UInt64(i % UInt64.MAX_LONG_VALUE), new UInt64(i % UInt64.MAX_LONG_VALUE));

        test(SamplesInterface.class, _tests, "IdentityDouble", 0.0, 0.0);
        test(SamplesInterface.class, _tests, "IdentityDouble", 1.0, 1.0);
        test(SamplesInterface.class, _tests, "IdentityDouble", -1.0, -1.0);
        test(SamplesInterface.class, _tests, "IdentityDouble", Double.MAX_VALUE, Double.MAX_VALUE);
        test(SamplesInterface.class, _tests, "IdentityDouble", Double.MIN_VALUE, Double.MIN_VALUE);
        i = r.nextInt();
        test(SamplesInterface.class, _tests, "IdentityDouble", (double) i, (double) i);

        test(SamplesInterface.class, _tests, "IdentityString", "", "");
        test(SamplesInterface.class, _tests, "IdentityString", "The Quick Brown Fox Jumped Over The Lazy Dog", "The Quick Brown Fox Jumped Over The Lazy Dog");
        test(SamplesInterface.class, _tests, "IdentityString", "ひらがなゲーム - かなぶん", "ひらがなゲーム - かなぶん");

        testArray(SamplesInterface.class, _tests, "IdentityBoolArray", Boolean.TYPE, null);
        testArray(SamplesInterface.class, _tests, "IdentityByteArray", Byte.TYPE, null);
        testArray(SamplesInterface.class, _tests, "IdentityInt16Array", Short.TYPE, null);
        testArray(SamplesInterface.class, _tests, "IdentityInt32Array", Integer.TYPE, null);
        testArray(SamplesInterface.class, _tests, "IdentityInt64Array", Long.TYPE, null);
        testArray(SamplesInterface.class, _tests, "IdentityDoubleArray", Double.TYPE, null);

        Variant<String> content = new Variant<>("aoeu");
        testArray(SamplesInterface.class, _tests, "IdentityArray", Variant.class, content);
        testArray(SamplesInterface.class, _tests, "IdentityUInt16Array", UInt16.class, new UInt16(12));
        testArray(SamplesInterface.class, _tests, "IdentityUInt32Array", UInt32.class, new UInt32(190));
        testArray(SamplesInterface.class, _tests, "IdentityUInt64Array", UInt64.class, new UInt64(103948));
        testArray(SamplesInterface.class, _tests, "IdentityStringArray", String.class, "asdf");

        int[] is = new int[0];
        test(SamplesInterface.class, _tests, "Sum", 0L, is);
        int len = r.nextInt() % 100 + 15;
        len = (len < 0 ? -len : len) + 15;
        is = new int[len];
        long result = 0;
        for (i = 0; i < len; i++) {
            is[i] = r.nextInt();
            result += is[i];
        }
        test(SamplesInterface.class, _tests, "Sum", result, is);

        byte[] bs = new byte[0];
        test(Binding.SingleSample.class, _singletests, "Sum", new UInt32(0), bs);
        len = r.nextInt() % 100;
        len = (len < 0 ? -len : len) + 15;
        bs = new byte[len];
        int res = 0;
        for (i = 0; i < len; i++) {
            bs[i] = (byte) r.nextInt();
            res += bs[i] < 0 ? bs[i] + 256 : bs[i];
        }
        test(Binding.SingleSample.class, _singletests, "Sum", new UInt32(res % (UInt32.MAX_VALUE + 1)), bs);

        test(SamplesInterface.class, _tests, "DeStruct",
                new Binding.Triplet<>("hi", new UInt32(12),
                        (short) 99), new CrossSampleStruct("hi", new UInt32(12),
                        (short) 99));

        Map<String, String> in = new HashMap<>();
        Map<String, List<String>> out = new HashMap<>();
        test(SamplesInterface.class, _tests, "InvertMapping", out, in);

        in.put("hi", "there");
        in.put("to", "there");
        in.put("from", "here");
        in.put("in", "out");
        List<String> l = new ArrayList<>();
        l.add("hi");
        l.add("to");
        out.put("there", l);
        l = new ArrayList<>();
        l.add("from");
        out.put("here", l);
        l = new ArrayList<>();
        l.add("in");
        out.put("out", l);
        test(SamplesInterface.class, _tests, "InvertMapping", out, in);

        primitizeTest(_tests, 1);
        primitizeTest(_tests, new Variant<>(new Variant<>(new Variant<>(new Variant<>("Hi")))));
        primitizeTest(_tests, new Variant<>(in, new DBusMapType(String.class, String.class)));

        test(SamplesInterface.class, _tests, "Trigger", null, "/Test", new UInt64(21389479283L));

        try {
            conn.sendMessage(new Trigger("/Test", new UInt16(15), 12.5));
        } catch (DBusException _ex) {

            LOGGER.debug("", _ex);

            throw new DBusExecutionException(_ex.getMessage(), _ex);
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException _ex) {
        }

        test(SamplesInterface.class, _tests, "Exit", null);
    }

    public void testArray(Class<? extends DBusInterface> _iface, Object _proxy, String _method, Class<? extends Object> _arrayType, Object _content) {
        Object array = Array.newInstance(_arrayType, 0);
        test(_iface, _proxy, _method, array, array);
        Random r = new Random();
        int l = r.nextInt() % 100;
        array = Array.newInstance(_arrayType, (l < 0 ? -l : l) + 15);
        if (null != _content) {
            Arrays.fill((Object[]) array, _content);
        }
        test(_iface, _proxy, _method, array, array);
    }

    public void compareArray(String _test, Object _a, Object _b) {
        if (!_a.getClass().equals(_b.getClass())) {
            fail(_test, "Incorrect return type; expected " + _a.getClass() + " got " + _b.getClass());
            return;
        }
        boolean pass = false;

        if (_a instanceof Object[]) {
            pass = Arrays.equals((Object[]) _a, (Object[]) _b);
        } else if (_a instanceof byte[]) {
            pass = Arrays.equals((byte[]) _a, (byte[]) _b);
        } else if (_a instanceof boolean[]) {
            pass = Arrays.equals((boolean[]) _a, (boolean[]) _b);
        } else if (_a instanceof int[]) {
            pass = Arrays.equals((int[]) _a, (int[]) _b);
        } else if (_a instanceof short[]) {
            pass = Arrays.equals((short[]) _a, (short[]) _b);
        } else if (_a instanceof long[]) {
            pass = Arrays.equals((long[]) _a, (long[]) _b);
        } else if (_a instanceof double[]) {
            pass = Arrays.equals((double[]) _a, (double[]) _b);
        }

        if (pass) {
            pass(_test);
        } else {
            String s = "Incorrect return value; expected ";
            s += collapseArray(_a);
            s += " got ";
            s += collapseArray(_b);
            fail(_test, s);
        }
    }

    public Set<String> getPassed() {
        return passed;
    }

    public Map<String, List<String>> getFailed() {
        return failed;
    }

}
