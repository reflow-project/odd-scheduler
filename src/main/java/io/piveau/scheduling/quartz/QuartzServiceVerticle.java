package io.piveau.scheduling.quartz;

import io.piveau.scheduling.launcher.LauncherService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class QuartzServiceVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void start(Promise<Void> startPromise) {

        initH2();

        LauncherService launcherService = LauncherService.createProxy(vertx, LauncherService.SERVICE_ADDRESS);

        QuartzService.create(VertxJobFactory.create(vertx), launcherService, ready -> {
            if (ready.succeeded()) {
                new ServiceBinder(vertx).setAddress(QuartzService.SERVICE_ADDRESS).register(QuartzService.class, ready.result());
                startPromise.complete();
            } else {
                startPromise.fail(ready.cause());
            }
        });
    }

    private void initH2() {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:file:./db/quartzdb", "sa", "")) {
            ResultSet rset = connection.getMetaData().getTables(null, null, "QRTZ_TRIGGERS", null);
            if (!rset.next()) {
                try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("tables_h2.sql")) {
                    if (inputStream != null) {
                        RunScript.execute(connection, new InputStreamReader(inputStream));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Init H2 db", e);
        }
    }

}
