/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.runable;

public class Exec {
    public static int i = 1;
    private int d;

    public Exec(int n) {
        this.d = n;
    }

    /*
     * Unable to fully structure code
     */
    void doAdd() {
        try {
            Thread.sleep(200L);
        }
        catch (Exception var1_1) {
            // empty catch block
        }
        switch (471 + -134) {
            case 894: {
                ** GOTO lbl11
            }
            case 568: {
                "fake".hashCode();
                ** GOTO lbl11
            }
            ** case 111:
lbl11:
            // 4 sources

            default: {
                throw new IllegalStateException();
            }
            case 337: 
        }
        Exec.i += this.d;
    }
}

