package net.strealex.totemguard.data;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

@Getter
@Setter
@Builder
public class CheckDetails {
    private String checkName;
    private String checkDescription;
    private Component alert;
    private int violations;
    private int tps;
    private int ping;
    private String gamemode;
    private boolean experimental;
    private boolean enabled;
    private boolean punishable;
    private int maxViolations;
    private String[] punishmentCommands;
}
