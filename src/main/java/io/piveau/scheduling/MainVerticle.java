package io.piveau.scheduling;

import io.piveau.scheduling.launcher.LauncherServiceVerticle;
import io.piveau.scheduling.quartz.QuartzService;
import io.piveau.scheduling.quartz.QuartzServiceVerticle;
import io.piveau.scheduling.shell.ShellVerticle;
import io.piveau.utils.ConfigurableAssetHandler;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.serviceproxy.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.piveau.scheduling.ApplicationConfig.*;

public class MainVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private QuartzService quartzService;
    private JsonObject config;

    @Override
    public void start(Promise<Void> startPromise) {
        ConfigStoreOptions envStoreOptions = new ConfigStoreOptions()
                .setType("env")
                .setConfig(new JsonObject().put("keys", new JsonArray()
                        .add(ENV_APPLICATION_PORT)
                        .add(ENV_PIVEAU_CLUSTER_CONFIG)
                        .add(ENV_PIVEAU_SHELL_CONFIG)
                        .add(ENV_PIVEAU_FAVICON_PATH)
                        .add(ENV_PIVEAU_LOGO_PATH)));

        ConfigStoreOptions fileStoreOptions = new ConfigStoreOptions()
                .setType("directory")
                .setConfig(new JsonObject()
                        .put("path", "config")
                        .put("filesets", new JsonArray()
                                .add(new JsonObject()
                                        .put("format", "json")
                                        .put("pattern", "*.json"))
                                .add(new JsonObject()
                                        .put("format", "yaml")
                                        .put("pattern", "*.yaml"))
                                .add(new JsonObject()
                                        .put("format", "yaml")
                                        .put("pattern", "*.yml"))
                                .add(new JsonObject()
                                        .put("format", "properties")
                                        .put("pattern", "*.properties"))));

        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
                .addStore(envStoreOptions)
                .addStore(fileStoreOptions));

        AtomicReference<String> logoPath = new AtomicReference<>();
        AtomicReference<String> faviconPath = new AtomicReference<>();

        retriever.getConfig()
                .compose(configuration -> {
                    config = configuration;
                    log.debug("Config: {}", config.encodePrettily());

                    faviconPath.set(config.getString(ENV_PIVEAU_FAVICON_PATH, DEFAULT_PIVEAU_FAVICON_PATH));
                    logoPath.set(config.getString(ENV_PIVEAU_LOGO_PATH, DEFAULT_PIVEAU_LOGO_PATH));

                    Promise<JsonObject> launcherPromise = Promise.promise();
                    vertx.deployVerticle(LauncherServiceVerticle.class, new DeploymentOptions().setWorker(true).setConfig(config))
                            .onSuccess(id -> launcherPromise.complete(config))
                            .onFailure(launcherPromise::fail);

                    return launcherPromise.future();
                })
                .compose(config -> {
                    Future<String> quartzFuture = vertx.deployVerticle(QuartzServiceVerticle.class, new DeploymentOptions().setWorker(true).setConfig(config));
                    Future<String> shellFuture = vertx.deployVerticle(ShellVerticle.class, new DeploymentOptions().setWorker(true).setConfig(config));

                    return CompositeFuture.all(quartzFuture, shellFuture);
                })
                .compose(v -> {
                    quartzService = QuartzService.createProxy(vertx, QuartzService.SERVICE_ADDRESS);

                    return RouterBuilder.create(vertx, "webroot/openapi.yaml");
                })
                .onSuccess(builder -> {
                    builder.operation("listTriggers").handler(this::handleListTriggers);
                    builder.operation("getTriggers").handler(this::handleGetTriggers);
                    builder.operation("createOrUpdateTriggers").handler(this::handleCreateOrUpdateTriggers);
                    builder.operation("deleteTriggers").handler(this::handleDeleteTriggers);
                    builder.operation("setTriggerStatus").handler(this::handleSetTriggerStatus);
                    builder.operation("bulkUpdate").handler(this::handleBulkUpdate);

                    builder.rootHandler(CorsHandler.create("*").allowedHeader("Content-Type").allowedMethods(Stream.of(HttpMethod.PUT, HttpMethod.GET).collect(Collectors.toSet())));

                    Router router = builder.createRouter();

                    WebClient client = WebClient.create(vertx);
                    router.route("/images/logo").handler(new ConfigurableAssetHandler(logoPath.get(), client));
                    router.route("/images/favicon").handler(new ConfigurableAssetHandler(faviconPath.get(), client));

                    router.route("/*").handler(StaticHandler.create());

                    HealthCheckHandler hch = HealthCheckHandler.create(vertx);
                    hch.register("buildInfo", future -> vertx.fileSystem().readFile("buildInfo.json", bi -> {
                        if (bi.succeeded()) {
                            future.complete(Status.OK(bi.result().toJsonObject()));
                        } else {
                            future.fail(bi.cause());
                        }
                    }));
                    router.get("/health").handler(hch);

                    vertx.createHttpServer()
                            .requestHandler(router)
                            .listen(config.getInteger(ENV_APPLICATION_PORT, DEFAULT_APPLICATION_PORT))
                            .onSuccess(success -> startPromise.complete())
                            .onFailure(startPromise::fail);
                })
                .onFailure(startPromise::fail);
    }

    private void handleListTriggers(RoutingContext routingContext) {
        quartzService.listTriggers()
                .onSuccess(result -> routingContext.response().end(result.encodePrettily()))
                .onFailure(cause -> {
                    if (cause instanceof ServiceException) {
                        ServiceException se = (ServiceException) cause;
                        routingContext.response().setStatusCode(se.failureCode()).end(se.getMessage());
                    } else {
                        routingContext.response().setStatusCode(500).end(cause.getMessage());
                    }
                });
    }

    private void handleBulkUpdate(RoutingContext routingContext) {
        JsonObject bulk = routingContext.getBodyAsJson();

        ArrayList<Future<String>> futureList = new ArrayList<>();

        bulk.fieldNames().forEach(name -> {
            JsonArray triggers = bulk.getJsonArray(name);
            futureList.add(quartzService.createOrUpdateTrigger(name, triggers));
        });
        CompositeFuture.join(new ArrayList<>(futureList))
                .onSuccess(v -> routingContext.response().end())
                .onFailure(cause -> routingContext.response().setStatusCode(200).setStatusMessage("Not all triggers were successfully created or updated.").end());
    }

    private void handleGetTriggers(RoutingContext routingContext) {
        String pipeId = routingContext.pathParam("pipeId");
        quartzService.getTriggers(pipeId)
                .onSuccess(result -> routingContext.response().end(result.encodePrettily()))
                .onFailure(cause -> {
                    if (cause instanceof ServiceException) {
                        ServiceException se = (ServiceException) cause;
                        routingContext.response().setStatusCode(se.failureCode()).end(se.getMessage());
                    } else {
                        routingContext.response().setStatusCode(500).end(cause.getMessage());
                    }
                });
    }

    private void handleCreateOrUpdateTriggers(RoutingContext routingContext) {
        String pipeId = routingContext.pathParam("pipeId");
        JsonArray triggers = routingContext.getBodyAsJsonArray();
        quartzService.createOrUpdateTrigger(pipeId, triggers)
                .onSuccess(statusCode -> {
                    int code = "created".equalsIgnoreCase(statusCode) ? 201 : 200;
                    routingContext.response().setStatusCode(code).end();
                })
                .onFailure(cause -> {
                    if (cause instanceof ServiceException) {
                        ServiceException se = (ServiceException) cause;
                        routingContext.response().setStatusCode(se.failureCode()).end(se.getMessage());
                    } else {
                        routingContext.response().setStatusCode(500).end(cause.getMessage());
                    }
                });
    }

    private void handleDeleteTriggers(RoutingContext routingContext) {
        String pipeId = routingContext.pathParam("pipeId");
        quartzService.deleteTriggers(pipeId)
                .onSuccess(v -> routingContext.response().end())
                .onFailure(cause -> {
                    if (cause instanceof ServiceException) {
                        ServiceException se = (ServiceException) cause;
                        routingContext.response().setStatusCode(se.failureCode()).end(se.getMessage());
                    } else {
                        routingContext.response().setStatusCode(500).end(cause.getMessage());
                    }
                });
    }

    private void handleSetTriggerStatus(RoutingContext routingContext) {
        String pipeId = routingContext.pathParam("pipeId");
        String triggerId = routingContext.pathParam("triggerId");
        String status = routingContext.pathParam("status");

        quartzService.setTriggerStatus(pipeId, triggerId, status)
                .onSuccess(result -> routingContext.response().end(result))
                .onFailure(cause -> {
                    String message = cause.getMessage();
                    if (message.contains("not found")) {
                        routingContext.response().setStatusCode(404).end(message);
                    } else if (message.equals("status already set or unknown")) {
                        routingContext.response().setStatusCode(409).end(message);
                    } else {
                        routingContext.response().setStatusCode(500).end(message);
                    }
                });
    }

    public static void main(String[] args) {
        String[] params = Arrays.copyOf(args, args.length + 1);
        params[params.length - 1] = MainVerticle.class.getName();
        Launcher.executeCommand("run", params);
    }

}
