/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.connections.impl.DirectConnection;
import org.freedesktop.dbus.types.UInt16;

public class P2pTestServer implements TestRemoteInterface {
    @Override
    public int[][] teststructstruct(TestStruct3 in) {
        List<List<Integer>> lli = in.b;
        int[][] out = new int[lli.size()][];
        for (int j = 0; j < out.length; j++) {
            out[j] = new int[lli.get(j).size()];
            for (int k = 0; k < out[j].length; k++) {
                out[j][k] = lli.get(j).get(k);
            }
        }
        return out;
    }

    @Override
    public String getNameAndThrow() {
        return getName();
    }

    @Override
    public String getName() {
        System.out.println("getName called");
        return "Peer2Peer Server";
    }

    @Override
    public <T> int frobnicate(List<Long> n, Map<String, Map<UInt16, Short>> m, T v) {
        return 3;
    }

    @Override
    public void throwme() throws TestException {
        System.out.println("throwme called");
        throw new TestException("BOO");
    }

    @Override
    public void waitawhile() {
        return;
    }

    @Override
    public int overload() {
        return 1;
    }

    @Override
    public void sig(Type[] s) {
    }

    @Override
    public void newpathtest(Path p) {
    }

    @Override
    public void reg13291(byte[] as, byte[] bs) {
    }

    @Override
    public Path pathrv(Path a) {
        return a;
    }

    @Override
    public List<Path> pathlistrv(List<Path> a) {
        return a;
    }

    @Override
    public Map<Path, Path> pathmaprv(Map<Path, Path> a) {
        return a;
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
    public float testfloat(float[] f) {
        System.out.println("got float: " + Arrays.toString(f));
        return f[0];
    }

    public static void main(String[] args) throws Exception {
        String address = DirectConnection.createDynamicSession();
        // String address = "tcp:host=localhost,port=12344,guid="+Transport.genGUID();
        PrintWriter w = new PrintWriter(new FileOutputStream("address"));
        w.println(address);
        w.flush();
        w.close();
        try (DirectConnection dc = new DirectConnection(address + ",listen=true")) {            
            System.out.println("Connected");
            dc.exportObject("/Test", new P2pTestServer());
        }
    }
}
