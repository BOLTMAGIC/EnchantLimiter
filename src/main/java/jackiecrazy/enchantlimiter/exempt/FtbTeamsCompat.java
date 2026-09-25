package jackiecrazy.enchantlimiter.exempt;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * FTB Teams integration. Only touched when the "ftbteams" mod is loaded, so the FTB classes
 * are never resolved otherwise.
 */
final class FtbTeamsCompat {
    private FtbTeamsCompat() {
    }

    /**
     * All members of the player's party team, including the player. Returns an empty set when
     * the player has no party (FTB gives every player a solo team, which contains only them).
     */
    static Set<UUID> teamMembers(UUID player) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) return Collections.emptySet();
        return FTBTeamsAPI.api().getManager().getTeamForPlayerID(player)
                .map(Team::getMembers)
                .orElse(Collections.emptySet());
    }
}
