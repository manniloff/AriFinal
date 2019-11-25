package com.asterisk.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

@ApplicationScoped
public class UtilsFactory {

    @Produces
    public Logger initiateLogger(InjectionPoint point) {
        return LoggerFactory.getLogger(point.getMember().getDeclaringClass().getName());
    }

    @Produces
    //@ObjectMapperQualifier
    public ObjectMapper initiateObjectMapper(InjectionPoint point) {
        return new ObjectMapper();
    }
}


