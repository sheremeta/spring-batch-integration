package com.example.integration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.launch.JobLaunchingMessageHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;

import java.io.File;

import static org.springframework.integration.dsl.Pollers.fixedRate;
import static org.springframework.integration.file.FileReadingMessageSource.WatchEventType.CREATE;

@Configuration
public class IntegrationConfig {

    private final JobLauncher jobLauncher;
    private final Job sampleJob;

    public IntegrationConfig(JobLauncher jobLauncher, Job sampleJob) {
        this.jobLauncher = jobLauncher;
        this.sampleJob = sampleJob;
    }

    @Bean
    public IntegrationFlow sampleFlow() {
        return IntegrationFlows.from(
                fileSource(), c -> c.poller(fixedRate(1000).maxMessagesPerPoll(1)))
                .transform(fileMessageToJobRequest())
                .handle(jobLaunchingMessageHandler())
                .log(LoggingHandler.Level.INFO, Message::getPayload)
                .get();
    }

    @Bean
    public MessageSource<File> fileSource() {
        FileReadingMessageSource source = new FileReadingMessageSource();
        source.setDirectory(new File("/tmp/files"));
        source.setFilter(new SimplePatternFileListFilter("*.txt"));
        source.setUseWatchService(true);
        source.setWatchEvents(CREATE);
        return source;
    }

    @Bean
    FileMessageToJobRequest fileMessageToJobRequest() {
        return new FileMessageToJobRequest(sampleJob);
    }

    @Bean
    JobLaunchingMessageHandler jobLaunchingMessageHandler() {
        return new JobLaunchingMessageHandler(jobLauncher);
    }
}