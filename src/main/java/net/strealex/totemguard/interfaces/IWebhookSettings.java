package net.strealex.totemguard.interfaces;

public interface IWebhookSettings {

    boolean isEnabled();

    String getUrl();

    String getName();

    String getColor();

    String getTitle();

    String getProfileImage();

    boolean isTimestamp();
}
