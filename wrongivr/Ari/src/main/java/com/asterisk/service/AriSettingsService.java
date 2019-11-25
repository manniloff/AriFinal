package com.asterisk.service;

import com.asterisk.model.AriSettings;
import com.asterisk.repository.AriSettingsRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

@ApplicationScoped
public class AriSettingsService {
    @Inject
    AriSettingsRepository ariSettingsRepository;

    public PanacheQuery<AriSettings> getAll() {
        return ariSettingsRepository.findAll();
    }

    public AriSettings getById(long id) {
        return ariSettingsRepository.findById(id);
    }

    @Transactional
    public void createOrUpdate(AriSettings newAriSettings) {
        if (ariSettingsRepository.isPersistent(newAriSettings)) {
            AriSettings ari = ariSettingsRepository.findById(newAriSettings.getId());
            ari.setAriUrl(newAriSettings.getAriUrl());
            ari.setUser(newAriSettings.getUser());
            ari.setPassword(newAriSettings.getPassword());
            ari.setApplicationName(newAriSettings.getApplicationName());
            ari.setAriVersion(newAriSettings.getAriVersion());

            ariSettingsRepository.persist(ari);
        } else {
            AriSettings ari = new AriSettings();

            ari.setAriUrl(newAriSettings.getAriUrl());
            ari.setUser(newAriSettings.getUser());
            ari.setPassword(newAriSettings.getPassword());
            ari.setApplicationName(newAriSettings.getApplicationName());
            ari.setAriVersion(newAriSettings.getAriVersion());

            ariSettingsRepository.persist(ari);
        }
    }

    public AriSettings deleteById(long id) {
        AriSettings user = ariSettingsRepository.findById(id);
        if (user != null) {
            ariSettingsRepository.delete(user);
        }
        return user;
    }
}
