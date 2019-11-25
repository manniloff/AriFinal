package com.asterisk.model;

import ch.loway.oss.ari4java.AriVersion;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Table(name = "settings_ari")
public class AriSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "ari_url", length = 50)
    private String ariUrl;

    @Column(name = "user", length = 30)
    private String user;

    @Column(name = "password", length = 30)
    private String password;

    @Column(name = "application_name", unique = true, length = 30)
    private String applicationName;

    @Column(name = "ari_version", length = 15)
    @Enumerated(EnumType.STRING)
    private AriVersion ariVersion;

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (this == obj) {
            result = true;
        }
        if (this.id == ((AriSettings)obj).id) {
            result = true;
        }
        return result;
    }

    public AriSettings(String ariUrl, String user, String password, String applicationName, AriVersion ariVersion) {
        this.ariUrl = ariUrl;
        this.user = user;
        this.password = password;
        this.applicationName = applicationName;
        this.ariVersion = ariVersion;
    }
}

