/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.ctrl;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Ctrl {
    private String ret = "FAIL";

    /*
     * Unable to fully structure code
     */
    public void runf() {
        block11: {
            try {
                Ctrl.GrDjDHFI("invoke", 919095973L, (Ctrl)this);
                if (29874516 + (463230494 ^ -282907833 ^ 1835287514 ^ -282907833) == (366718146 ^ -282907833 ^ 1835287514 ^ -282907833)) break block11;
            }
            catch (RuntimeException var1_1) {
                this.ret = "PASS";
            }
            throw null;
        }
        System.identityHashCode("seed");
        -2109249379 ^ -282907833 ^ 1835287514 ^ -282907833 ^ -598961295;
        Integer.rotateLeft(862959670 * -1640531535, 13);
        try {
            Ctrl.GrDjDHFI("invoke", 919095973L, (Ctrl)this);
            this.ret = "FAIL";
        }
        catch (Exception var1_2) {
            // empty catch block
        }
        switch (4358 % 1000) {
            case 865: {
                ** GOTO lbl29
            }
            case 252: {
                Integer.bitCount(1376839166);
                ** GOTO lbl29
            }
            case 866: {
                "fake".hashCode();
            }
lbl29:
            // 4 sources

            default: {
                throw null;
            }
            case 358: 
        }
        if ((-829465987 ^ -282907833 ^ 1835287514 ^ -282907833 ^ (-1238325051 ^ -282907833 ^ 1835287514 ^ -282907833)) != 2025845432) {
            throw new Error();
        }
        -282907833 ^ 1207871901;
    }

    public void runt() {
        if ("a".equals("b")) {
            try {
                if ((0xEF232B47 ^ 0x16C6A20B) != -102397620) {
                    throw null;
                }
                throw new ArrayStoreException();
            }
            catch (ArrayStoreException arrayStoreException) {}
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void run() {
        Ctrl.GrDjDHFI("invoke", 5214063269L, (Ctrl)this);
        System.out.println(this.ret);
    }

    private static /* synthetic */ CallSite GrDjDHFI(MethodHandles.Lookup lookup, String string, MethodType methodType, long l) throws Exception {
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
                                string3 = "pack.tests.basics.ctrl.Ctrl";
                                string2 = "runt";
                                break block3;
                            }
                            string3 = "pack.tests.basics.ctrl.Ctrl";
                            string2 = "runf";
                        }
                        clazz = lookup.findClass(string3);
                        if (n == 1582078030) break block4;
                        if (n == 919095973) break block5;
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

