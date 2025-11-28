/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.runable;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.RejectedExecutionException;
import pack.tests.basics.runable.Exec;
import pack.tests.basics.runable.Pool;

public class Task {
    private static /* synthetic */ void lambda$run$0(Exec exec) {
        int n = Exec.i;
        Task.uQImShEQZL("invoke", 1604502172L, (Exec)exec);
        Exec.i += n;
    }

    /*
     * Unable to fully structure code
     */
    public void run() throws Exception {
        var1_1 = new Exec(2);
        var2_2 = new Exec(3);
        var3_3 = new Exec(100);
        try {
            Pool.tpe.submit((Runnable)LambdaMetafactory.metafactory(null, null, null, ()V, doAdd(), ()V)((Exec)var2_2));
            try {
                Thread.sleep(50L);
            }
            catch (InterruptedException var4_4) {
                // empty catch block
            }
            switch (314 + -75) {
                case 808: {
                    "fake".hashCode();
                    ** GOTO lbl22
                }
                case 483: {
                    ** GOTO lbl22
                }
                case 617: {
                    Integer.bitCount(1824319496);
                }
lbl22:
                // 4 sources

                default: {
                    throw new IllegalStateException();
                }
                case 239: 
            }
            Integer.reverse(-2048283705 ^ 1071130398 ^ -1785630829 ^ 1071130398);
            Integer.numberOfLeadingZeros(276350037);
            System.identityHashCode("seed");
            Pool.tpe.submit((Runnable)LambdaMetafactory.metafactory(null, null, null, ()V, lambda$run$0(pack.tests.basics.runable.Exec ), ()V)((Exec)var1_1));
            try {
                Thread.sleep(50L);
                "proxy_de11e00".hashCode();
            }
            catch (InterruptedException var4_5) {
            }
            Integer.bitCount(232857088);
            Integer.reverse(-1737461357 ^ 1071130398 ^ -1785630829 ^ 1071130398);
            Pool.tpe.submit((Runnable)LambdaMetafactory.metafactory(null, null, null, ()V, doAdd(), ()V)((Exec)var3_3));
        }
        catch (RejectedExecutionException var4_6) {
            Exec.i += 10;
        }
        switch (476332717 ^ 476332587) {
            case 515: {
                Integer.bitCount(-525266109);
                ** GOTO lbl63
            }
            case 236: {
                ** GOTO lbl63
            }
            case 100: {
                "fake".hashCode();
            }
lbl63:
            // 4 sources

            default: {
                throw new RuntimeException("6652039933864760159");
            }
            case 134: 
        }
        if ((-1685143649 ^ 1071130398 ^ -1785630829 ^ 1071130398) + 1202431050 != 1439396950) {
            throw new Error("1779390141862003118");
        }
        Thread.sleep(300L);
        if (Exec.i == 30) {
            // empty if block
        }
        -1438037875 ^ 1071130398 ^ -1785630829 ^ 1071130398 ^ (4613725 ^ 1071130398 ^ -1785630829 ^ 1071130398);
        System.out.println("FAIL");
    }

    private static /* synthetic */ CallSite uQImShEQZL(MethodHandles.Lookup lookup, String string, MethodType methodType, long l) throws Exception {
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
                        String string3 = "pack.tests.basics.runable.Exec";
                        string2 = "doAdd";
                        clazz = lookup.findClass(string3);
                        if (n2 == 589836216) break block1;
                        if (n2 == 1604502172) break block2;
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

