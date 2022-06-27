package io.piveau.scheduling.shell;

import io.piveau.scheduling.launcher.LauncherService;
import io.vertx.core.Vertx;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.Option;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandBuilder;

import java.util.Comparator;

public class PipesCommand {
    private final Command command;

    private final LauncherService launcherService;

    private PipesCommand(Vertx vertx) {
        launcherService = LauncherService.createProxy(vertx, LauncherService.SERVICE_ADDRESS);
        command = CommandBuilder.command(
                CLI.create("pipes").setDescription("List all pipes")
                        .addOption(new Option().setHelp(true).setFlag(true).setArgName("help").setShortName("h").setLongName("help"))
        ).processHandler(process -> {
            launcherService.availablePipes()
                    .onSuccess(list -> {
                        list.stream()
                                .map(pipe -> pipe.getJsonObject("header").getString("name"))
                                .sorted()
                                .forEach(pipeName -> process.write(pipeName + "\n"));
                        process.end();
                    })
                    .onFailure(cause -> process.write("Pipe list not available. " + cause.getMessage() + "\n").end());
        }).build(vertx);
    }

    public static Command create(Vertx vertx) {
        return new PipesCommand(vertx).command;
    }

}
