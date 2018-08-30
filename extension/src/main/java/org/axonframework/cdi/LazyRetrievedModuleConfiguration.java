package org.axonframework.cdi;

import org.axonframework.config.Configuration;
import org.axonframework.config.ModuleConfiguration;

import java.util.function.Supplier;

/**
 * @author Milan Savic
 */
class LazyRetrievedModuleConfiguration implements ModuleConfiguration {

    private final Supplier<ModuleConfiguration> delegateSupplier;
    private ModuleConfiguration delegate;

    LazyRetrievedModuleConfiguration(Supplier<ModuleConfiguration> delegateSupplier) {
        this.delegateSupplier = delegateSupplier;
    }

    @Override
    public void initialize(Configuration config) {
        getDelegate().initialize(config);
    }

    @Override
    public void start() {
        getDelegate().start();
    }

    @Override
    public void shutdown() {
        getDelegate().shutdown();
    }

    @Override
    public int phase() {
        return getDelegate().phase();
    }

    @Override
    public ModuleConfiguration unwrap() {
        return getDelegate();
    }

    private ModuleConfiguration getDelegate() {
        if (delegate == null) {
            delegate = delegateSupplier.get();
        }
        return delegate;
    }
}
