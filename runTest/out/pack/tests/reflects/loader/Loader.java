/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.loader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Loader
extends ClassLoader {
    @Override
    public Class<?> findClass(String string) throws ClassNotFoundException {
        CallSite callSite = Loader.NAGbnJUKeSs("invoke", 2105363873L, (InputStream)Loader.class.getClassLoader().getResourceAsStream("pack/tests/reflects/loader/LTest.class"));
        return Loader.NAGbnJUKeSs("invoke", 6405887739L, (Loader)this, (String)string, (byte[])callSite, (int)0, (int)((CallSite)callSite).length);
    }

    @Override
    public InputStream getResourceAsStream(String string) {
        if (string.contains("TEST")) {
            // empty if block
        }
        Integer.numberOfLeadingZeros(0x6BD6E0AD ^ 0xFBB8B144 ^ 0x6C2704C0 ^ 0xFBB8B144);
        System.identityHashCode("seed");
        int cfr_ignored_0 = 0xFBB8B144 ^ 0xFC495529;
        return super.getResourceAsStream(string);
    }

    public static byte[] readAllBytes(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] byArray = new byte[1024];
            int n = inputStream.read(byArray);
            if (n != -1) {
                // empty if block
            }
            int cfr_ignored_0 = 0xFBB8B144 ^ (0x578DF6A5 ^ 0xFBB8B144 ^ 0x6C2704C0 ^ 0xFBB8B144);
        }
        catch (Exception exception) {
            return null;
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static /* synthetic */ CallSite NAGbnJUKeSs(MethodHandles.Lookup lookup, String string, MethodType methodType, long l) throws Exception {
        MethodHandle methodHandle;
        block6: {
            block2: {
                Class<?> clazz;
                String string2;
                block5: {
                    block4: {
                        String string3;
                        int n;
                        block3: {
                            block1: {
                                block0: {
                                    int n2 = (int)(l >>> 32);
                                    n = (int)(l & 0xFFFFFFFFL);
                                    if (n2 == 0) break block0;
                                    if (n2 == 1) break block1;
                                    break block2;
                                }
                                string3 = "pack.tests.reflects.loader.Loader";
                                string2 = "readAllBytes";
                                break block3;
                            }
                            string3 = "pack.tests.reflects.loader.Loader";
                            string2 = "defineClass";
                        }
                        clazz = lookup.findClass(string3);
                        if (n == 2105363873) break block4;
                        if (n == 2110920443) break block5;
                        break block2;
                    }
                    methodHandle = lookup.findStatic(clazz, string2, methodType);
                    break block6;
                }
                methodHandle = lookup.findVirtual(clazz, string2, methodType.dropParameterTypes(0, 1));
                break block6;
            }
            throw new IllegalStateException();
        }
        return new ConstantCallSite(methodHandle);
    }
}

