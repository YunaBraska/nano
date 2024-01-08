package de.yuna.berlin.nativeapp.helper;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.AfterEachCallback;

public class PrintTestNamesExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(final ExtensionContext context) {
        System.out.println("########## START TEST [" + context.getTestMethod().map(method -> method.getName() + " - ").orElse("") + context.getDisplayName() + "] ##########");
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        System.out.println("########## STOP  TEST [" + context.getTestMethod().map(method -> method.getName() + " - ").orElse("") + context.getDisplayName() + "] ##########");
    }
}

