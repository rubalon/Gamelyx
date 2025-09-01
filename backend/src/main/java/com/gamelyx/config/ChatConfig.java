package com.gamelyx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.chat")
public class ChatConfig {

    private Encryption encryption = new Encryption();

    public Encryption getEncryption() {
        return encryption;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public static class Encryption {
        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            if (key == null || key.length() < 32) {
                throw new IllegalArgumentException("La clave de cifrado debe tener al menos 32 caracteres para AES-256");
            }
            this.key = key;
        }
    }
}