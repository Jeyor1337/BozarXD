/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.retrace;

public class Tracee {
    public static int p = 0;

    public void toTrace(int n) throws Exception {
        if (n == 0) {
            // empty if block
        }
        int cfr_ignored_0 = 0xDE08A7C5 ^ (0x184E7B6E ^ 0xDE08A7C5 ^ 0x3937C877 ^ 0xDE08A7C5);
        int n2 = n;
        this.doTrace$2ce8(0x158EF41 ^ 0xDE08A7C5 ^ 0x3937C877 ^ 0xDE08A7C5, n2, 1225581776);
    }

    private /* synthetic */ void doTrace$2ce8(int n, int n2, long l) throws Exception {
        if ((n ^ (int)l) != 1902363622) {
            throw new RuntimeException("Verification failed");
        }
        ++p;
        StackTraceElement stackTraceElement = new Throwable().getStackTrace()[1];
        Tracee.class.getDeclaredMethod(stackTraceElement.getMethodName(), Integer.TYPE).invoke((Object)this, n2 - 1);
    }
}

