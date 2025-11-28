/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.security;

public class SecExec {
    private static /* bridge */ /* synthetic */ void doShutdown() {
        SecExec.doShutdown$d758(1610246293, 1364618479);
    }

    private static /* synthetic */ void doShutdown$d758(int n, int n2) {
        if ((n ^ n2) != 246154362) {
            throw new RuntimeException("Verification failed");
        }
        System.exit(-1);
    }
}

