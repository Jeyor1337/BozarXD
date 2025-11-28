/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.accu;

public class Digi {
    public void run() {
        double fl = 0.0;
        int co = 0;
        float fx = 1.1f;
        while (++co <= 100 && (float)(fl += 1.0E-18) != 2.0E-17f) {
        }
        if (co == 20) {
            if ((fx += 1.3f) == 2.4f) {
                System.out.println("PASS");
                return;
            }
            System.out.println("FAIL");
        } else {
            System.out.println("FAIL");
        }
    }
}

