package com.strealex.totemguard.discord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Placeholders {

    public static List<String> replacePlaceholders(List<String> messages, Map<String, String> placeholders) {
        List<String> replacedMessages = new ArrayList<>();
        for (String message : messages) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
            replacedMessages.add(message);
        }
        return replacedMessages;
    }

    public static String joinMessages(List<String> messages) {
        return String.join("\n", messages);
    }

}
