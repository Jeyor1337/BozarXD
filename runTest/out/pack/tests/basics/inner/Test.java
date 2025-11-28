/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.inner;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import pack.tests.basics.inner.Exec;

public class Test {
    /*
     * Unable to fully structure code
     */
    public void run() {
        block7: {
            block6: {
                v0 = var1_1 = new Exec();
                v0.getClass();
                var2_2 = new Exec.Inner(v0, -1007472062 ^ -624207599 ^ -1007472063 ^ -624207599);
                Test.eMVqaaDbqS("invoke", 1255188036L, (Exec.Inner)var2_2);
                v1 = var1_1;
                v1.getClass();
                var3_3 = new Exec.Inner(v1, 100);
                Test.eMVqaaDbqS("invoke", 1255188036L, (Exec.Inner)var3_3);
                if (var1_1.fuss != 108) break block6;
                System.out.println("PASS");
                switch (7021 % 1000) {
                    case 351: {
                        Integer.bitCount(1047758968);
                        ** GOTO lbl21
                    }
                    case 170: {
                        ** GOTO lbl21
                    }
                    ** case 873:
lbl21:
                    // 4 sources

                    default: {
                        throw null;
                    }
                    case 21: 
                }
                break block7;
            }
            System.out.println("ERROR");
        }
    }

    private static /* synthetic */ CallSite eMVqaaDbqS(MethodHandles.Lookup lookup, String string, MethodType methodType, long l) throws Exception {
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
                        String string3 = "pack.tests.basics.inner.Exec$Inner";
                        string2 = "doAdd";
                        clazz = lookup.findClass(string3);
                        if (n2 == 1732740957) break block1;
                        if (n2 == 1255188036) break block2;
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

