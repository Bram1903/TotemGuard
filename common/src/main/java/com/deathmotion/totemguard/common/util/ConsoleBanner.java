package com.deathmotion.totemguard.common.util;

import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.deathmotion.totemguard.common.TGPlatform;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

@UtilityClass
public final class ConsoleBanner {

    private final DateTimeFormatter BUILD_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.systemDefault());

    public void print() {
        Logger logger = TGPlatform.getInstance().getLogger();

        List<String> totem = List.of(
                "         ###########         ",
                "       ##+++++++++++##       ",
                "       %%++=======++%%       ",
                "       %%***+==++***%%       ",
                " ##########*+==++*########## ",
                " ##*+**##*********####***##% ",
                "  #######=====++++**#####%%  ",
                "       ##++*********%%       ",
                "       %%###########%%       ",
                "         %#*******%%         ",
                "          %%%%%%%%%          "
        );

        TGVersion version = TGVersions.CURRENT;

        String versionString = version.snapshot()
                ? version.major() + "." + version.minor() + "." + version.patch()
                + "-SNAPSHOT (" + version.snapshotCommit() + ")"
                : version.major() + "." + version.minor() + "." + version.patch();

        List<String> text = List.of(
                "TotemGuard",
                "Version    : " + versionString,
                "Build Time : " + BUILD_FORMAT.format(TGVersions.BUILD_TIMESTAMP),
                "Edition    : Public",
                "Authors    : Bram & OutDev"
        );

        logger.info(generateBanner(totem, text).toString());
    }

    private @NonNull StringBuilder generateBanner(List<String> totem, List<String> text) {
        int totemHeight = totem.size();
        int textHeight = text.size();
        int textStartRow = (totemHeight - textHeight) / 2;
        int gap = 3;

        StringBuilder banner = new StringBuilder();
        banner.append('\n');

        for (int i = 0; i < totemHeight; i++) {
            banner.append(totem.get(i));

            int textIndex = i - textStartRow;
            if (textIndex >= 0 && textIndex < textHeight) {
                banner.append(" ".repeat(gap));
                banner.append(text.get(textIndex));
            }

            if (i < totemHeight - 1) {
                banner.append('\n');
            }
        }
        return banner;
    }
}
