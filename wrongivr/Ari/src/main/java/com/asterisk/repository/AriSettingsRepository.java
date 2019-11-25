package com.asterisk.repository;

import com.asterisk.model.AriSettings;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AriSettingsRepository implements PanacheRepository<AriSettings> {
}
