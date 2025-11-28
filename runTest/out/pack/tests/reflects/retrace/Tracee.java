/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.retrace;

public class Tracee {
    public static int p = 0;

    private void doTrace(int i) throws Exception {
        ++p;
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        Tracee.class.getDeclaredMethod(ste.getMethodName(), Integer.TYPE).invoke((Object)this, i - 1);
    }

    public void toTrace(int i) throws Exception {
        if (i == 0) {
            return;
        }
        this.doTrace(i);
    }
}

