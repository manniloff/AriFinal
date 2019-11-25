package com.asterisk.controller;

import com.asterisk.service.AriSettingsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/arisettings")
public class AriSettingsController {

    private AriSettingsService ariSettingsService;
    private ObjectMapper objectMapper;

    @Inject
    public AriSettingsController(AriSettingsService ariSettingsService,ObjectMapper objectMapper){
        this.ariSettingsService = ariSettingsService;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getAll() throws JsonProcessingException {
        return objectMapper.writeValueAsString(ariSettingsService.getAll().list());
    }
}
