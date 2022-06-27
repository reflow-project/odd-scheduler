package io.piveau.scheduling.quartz;

import io.piveau.scheduling.launcher.LauncherService;
import io.vertx.core.json.JsonObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisallowConcurrentExecution
public class PipeJob implements Job {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final LauncherService launcherService;
    private final String pipeName;

    public PipeJob(LauncherService launcherService, String pipeName) {
        this.launcherService = launcherService;
        this.pipeName = pipeName;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {

        String triggerObject = jobExecutionContext.getMergedJobDataMap().getString("triggerObject");
        log.debug("Job triggered: {}", triggerObject);

        JsonObject trigger = new JsonObject(triggerObject);

        JsonObject configs = trigger.getJsonObject("configs", new JsonObject());

        launcherService.launch(pipeName, configs)
                .onSuccess(runId -> log.debug("Pipe {} started successfully ({})!", pipeName, runId))
                .onFailure(cause -> log.error("Starting pipe " + pipeName + " failed!", cause));
    }

}
