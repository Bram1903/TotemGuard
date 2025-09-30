package com.deathmotion.totemguard.util;

import com.deathmotion.totemguard.TotemGuard;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;

@UtilityClass
public class MessageUtil {

    public Component format(String message) {
        Formatter formatter = TotemGuard.getInstance().getConfigManager().getMessages().getFormat();
        return formatter.format(message);
    }

    public String unformat(Component component) {
        Formatter formatter = TotemGuard.getInstance().getConfigManager().getMessages().getFormat();
        return formatter.unformat(component);
    }

    public Component getPrefix() {
        return format(TotemGuard.getInstance().getConfigManager().getMessages().getPrefix());
    }
}
