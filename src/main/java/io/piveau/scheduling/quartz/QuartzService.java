package io.piveau.scheduling.quartz;

import io.piveau.scheduling.launcher.LauncherService;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.quartz.spi.JobFactory;

@ProxyGen
public interface QuartzService {
    String SERVICE_ADDRESS = "io.piveau.scheduling.quartz.service";

    static QuartzService create(JobFactory jobFactory, LauncherService launcherService, Handler<AsyncResult<QuartzService>> readyHandler) {
        return new QuartzServiceImpl(jobFactory, launcherService, readyHandler);
    }

    static QuartzService createProxy(Vertx vertx, String address) {
        return new QuartzServiceVertxEBProxy(vertx, address);
    }

    Future<JsonObject> listTriggers();

    Future<JsonArray> getTriggers(String pipeId);

    Future<String> createOrUpdateTrigger(String pipeId, JsonArray triggerArray);

    Future<Void> deleteTriggers(String pipeId);

    Future<String> setTriggerStatus(String pipeId, String triggerId, String status);

}
