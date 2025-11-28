/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class LTest {
    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] byArray = new byte[1024];
        int n = inputStream.read(byArray);
        if (n != -1) {
            // empty if block
        }
        return byteArrayOutputStream.toByteArray();
    }

    public void run() throws Exception {
        System.out.println(new String((byte[])LTest.WWbwhZlLo("invoke", 1419364270L, (InputStream)LTest.class.getResourceAsStream("TEST"))));
    }

    private static /* synthetic */ CallSite WWbwhZlLo(MethodHandles.Lookup lookup, String string, MethodType methodType, long l) throws Exception {
        MethodHandle methodHandle;
        block3: {
            block0: {
                Class<?> clazz;
                String string2;
                block2: {
                    block1: {
                        int n = (int)(l >>> 32);
                        int n2 = (int)(l & 0xFFFFFFFFL);
                        if (n != 0) break block0;
                        String string3 = "pack.tests.reflects.loader.LTest";
                        string2 = "readAllBytes";
                        clazz = lookup.findClass(string3);
                        if (n2 == 1419364270) break block1;
                        if (n2 == 270932149) break block2;
                        break block0;
                    }
                    methodHandle = lookup.findStatic(clazz, string2, methodType);
                    break block3;
                }
                methodHandle = lookup.findVirtual(clazz, string2, methodType.dropParameterTypes(0, 1));
                break block3;
            }
            throw new IllegalStateException();
        }
        return new ConstantCallSite(methodHandle);
    }
}

