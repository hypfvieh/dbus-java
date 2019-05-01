/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DBusMap<K, V> implements Map<K, V> {
    // CHECKSTYLE:OFF
    Object[][] entries;
    // CHECKSTYLE:ON
    public DBusMap(Object[][] _entries) {
        this.entries = _entries;
    }

    class Entry implements Map.Entry<K, V>, Comparable<Entry> {
        private int entry;

        Entry(int i) {
            this.entry = i;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object o) {
            if (null == o) {
                return false;
            }
            if (!(o instanceof DBusMap.Entry)) {
                return false;
            }
            return this.entry == ((Entry) o).entry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public K getKey() {
            return (K) entries[entry][0];
        }

        @Override
        @SuppressWarnings("unchecked")
        public V getValue() {
            return (V) entries[entry][1];
        }

        @Override
        public int hashCode() {
            return entries[entry][0].hashCode();
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareTo(Entry e) {
            return entry - e.entry;
        }
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        for (Object[] entrie : entries) {
            if (key == entrie[0] || (key != null && key.equals(entrie[0]))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Object[] entrie : entries) {
            if (value == entrie[1] || (value != null && value.equals(entrie[1]))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> s = new TreeSet<Map.Entry<K, V>>();
        for (int i = 0; i < entries.length; i++) {
            s.add(new Entry(i));
        }
        return s;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        for (Object[] entrie : entries) {
            if (key == entrie[0] || (key != null && key.equals(entrie[0]))) {
                return (V) entrie[1];
            }
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return entries.length == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<K> keySet() {
        Set<K> s = new TreeSet<K>();
        for (Object[] entry : entries) {
            s.add((K) entry[0]);
        }
        return s;
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return entries.length;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<V> values() {
        List<V> l = new ArrayList<>();
        for (Object[] entry : entries) {
            l.add((V) entry[1]);
        }
        return l;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(entries);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (null == o) {
            return false;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        return ((Map<K, V>) o).entrySet().equals(entrySet());
    }

    @Override
    public String toString() {
        String s = "{ ";
        for (Object[] entrie : entries) {
            s += entrie[0] + " => " + entrie[1] + ",";
        }
        return s.replaceAll(".$", " }");
    }
}
