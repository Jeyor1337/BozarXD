/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.runable;

public class Exec {
    public static int i = 1;
    private int d;

    public Exec(int delta) {
        this.d = delta;
    }

    void doAdd() {
        try {
            Thread.sleep(200L);
        }
        catch (Exception exception) {
            // empty catch block
        }
        i += this.d;
    }
}

