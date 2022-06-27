package io.piveau.scheduling.launcher;

import io.piveau.json.ConfigHelper;
import io.piveau.pipe.PiveauCluster;
import io.piveau.scheduling.ApplicationConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LauncherServiceVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void start(Promise<Void> startPromise) {
        log.debug("Start Launcher Service Verticle");

        JsonObject clusterConfig = ConfigHelper.forConfig(config()).forceJsonObject(ApplicationConfig.ENV_PIVEAU_CLUSTER_CONFIG);
        PiveauCluster.create(vertx, clusterConfig)
                .onSuccess(cluster ->
                        LauncherService.create(vertx, cluster, ready -> {
                            if (ready.succeeded()) {
                                new ServiceBinder(vertx).setAddress(LauncherService.SERVICE_ADDRESS).register(LauncherService.class, ready.result());
                                startPromise.complete();
                            } else {
                                startPromise.fail(ready.cause());
                            }
                        }))
                .onFailure(startPromise::fail);
    }

}
