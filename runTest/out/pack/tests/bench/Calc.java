/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.bench;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Calc {
    public static int count = 0;

    public static void runAll() {
        long l = System.currentTimeMillis();
        for (int i = 0; i < 10000; ++i) {
            int n = 100;
            Calc.TfgncqZRBV("invoke", 2111275631L, (int)n, 2039836759, (int)(0xBCAB524 ^ 0x92CF3AF5 ^ 0x66B361F7 ^ 0x92CF3AF5), (int)(0x4F29965 ^ 0x92CF3AF5 ^ 0x66B361F7 ^ 0x92CF3AF5));
            Calc.TfgncqZRBV("invoke", 6406242927L, (long)1629891203, 0x6C9B4599 ^ 0x92CF3AF5 ^ 0x66B361F7 ^ 0x92CF3AF5);
            Calc.TfgncqZRBV("invoke", 10701210223L, (long)1222907822, 1386456307, (long)(0x67A0276C ^ 0x92CF3AF5 ^ 0x66B361F7 ^ 0x92CF3AF5), (int)(0x46420C80 ^ 0x92CF3AF5 ^ 0x66B361F7 ^ 0x92CF3AF5));
        }
        System.out.println("Calc: " + (System.currentTimeMillis() - l) + "ms");
        if (count != (0x66B314C7 ^ 0x92CF3AF5 ^ 0x66B361F7 ^ 0x92CF3AF5)) {
            // empty if block
        }
    }

    private static /* bridge */ /* synthetic */ void call(int n) {
        Calc.call$a57f(n, 2039836759, 1836700883, 1648490642);
    }

    private static /* bridge */ /* synthetic */ void runStr() {
        Calc.runStr$2aa(1222907822, 1386456307, 18040475, 552693111);
    }

    private static /* bridge */ /* synthetic */ void runAdd() {
        Calc.runAdd$ecee(1629891203, 170402926);
    }

    private static /* synthetic */ void runAdd$ecee(long l, long l2) {
        if (((int)l ^ (int)l2) != 1796083437) {
            throw new Error();
        }
        for (double d = 0.0; d < 100.1; d += 0.99) {
        }
        ++count;
    }

    private static /* synthetic */ void runStr$2aa(long l, long l2, long l3, int n) {
        if (((int)l ^ (int)l2 ^ (int)l3 ^ n) != 1000708273) {
            throw new Error();
        }
        String string = "";
        while (string.length() < (0x66B36192 ^ 0x92CF3AF5 ^ 0x66B361F7 ^ 0x92CF3AF5)) {
            string = string + "ax";
        }
        ++count;
    }

    private static /* synthetic */ void call$a57f(int n, int n2, int n3, int n4) {
        if ((n2 ^ n3 ^ n4) != 1991072790) {
            throw new Error();
        }
        if (n == 0) {
            // empty if block
        }
        int n5 = n - 1;
        Calc.TfgncqZRBV("invoke", 2111275631L, (int)n5, 0x1F2611A0 ^ 0x92CF3AF5 ^ 0x66B361F7 ^ 0x92CF3AF5, (int)(0xBCAB524 ^ 0x92CF3AF5 ^ 0x66B361F7 ^ 0x92CF3AF5), (int)(0x4F29965 ^ 0x92CF3AF5 ^ 0x66B361F7 ^ 0x92CF3AF5));
    }

    private static /* synthetic */ CallSite TfgncqZRBV(MethodHandles.Lookup lookup, String string, MethodType methodType, long l) throws Exception {
        MethodHandle methodHandle;
        block7: {
            block3: {
                Class<?> clazz;
                String string2;
                block6: {
                    block5: {
                        String string3;
                        int n;
                        block4: {
                            block2: {
                                block1: {
                                    block0: {
                                        int n2 = (int)(l >>> 32);
                                        n = (int)(l & 0xFFFFFFFFL);
                                        if (n2 == 0) break block0;
                                        if (n2 == 1) break block1;
                                        if (n2 == 2) break block2;
                                        break block3;
                                    }
                                    string3 = "pack.tests.bench.Calc";
                                    string2 = "call$a57f";
                                    break block4;
                                }
                                string3 = "pack.tests.bench.Calc";
                                string2 = "runAdd$ecee";
                                break block4;
                            }
                            string3 = "pack.tests.bench.Calc";
                            string2 = "runStr$2aa";
                        }
                        clazz = lookup.findClass(string3);
                        if (n == 2111275631) break block5;
                        if (n == 1226036642) break block6;
                        break block3;
                    }
                    methodHandle = lookup.findStatic(clazz, string2, methodType);
                    break block7;
                }
                methodHandle = lookup.findVirtual(clazz, string2, methodType.dropParameterTypes(0, 1));
                break block7;
            }
            throw new IllegalStateException();
        }
        return new ConstantCallSite(methodHandle);
    }
}

