/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.bench;

public class Calc {
    public static int count = 0;

    public static void runAll() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; ++i) {
            Calc.call(100);
            Calc.runAdd();
            Calc.runStr();
        }
        System.out.println("Calc: " + (System.currentTimeMillis() - start) + "ms");
        if (count != 30000) {
            throw new RuntimeException("[ERROR]: Errors occurred in calc!");
        }
    }

    private static void call(int i) {
        if (i == 0) {
            ++count;
        } else {
            Calc.call(i - 1);
        }
    }

    private static void runAdd() {
        for (double i = 0.0; i < 100.1; i += 0.99) {
        }
        ++count;
    }

    private static void runStr() {
        String str = "";
        while (str.length() < 101) {
            str = str + "ax";
        }
        ++count;
    }
}

