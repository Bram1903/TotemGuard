package net.strealex.totemguard.interfaces;

public interface ICheckSettings {
    boolean isEnabled();

    boolean isPunishable();

    int getMaxViolations();

    String[] getPunishmentCommands();
}
