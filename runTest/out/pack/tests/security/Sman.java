/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.security;

import java.security.Permission;

public class Sman
extends SecurityManager {
    @Override
    public void checkPermission(Permission permission) {
        if (permission.getName().contains("exitVM")) {
            // empty if block
        }
        System.identityHashCode("seed");
        int cfr_ignored_0 = 0x3BBB281 ^ 0xE6E4A3B9 ^ 0xE55F1138 ^ 0xE6E4A3B9 ^ 0xB9DF7204;
        Integer.rotateLeft((0xBA64C085 ^ 0xE6E4A3B9 ^ 0xE55F1138 ^ 0xE6E4A3B9) * (0x7B686889 ^ 0xE6E4A3B9 ^ 0xE55F1138 ^ 0xE6E4A3B9), 0xE55F1135 ^ 0xE6E4A3B9 ^ 0xE55F1138 ^ 0xE6E4A3B9);
    }
}

