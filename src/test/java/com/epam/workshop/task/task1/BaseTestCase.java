package com.epam.workshop.task.task1;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class BaseTestCase {
    public final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulerConfiguration.class);

    @Test
    public void shouldStartJobExecution() {
        runner
                .withUserConfiguration(getUserConfigurations())
                .run(ctx -> {
                    Assertions.assertThat(ctx)
                            .hasSingleBean(SimpleTriggerFactoryBean.class)
                            .hasBean("trigger")
                            .hasSingleBean(JobDetailFactoryBean.class)
                            .hasBean("jobDetail");

                    final CountDownLatch latch = ctx.getBean(CountDownLatch.class);

                    Assertions.assertThat(latch.await(5L, TimeUnit.SECONDS))
                            .describedAs("Job should be executed at least 2 times!")
                            .isTrue();
                });
    }

    protected abstract Class<?>[] getUserConfigurations();

    @DisallowConcurrentExecution
    public static abstract class BaseJobClass extends QuartzJobBean {
        @Autowired
        private CountDownLatch latch;

        @Override
        protected final void executeInternal(JobExecutionContext context) {
            System.out.println("Hello!");
            latch.countDown();
        }
    }

    @TestConfiguration
    public static class SchedulerConfiguration {

        @Bean
        public CountDownLatch latch() {
            return new CountDownLatch(2);
        }

        @Bean
        public SchedulerFactoryBean scheduler(JobFactory jobFactory, ObjectProvider<Trigger[]> triggers) {
            SchedulerFactoryBean bean = new SchedulerFactoryBean();
            bean.setSchedulerName("name");
            bean.setWaitForJobsToCompleteOnShutdown(true);
            bean.setOverwriteExistingJobs(true);
            bean.setJobFactory(jobFactory);
            bean.setAutoStartup(true);
            triggers.ifAvailable(bean::setTriggers);

            Properties props = new Properties();
            props.put("org.quartz.scheduler.instanceId", "AUTO");
            props.put("org.quartz.threadPool.threadCount", "2");
            props.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

            bean.setQuartzProperties(props);

            return bean;
        }

        @Bean
        public JobFactory beanFactoryJobFactory(ApplicationContext ctx) {
            final SpringBeanJobFactory springBeanJobFactory = new SpringBeanJobFactory();
            springBeanJobFactory.setApplicationContext(ctx);
            return springBeanJobFactory;
        }
    }

}