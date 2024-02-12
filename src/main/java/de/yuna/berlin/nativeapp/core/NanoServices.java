package de.yuna.berlin.nativeapp.core;

import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_PARALLEL_SHUTDOWN;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SERVICE_REGISTER;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SERVICE_UNREGISTER;
import static java.util.Collections.unmodifiableList;

/**
 * The abstract base class for {@link Nano} framework providing {@link Service} handling functionalities.
 *
 * @param <T> The type of the {@link NanoServices} implementation, used for method chaining.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class NanoServices<T extends NanoServices<T>> extends NanoThreads<T> {

    protected final List<Service> services;

    /**
     * Initializes {@link NanoServices} with configurations and command-line arguments.
     *
     * @param config Configuration parameters for the {@link NanoServices} instance.
     * @param args   Command-line arguments passed during the application start.
     */
    protected NanoServices(final Map<Object, Object> config, final String... args) {
        super(config, args);
        this.services = new CopyOnWriteArrayList<>();
        addEventListener(EVENT_APP_SERVICE_REGISTER.id(), event -> event.payloadOpt(Service.class).map(this::registerService).ifPresent(nano -> event.acknowledge()));
        addEventListener(EVENT_APP_SERVICE_UNREGISTER.id(), event -> event.payloadOpt(Service.class).map(service -> unregisterService(event.context(), service)).ifPresent(nano -> event.acknowledge()));
    }

    /**
     * Retrieves a {@link Service} of a specified type.
     *
     * @param serviceClass The class of the {@link Service} to retrieve.
     * @return The first instance of the specified {@link Service}, or null if not found.
     */
    public Service service(final Class<? extends Service> serviceClass) {
        final List<Service> results = services(serviceClass);
        if (results != null && !results.isEmpty()) {
            return results.getFirst();
        }
        return null;
    }

    /**
     * Retrieves a list of {@link Service} of a specified type.
     *
     * @param serviceClass The class of the {@link Service} to retrieve.
     * @return A list of {@link Service} of the specified type.
     */
    public List<Service> services(final Class<? extends Service> serviceClass) {
        if (serviceClass != null) {
            return services.stream().filter(rc -> serviceClass.isAssignableFrom(rc.getClass())).toList();
        }
        return Collections.emptyList();
    }

    /**
     * Provides an unmodifiable list of all registered {@link Service}.
     *
     * @return An unmodifiable list of {@link Service} instances.
     */
    public List<Service> services() {
        return unmodifiableList(services);
    }

    /**
     * Shuts down all registered {@link Service} gracefully.
     *
     * @param context The {@link Context} in which the services are shut down.
     */
    protected void shutdownServices(final Context context) {
        if (rootContext.gett(CONFIG_PARALLEL_SHUTDOWN.id(), Boolean.class).orElse(false)) {
            try {
                CompletableFuture.allOf(services.stream().map(service -> execute(() -> unregisterService(context, service))).toArray(CompletableFuture[]::new)).join();
            } catch (final Exception err) {
                logger.fatal(err, () -> "[{}] shutdown error", Service.class.getSimpleName());
                Thread.currentThread().interrupt();
            }
        } else {
            final List<Service> servicesList = new ArrayList<>(services);
            Collections.reverse(servicesList);
            servicesList.forEach(service -> unregisterService(context, service));
        }
    }

    /**
     * Registers a new service in the {@link Nano} framework.
     *
     * @param service The {@link Service} to register.
     * @return Self for chaining
     */
    @SuppressWarnings("unchecked")
    protected T registerService(final Service service) {
        if (service != null) {
            services.add(service);
        }
        return (T) this;
    }

    /**
     * Unregisters a {@link Service} from the {@link Nano} framework and stops it.
     *
     * @param context The {@link Context} in which the {@link Service} is unregistered and stopped.
     * @param service The {@link Service} to unregister and stop.
     * @return Self for chaining
     */
    @SuppressWarnings("unchecked")
    protected T unregisterService(final Context context, final Service service) {
        if (service != null) {
            services.remove(service);
            try {
                service.stop(() -> context);
            } catch (final Exception e) {
                logger.warn(e, () -> "Stop [{}] error", service.name());
            }
        }
        return (T) this;
    }
}
