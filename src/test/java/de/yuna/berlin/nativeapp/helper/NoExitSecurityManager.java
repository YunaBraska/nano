package de.yuna.berlin.nativeapp.helper;

import java.security.Permission;

public class NoExitSecurityManager extends SecurityManager {
    private final SecurityManager originalSecurityManager;
    private int statusCode;

    public static int catchSystemExit(final ExRunnable runnable) throws Exception {
        final SecurityManager originalSecurityManager = System.getSecurityManager();
        final NoExitSecurityManager noExitSecurityManager = new NoExitSecurityManager(originalSecurityManager);
        try {
            System.setSecurityManager(noExitSecurityManager);
            runnable.run();
            throw new AssertionError("SecurityException for System.exit was not thrown");
        } catch (final SecurityException e) {
            return noExitSecurityManager.getStatusCode();
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }

    public NoExitSecurityManager(final SecurityManager originalSecurityManager) {
        this.originalSecurityManager = originalSecurityManager;
    }

    @Override
    public void checkPermission(final Permission perm) {
        // Allow all other activities by default
    }

    @Override
    public void checkExit(final int status) {
        this.statusCode = status;
        throw new SecurityException("System exit prevented");
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public SecurityManager getOriginalSecurityManager() {
        return this.originalSecurityManager;
    }
}

