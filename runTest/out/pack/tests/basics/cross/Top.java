/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.cross;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import pack.tests.basics.cross.Abst1;
import pack.tests.basics.cross.Inte;

public class Top
extends Abst1
implements Inte {
    public void run() {
        if (Top.qAtXjKAxB("invoke", 348137635L, (Top)this, 1, (int)2) == 3) {
            // empty if block
        }
        Integer.reverse(0xF39A97DA ^ 0xEFCF3F63 ^ 0xE4DA807D ^ 0xEFCF3F63);
        Integer.numberOfLeadingZeros(390076327);
        System.identityHashCode("seed");
        System.out.println("FAIL");
    }

    @Override
    public int add(int n, int n2) {
        return n + n2;
    }

    private static /* synthetic */ CallSite qAtXjKAxB(MethodHandles.Lookup lookup, String string, MethodType methodType, long l) throws Exception {
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
                                string3 = "pack.tests.basics.cross.Top";
                                string2 = "add";
                                break block3;
                            }
                            string3 = "pack.tests.basics.cross.Top";
                            string2 = "mul";
                        }
                        clazz = lookup.findClass(string3);
                        if (n == 1376124816) break block4;
                        if (n == 348137635) break block5;
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

