/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.inner;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Exec {
    public int fuss = 1;

    public void addF() {
        this.fuss += 2;
    }

    public class Inner {
        int i;

        public Inner(int n) {
            this.i = n;
        }

        public void doAdd() {
            Inner.APRpppehpY("invoke", 883428775L, (Exec)Exec.this);
            Exec.this.fuss += this.i;
        }

        private static /* synthetic */ CallSite APRpppehpY(MethodHandles.Lookup lookup, String string, MethodType methodType, long l) throws Exception {
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
                            String string3 = "pack.tests.basics.inner.Exec";
                            string2 = "addF";
                            clazz = lookup.findClass(string3);
                            if (n2 == 617898324) break block1;
                            if (n2 == 883428775) break block2;
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
}

