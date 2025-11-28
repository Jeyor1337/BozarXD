/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.retrace;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import pack.tests.reflects.retrace.Tracee;

public class Tracer {
    public void run() throws Exception {
        Tracer.TwZNPrVeFFv("invoke", 524069876L, (Tracee)new Tracee(), 5);
        if (Tracee.p == (0xCBA3AC6E ^ 0x591C202A ^ 0xCBA3AC6B ^ 0x591C202A)) {
            System.out.println("PASS");
            try {
                if ((0x591C202A ^ 0xCBA3AC6B ^ 0xBAA6B8EE) != 672740527) {
                    throw null;
                }
                throw new IllegalArgumentException();
            }
            catch (IllegalArgumentException illegalArgumentException) {
                if ((0xA073B321 ^ 0x591C202A ^ 0xCBA3AC6B ^ 0x591C202A ^ 0xFADB40D1) != (0x5AA8F3F0 ^ 0x591C202A ^ 0xCBA3AC6B ^ 0x591C202A)) {
                    throw new RuntimeException("4788103888377493566");
                }
            }
        } else {
            System.out.println("FAIL");
        }
    }

    private static /* synthetic */ CallSite TwZNPrVeFFv(MethodHandles.Lookup lookup, String string, MethodType methodType, long l) throws Exception {
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
                        String string3 = "pack.tests.reflects.retrace.Tracee";
                        string2 = "toTrace";
                        clazz = lookup.findClass(string3);
                        if (n2 == 639364660) break block1;
                        if (n2 == 524069876) break block2;
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

