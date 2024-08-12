package com.strealex.totemguard.checks;

public interface ICheckSettings {
    boolean isEnabled();

    boolean isPunishable();

    int getMaxViolations();

    String[] getPunishmentCommands();
}
