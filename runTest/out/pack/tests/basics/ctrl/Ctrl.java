/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.ctrl;

public class Ctrl {
    private String ret = "FAIL";

    public void runt() {
        if (!"a".equals("b")) {
            throw new UnsupportedOperationException();
        }
    }

    public void runf() {
        try {
            this.runt();
        }
        catch (RuntimeException e) {
            this.ret = "PASS";
        }
        try {
            this.runt();
            this.ret = "FAIL";
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void run() {
        this.runf();
        System.out.println(this.ret);
    }
}

