package io.piveau.scheduling.shell;

import io.piveau.scheduling.launcher.LauncherService;
import io.piveau.utils.Piveau;
import io.vertx.core.Vertx;
import io.vertx.core.cli.Argument;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.Option;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class ShowCommand {
    private final Command command;

    private final LauncherService launcherService;

    private ShowCommand(Vertx vertx) {
        launcherService = LauncherService.createProxy(vertx, LauncherService.SERVICE_ADDRESS);
        command = CommandBuilder.command(
                CLI.create("show")
                        .setDescription("Display a pipe")
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
                    .onFailure(cause ->completion.complete("", true))
        ).processHandler(process -> {
            String pipeName = process.commandLine().getArgumentValue(0);
            if (pipeName != null) {
                launcherService.getPipe(pipeName)
                        .onSuccess(pipe -> process.write("\n" + pipe.encodePrettily() + "\n").end())
                        .onFailure(cause -> process.write(cause.getMessage() + "\n"));
            } else {
                process.write("Please, name a pipe.").end();
            }
        }).build(vertx);
    }

    public static Command create(Vertx vertx) {
        return new ShowCommand(vertx).command;
    }

}
