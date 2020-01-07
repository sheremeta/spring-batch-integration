package com.example.integration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static javax.batch.runtime.BatchStatus.COMPLETED;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    private static final String JOB_NAME = "Job";
    private static final String STEP_READ = "Read";
    private static final String STEP_END = "Complete";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public BatchConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    Job job() {
        return jobBuilderFactory.get(JOB_NAME)
                .incrementer(new RunIdIncrementer())
                .start(read())
                .on(COMPLETED.name()).to(complete())
                .end()
                .build();
    }

    @Bean
    Step read() {
        return stepBuilderFactory.get(STEP_READ)
                .<String, String>chunk(1)
                .reader(itemReader(null))
                .writer(i -> i.forEach(System.out::println))
                .build();
    }

    @Bean
    Step complete() {
        return stepBuilderFactory.get(STEP_END)
                .tasklet(completionMarker(null))
                .build();
    }

    @Bean
    @StepScope
    FlatFileItemReader<String> itemReader(@Value("#{jobParameters['input.file.name']}") String filePath) {
        FlatFileItemReader<String> reader = new FlatFileItemReader<>();

        FileSystemResource fileResource = new FileSystemResource(filePath);
        reader.setResource(fileResource);
        reader.setLineMapper(new PassThroughLineMapper());

        return reader;
    }

    @Bean
    @StepScope
    Tasklet completionMarker(@Value("#{jobParameters['input.file.name']}") String filePath) {
        return (stepContribution, chunkContext) -> {
            try {
                Files.move(Path.of(filePath), Path.of(filePath.replaceFirst(".txt", ".done")));
            } catch (IOException e) {
                return RepeatStatus.CONTINUABLE;
            }

            return RepeatStatus.FINISHED;
        };
    }

}
