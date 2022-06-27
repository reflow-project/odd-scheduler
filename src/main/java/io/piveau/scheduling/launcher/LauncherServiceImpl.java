package io.piveau.scheduling.launcher;

import io.piveau.pipe.PipeLauncher;
import io.piveau.pipe.PiveauCluster;
import io.piveau.pipe.model.ModelKt;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class LauncherServiceImpl implements LauncherService {

    private final PipeLauncher launcher;

    LauncherServiceImpl(Vertx vertx, PiveauCluster cluster, Handler<AsyncResult<LauncherService>> readyHandler) {
        launcher = cluster.pipeLauncher(vertx);
        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public Future<String> launch(String pipeName, JsonObject configs) {
        if (launcher.isPipeAvailable(pipeName)) {
            return launcher.runPipe(pipeName, configs, null);
        } else {
            return Future.failedFuture("Pipe not available.");
        }
    }

    @Override
    public Future<Boolean> isPipeAvailable(String pipeName) {
        return Future.succeededFuture(launcher.isPipeAvailable(pipeName));
    }

    @Override
    public Future<JsonObject> getPipe(String pipeName) {
        if (launcher.isPipeAvailable(pipeName)) {
            return Future.succeededFuture(new JsonObject(ModelKt.prettyPrint(launcher.getPipe(pipeName))));
        } else {
            return Future.failedFuture("Pipe not available.");
        }
    }

    @Override
    public Future<List<JsonObject>> availablePipes() {
        List<JsonObject> list = launcher.availablePipes().stream().map(p -> new JsonObject(ModelKt.prettyPrint(p))).collect(Collectors.toList());
        return Future.succeededFuture(list);
    }

}
