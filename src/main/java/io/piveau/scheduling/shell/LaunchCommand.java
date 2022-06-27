package io.piveau.scheduling.shell;

import io.piveau.scheduling.launcher.LauncherService;
import io.piveau.utils.Piveau;
import io.vertx.core.Vertx;
import io.vertx.core.cli.Argument;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.Option;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class LaunchCommand {
    private final Command command;

    private final LauncherService launcherService;

    private LaunchCommand(Vertx vertx) {
        launcherService = LauncherService.createProxy(vertx, LauncherService.SERVICE_ADDRESS);
        command = CommandBuilder.command(
                CLI.create("launch")
                        .setDescription("Launch a pipe")
                        .addArgument(
                                new Argument().setIndex(0)
                                        .setArgName("pipeName")
                                        .setRequired(true)
                                        .setDescription("Name of the pipe"))
                        .addOption(new Option().setHelp(true).setFlag(true).setArgName("help").setShortName("h").setLongName("help"))
        ).completionHandler(completion ->
            launcherService.availablePipes()
                .onSuccess(list -> {
                    List<String> candidates = list.stream().map(p -> p.getJsonObject("header").getString("name"))
                            .collect(Collectors.toList());
                    Piveau.candidatesCompletion(completion, candidates);
                })
                .onFailure(cause -> completion.complete("", true))
        ).processHandler(process -> {
            String pipe = process.commandLine().getArgumentValue(0);
            if (pipe != null) {
                launcherService.launch(pipe, new JsonObject())
                        .onSuccess(runId -> process.write("Pipe " + pipe + " successfully launched. Run id is " + runId + "\n").end())
                        .onFailure(cause -> process.write("Pipe launch failed: " + cause.getMessage()).end());
            } else {
                process.write("Please, name a pipe.").end();
            }
        }).build(vertx);
    }

    public static Command create(Vertx vertx) {
        return new LaunchCommand(vertx).command;
    }

}
