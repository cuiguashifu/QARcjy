package com.qar.securitysystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.person")
public class PersonSeedProperties {
    private boolean seedEnabled = true;
    private String seedCsv = "classpath:person_seed.csv";

    public boolean isSeedEnabled() {
        return seedEnabled;
    }

    public void setSeedEnabled(boolean seedEnabled) {
        this.seedEnabled = seedEnabled;
    }

    public String getSeedCsv() {
        return seedCsv;
    }

    public void setSeedCsv(String seedCsv) {
        this.seedCsv = seedCsv;
    }
}

