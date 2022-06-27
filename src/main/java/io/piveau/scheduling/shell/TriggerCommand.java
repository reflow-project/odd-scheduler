package io.piveau.scheduling.shell;

import io.piveau.scheduling.launcher.LauncherService;
import io.piveau.scheduling.quartz.QuartzService;
import io.piveau.utils.Piveau;
import io.vertx.core.Vertx;
import io.vertx.core.cli.Argument;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class TriggerCommand {

    private final Command command;

    private final QuartzService quartzService;

    private final LauncherService launcherService;

    private TriggerCommand(Vertx vertx) {
        launcherService = LauncherService.createProxy(vertx, LauncherService.SERVICE_ADDRESS);
        quartzService = QuartzService.createProxy(vertx, QuartzService.SERVICE_ADDRESS);
        command = CommandBuilder.command(
                CLI.create("trigger")
                        .addArgument(
                                new Argument()
                                        .setArgName("pipeName")
                                        .setRequired(false)
                                        .setDescription("The name of the pipe."))
                        .addOption(new Option().setHelp(true).setFlag(true).setArgName("help").setShortName("h").setLongName("help"))
                        .addOption(new Option().setFlag(true).setArgName("verbose").setShortName("v").setLongName("verbose"))
        ).completionHandler(completion ->
            launcherService.availablePipes()
                    .onSuccess(list -> {
                        List<String> candidates = list.stream().map(p -> p.getJsonObject("header").getString("name"))
                                .collect(Collectors.toList());
                        Piveau.candidatesCompletion(completion, candidates);
                    })
                    .onFailure(cause -> completion.complete("", true))
        ).processHandler(process -> {
            CommandLine commandLine = process.commandLine();
            if (commandLine.allArguments().isEmpty()) {
                quartzService.listTriggers()
                        .onSuccess(triggers -> process.write("\n" + triggers.encodePrettily() + "\n").end())
                        .onFailure(cause -> process.write(cause.getMessage() + "\n").end());
            } else {
                String pipeName = commandLine.getArgumentValue(0);
                quartzService.getTriggers(pipeName)
                        .onSuccess(triggers -> process.write("\n" + triggers.encodePrettily() + "\n").end())
                        .onFailure(cause -> process.write(cause.getMessage() + "\n").end());
            }
        }).build(vertx);
    }

    public static Command create(Vertx vertx) {
        return new TriggerCommand(vertx).command;
    }

}
