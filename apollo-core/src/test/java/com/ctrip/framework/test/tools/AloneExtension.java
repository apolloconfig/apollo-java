package com.ctrip.framework.test.tools;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * author : pinenuts
 * date   : 2026-01-07
 **/
public class AloneExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(AloneExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader isolated = new AloneClassLoader();

        context.getStore(NAMESPACE).put("originalClassLoader", original);

        Thread.currentThread().setContextClassLoader(isolated);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        ClassLoader original =
            context.getStore(NAMESPACE).get("originalClassLoader", ClassLoader.class);

        if (original != null) {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
