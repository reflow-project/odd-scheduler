package io.piveau.scheduling.launcher;

import io.piveau.pipe.PiveauCluster;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;

@ProxyGen
public interface LauncherService {
    String SERVICE_ADDRESS = "io.piveau.scheduling.launcher.service";

    static LauncherService create(Vertx vertx, PiveauCluster cluster, Handler<AsyncResult<LauncherService>> readyHandler) {
        return new LauncherServiceImpl(vertx, cluster, readyHandler);
    }

    static LauncherService createProxy(Vertx vertx, String address) {
        return new LauncherServiceVertxEBProxy(vertx, address);
    }

    Future<String> launch(String pipeName, JsonObject configs);

    Future<Boolean> isPipeAvailable(String pipeName);

    Future<JsonObject> getPipe(String pipeName);

    Future<List<JsonObject>> availablePipes();

}
