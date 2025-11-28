/*
 * Decompiled with CFR 0.152.
 */
package pack;

import java.io.File;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import pack.tests.basics.accu.Digi;
import pack.tests.basics.ctrl.Ctrl;
import pack.tests.basics.inner.Test;
import pack.tests.basics.overwirte.Sub;
import pack.tests.basics.runable.Task;
import pack.tests.basics.sub.Solver;
import pack.tests.reflects.annot.annot;
import pack.tests.reflects.counter.Count;
import pack.tests.reflects.field.FTest;
import pack.tests.reflects.loader.LRun;
import pack.tests.reflects.res.Accesor;
import pack.tests.reflects.retrace.Tracer;
import pack.tests.security.SecTest;

public class Main {
    /*
     * Recovered potentially malformed switches.  Disable with '--allowmalformedswitch false'
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static void main(String[] var0) throws Exception {
        System.out.println("Obfuscator Test Program");
        System.out.println("Author: huzpsb");
        System.out.println("Version: 1.0r");
        System.out.println("Link: https://github.com/huzpsb/JavaObfuscatorTest");
        var1_1 = new File("IK");
        if (!var1_1.exists()) {
            // empty if block
        }
        System.out.println("-------------Test #1: Basics-------------");
        System.out.print("Test 1.1: Inheritance ");
        try {
            Main.TGslaiyAkKi("invoke", 1775938093L, (Sub)new Sub());
        }
        catch (Throwable var2_2) {
            System.out.println("ERROR");
        }
        Integer.rotateLeft(382633441 * -1640531535, 13);
        "proxy_16ce85e1".hashCode();
        Integer.bitCount(1294451556 ^ -995277911 ^ 1542014597 ^ -995277911);
        System.out.print("Test 1.2: Cross ");
        try {
            Main.TGslaiyAkKi("invoke", 1775938093L, (Sub)new Sub());
        }
        catch (Throwable var2_3) {
            System.out.println("ERROR");
        }
        if ((Integer.rotateLeft(-1595600761 ^ -995277911 ^ 1542014597 ^ -995277911, 1542014594 ^ -995277911 ^ 1542014597 ^ -995277911) ^ -1150973827) + 2004156298 != (-282407409 ^ -995277911 ^ 1542014597 ^ -995277911)) {
            throw null;
        }
        System.out.print("Test 1.3: Throw ");
        try {
            Main.TGslaiyAkKi("invoke", 6070905389L, (Ctrl)new Ctrl());
        }
        catch (Throwable var2_4) {
            System.out.println("ERROR");
        }
        switch (565) {
            case 764: {
                ** break;
            }
            case 849: {
                Integer.bitCount(1010386246);
                ** break;
            }
            ** case 486:
lbl42:
            // 4 sources

            default: {
                throw new Error();
            }
            case 565: 
        }
        System.out.print("Test 1.4: Accuracy ");
        try {
            Main.TGslaiyAkKi("invoke", 10365872685L, (Digi)new Digi());
        }
        catch (Throwable var2_5) {
            System.out.println("ERROR");
        }
        Integer.numberOfLeadingZeros(1823271132 ^ -995277911 ^ 1542014597 ^ -995277911);
        System.identityHashCode("seed");
        -995277911 ^ (-1476285579 ^ -995277911 ^ 1542014597 ^ -995277911);
        System.out.print("Test 1.5: SubClass ");
        try {
            new Solver();
        }
        catch (Throwable var2_6) {
            System.out.println("ERROR");
        }
        switch (820) {
            case 559: {
                Integer.bitCount(1460682157);
                ** break;
            }
            case 879: {
                "fake".hashCode();
                ** break;
            }
            case 762: {
                Integer.bitCount(-1277834056);
            }
lbl78:
            // 4 sources

            default: {
                throw new RuntimeException();
            }
            case 820: 
        }
        Integer.rotateLeft((337597823 ^ -995277911 ^ 1542014597 ^ -995277911) * (-975290572 ^ -995277911 ^ 1542014597 ^ -995277911), 13);
        "proxy_4ff613fa".hashCode();
        Integer.bitCount(337597823 ^ -995277911 ^ 1542014597 ^ -995277911);
        System.out.print("Test 1.6: Pool ");
        try {
            Main.TGslaiyAkKi("invoke", 14660839981L, (Task)new Task());
        }
        catch (Throwable var2_7) {
            System.out.println("ERROR");
        }
        System.out.print("Test 1.7: InnerClass ");
        try {
            Main.TGslaiyAkKi("invoke", 18955807277L, (Test)new Test());
        }
        catch (Throwable var2_8) {
            System.out.println("ERROR");
        }
        System.out.println("-------------Test #2: Reflects-------------");
        System.out.print("Test 2.1: Counter ");
        try {
            Main.TGslaiyAkKi("invoke", 23250774573L, (Count)new Count());
        }
        catch (Throwable var2_9) {
            System.out.println("ERROR");
        }
        switch (272 + -56) {
            case 645: {
                "fake".hashCode();
                ** break;
            }
            case 134: {
                ** break;
            }
            case 842: {
                "fake".hashCode();
            }
lbl117:
            // 4 sources

            default: {
                throw null;
            }
            case 216: 
        }
        if ((Integer.rotateLeft(-490574013 ^ -995277911 ^ 1542014597 ^ -995277911, 1542014594 ^ -995277911 ^ 1542014597 ^ -995277911) ^ (429806404 ^ -995277911 ^ 1542014597 ^ -995277911)) + (-185509251 ^ -995277911 ^ 1542014597 ^ -995277911) != -2030300267) {
            throw null;
        }
        "proxy_22ac6313".hashCode();
        Integer.bitCount(2034574742 ^ -995277911 ^ 1542014597 ^ -995277911);
        Integer.reverse(2034574742 ^ -995277911 ^ 1542014597 ^ -995277911);
        System.out.print("Test 2.2: Chinese \u901a\u8fc7LMAO\b\b\b\b    \n");
        System.out.print("Test 2.3: Resource ");
        try {
            Main.TGslaiyAkKi("invoke", 27545741869L, (Accesor)new Accesor());
        }
        catch (Throwable var2_10) {
            System.out.println("ERROR");
        }
        System.out.print("Test 2.4: Field ");
        try {
            Main.TGslaiyAkKi("invoke", 31840709165L, (FTest)new FTest());
        }
        catch (Throwable var2_11) {
            System.out.println("ERROR");
        }
        switch (3136 % 1000) {
            case 541: {
                Integer.bitCount(916654131);
                ** break;
            }
            case 320: {
                Integer.bitCount(1261945650);
                ** break;
            }
            case 775: {
                Integer.bitCount(398476508);
            }
lbl154:
            // 4 sources

            default: {
                throw null;
            }
            case 136: 
        }
        System.out.print("Test 2.5: Loader ");
        try {
            Main.TGslaiyAkKi("invoke", 36135676461L, (LRun)new LRun());
        }
        catch (Throwable var2_12) {
            System.out.println("ERROR");
        }
        switch (74 + 801) {
            case 533: {
                ** break;
            }
            case 414: {
                Integer.bitCount(-1886342400);
                ** break;
            }
            ** case 488:
lbl172:
            // 4 sources

            default: {
                throw new IllegalStateException("5445783501528743254");
            }
            case 875: 
        }
        Integer.numberOfLeadingZeros(1703921372 ^ -995277911 ^ 1542014597 ^ -995277911);
        System.identityHashCode("seed");
        -995277911 ^ (-1591573132 ^ -995277911 ^ 1542014597 ^ -995277911);
        System.out.print("Test 2.6: ReTrace ");
        try {
            Main.TGslaiyAkKi("invoke", 40430643757L, (Tracer)new Tracer());
        }
        catch (Throwable var2_13) {
            System.out.println("ERROR");
        }
        Integer.rotateLeft((2124207039 ^ -995277911 ^ 1542014597 ^ -995277911) * (-975290572 ^ -995277911 ^ 1542014597 ^ -995277911), 13);
        "proxy_2575953a".hashCode();
        Integer.bitCount(2124207039 ^ -995277911 ^ 1542014597 ^ -995277911);
        System.out.print("Test 2.7: Annotation ");
        Main.TGslaiyAkKi("invoke", 44725611053L, (annot)new annot());
        {
            catch (Throwable var2_14) {
                System.out.println("ERROR");
                break;
            }
        }
        switch (3398 % 1000) {
            case 848: {
                ** break;
            }
            case 943: {
                ** break;
            }
            ** case 518:
lbl210:
            // 4 sources

            default: {
                throw new Error();
            }
            case 398: 
        }
        System.out.print("Test 2.8: Sec ");
        try {
            Main.TGslaiyAkKi("invoke", 49020578349L, (SecTest)new SecTest());
        }
        catch (Throwable var2_15) {
            System.out.println("ERROR");
        }
        System.out.println("-------------Test #3: Efficiency-------------");
        Main.TGslaiyAkKi("invoke", 52469340483L);
        System.out.println("-------------Tests r Finished-------------");
    }

    private static /* synthetic */ CallSite TGslaiyAkKi(MethodHandles.Lookup lookup, String string, MethodType methodType, long l) throws Exception {
        MethodHandle methodHandle;
        block17: {
            block13: {
                Class<?> clazz;
                String string2;
                block16: {
                    block15: {
                        String string3;
                        int n;
                        block14: {
                            block12: {
                                block11: {
                                    block10: {
                                        block9: {
                                            block8: {
                                                block7: {
                                                    block6: {
                                                        block5: {
                                                            block4: {
                                                                block3: {
                                                                    block2: {
                                                                        block1: {
                                                                            block0: {
                                                                                int n2 = (int)(l >>> 32);
                                                                                n = (int)(l & 0xFFFFFFFFL);
                                                                                if (n2 == 0) break block0;
                                                                                if (n2 == 1) break block1;
                                                                                if (n2 == 2) break block2;
                                                                                if (n2 == 3) break block3;
                                                                                if (n2 == 4) break block4;
                                                                                if (n2 == 5) break block5;
                                                                                if (n2 == 6) break block6;
                                                                                if (n2 == 7) break block7;
                                                                                if (n2 == 8) break block8;
                                                                                if (n2 == 9) break block9;
                                                                                if (n2 == 10) break block10;
                                                                                if (n2 == 11) break block11;
                                                                                if (n2 == 12) break block12;
                                                                                break block13;
                                                                            }
                                                                            string3 = "pack.tests.basics.overwirte.Sub";
                                                                            string2 = "run";
                                                                            break block14;
                                                                        }
                                                                        string3 = "pack.tests.basics.ctrl.Ctrl";
                                                                        string2 = "run";
                                                                        break block14;
                                                                    }
                                                                    string3 = "pack.tests.basics.accu.Digi";
                                                                    string2 = "run";
                                                                    break block14;
                                                                }
                                                                string3 = "pack.tests.basics.runable.Task";
                                                                string2 = "run";
                                                                break block14;
                                                            }
                                                            string3 = "pack.tests.basics.inner.Test";
                                                            string2 = "run";
                                                            break block14;
                                                        }
                                                        string3 = "pack.tests.reflects.counter.Count";
                                                        string2 = "run";
                                                        break block14;
                                                    }
                                                    string3 = "pack.tests.reflects.res.Accesor";
                                                    string2 = "run";
                                                    break block14;
                                                }
                                                string3 = "pack.tests.reflects.field.FTest";
                                                string2 = "run";
                                                break block14;
                                            }
                                            string3 = "pack.tests.reflects.loader.LRun";
                                            string2 = "run";
                                            break block14;
                                        }
                                        string3 = "pack.tests.reflects.retrace.Tracer";
                                        string2 = "run";
                                        break block14;
                                    }
                                    string3 = "pack.tests.reflects.annot.annot";
                                    string2 = "run";
                                    break block14;
                                }
                                string3 = "pack.tests.security.SecTest";
                                string2 = "run";
                                break block14;
                            }
                            string3 = "pack.tests.bench.Calc";
                            string2 = "runAll";
                        }
                        clazz = lookup.findClass(string3);
                        if (n == 929732931) break block15;
                        if (n == 1775938093) break block16;
                        break block13;
                    }
                    methodHandle = lookup.findStatic(clazz, string2, methodType);
                    break block17;
                }
                methodHandle = lookup.findVirtual(clazz, string2, methodType.dropParameterTypes(0, 1));
                break block17;
            }
            throw new IllegalStateException();
        }
        return new ConstantCallSite(methodHandle);
    }
}

