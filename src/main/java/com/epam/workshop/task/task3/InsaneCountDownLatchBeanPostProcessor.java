package com.epam.workshop.task.task3;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.concurrent.CountDownLatch;

public class InsaneCountDownLatchBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof CountDownLatch) {
            return new CountDownLatch((int) (((CountDownLatch) bean).getCount() + 1));
        }
        return bean;
    }
}
