package com.deathmotion.totemguard.util.webhook;

import lombok.Getter;
import lombok.Setter;

import java.net.URI;

@Getter
@Setter
public class WebhookConfig {
    URI uri;
    String username;
    String avatarUrl;
    String title;
    int color;
    boolean timestamp;
    boolean footer;
    boolean valid = false;
}
