package net.strealex.totemguard.data;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TotemPlayer {
    private UUID uuid;
    private String username;
    private String clientBrandName;
    private ClientVersion clientVersion;
}
