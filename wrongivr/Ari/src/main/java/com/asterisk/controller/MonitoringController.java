package com.asterisk.controller;

import ch.loway.oss.ari4java.tools.RestException;
import com.asterisk.ari.AriManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/rest/monitoring")
public class MonitoringController {

    private AriManager ariManager;
    private ObjectMapper objectMapper;
    private Logger logger;

    @Inject
    public MonitoringController(AriManager ariManager, ObjectMapper objectMapper, Logger logger) {
        this.ariManager = ariManager;
        this.objectMapper = objectMapper;
        this.logger = logger;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/endpoints")
    public String getAllEndPoints() throws JsonProcessingException {
        String response = "";
        try {
            response = objectMapper.writeValueAsString(ariManager.getAri().endpoints().list());
        } catch (RestException e) {
            e.printStackTrace();
            logger.error(String.valueOf(e.getMessage()));
        }

        return response;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/channels")
    public String getAllChannels() throws JsonProcessingException {
        String response = "";
        String dummy = "";
        try {
            for (int i = 0; i <= 3; i++) {
                ariManager.getAri().channels().create("SIP/6001"
                        , "Unifun-ARI", dummy, dummy, dummy, dummy, dummy);
            }
            objectMapper.writeValueAsString(ariManager.getAri().channels().list());
        } catch (RestException e) {
            logger.error(String.valueOf(e.getMessage()));
        }

        return response;
    }
}
