package io.piveau.scheduling.shell;

import io.piveau.json.ConfigHelper;
import io.piveau.scheduling.ApplicationConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandRegistry;
import io.vertx.ext.shell.term.HttpTermOptions;
import io.vertx.ext.shell.term.TelnetTermOptions;

import java.util.ArrayList;
import java.util.List;

public class ShellVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> promise) {

        ShellServiceOptions shellServiceOptions = new ShellServiceOptions().setWelcomeMessage("\n piveau scheduling shell\n\n");

        JsonObject clientConfig = ConfigHelper.forConfig(config()).forceJsonObject(ApplicationConfig.ENV_PIVEAU_SHELL_CONFIG);
        clientConfig.forEach(entry -> {
            JsonObject options = (JsonObject) entry.getValue();
            switch (entry.getKey()) {
                case "telnet":
                    shellServiceOptions.setTelnetOptions(new TelnetTermOptions()
                            .setHost(options.getString("host", "0.0.0.0"))
                            .setPort(options.getInteger("port", 5000)));
                    break;
                case "http":
                    shellServiceOptions.setHttpOptions(new HttpTermOptions()
                            .setHost(options.getString("host", "0.0.0.0"))
                            .setPort(options.getInteger("port", 8085)));
                    break;
            }
        });

        ShellService service = ShellService.create(vertx, shellServiceOptions);
        service.start()
                .compose(v -> {
                    List<Command> commands = new ArrayList<>();
                    commands.add(PipesCommand.create(vertx));
                    commands.add(LaunchCommand.create(vertx));
                    commands.add(ShowCommand.create(vertx));
                    commands.add(TriggerCommand.create(vertx));
                    return CommandRegistry.getShared(vertx).registerCommands(commands);
                })
                .onSuccess(list -> promise.complete())
                .onFailure(promise::fail);
    }

}
