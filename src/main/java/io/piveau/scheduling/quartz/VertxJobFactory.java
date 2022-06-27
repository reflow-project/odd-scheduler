package io.piveau.scheduling.quartz;

import io.piveau.scheduling.launcher.LauncherService;
import io.vertx.core.Vertx;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.simpl.SimpleJobFactory;
import org.quartz.spi.TriggerFiredBundle;

public class VertxJobFactory extends SimpleJobFactory {

    private final LauncherService launcherService;

    static VertxJobFactory create(Vertx vertx) {
        LauncherService launcherService = LauncherService.createProxy(vertx, LauncherService.SERVICE_ADDRESS);
        return new VertxJobFactory(launcherService);
    }

    private VertxJobFactory(LauncherService launcherService) {
        this.launcherService = launcherService;
    }

    @Override
    public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {
        final JobDetail jobDetail = triggerFiredBundle.getJobDetail();
        String pipeName = jobDetail.getKey().getName();
        final Class<? extends Job> jobClass = jobDetail.getJobClass();
        try {
            return jobClass.getConstructor(LauncherService.class, String.class).newInstance(launcherService, pipeName);
        } catch (Exception e) {
            throw new SchedulerException("Could not create a job of type " + jobClass);
        }
    }

}
