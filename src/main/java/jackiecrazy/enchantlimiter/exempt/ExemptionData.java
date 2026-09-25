package jackiecrazy.enchantlimiter.exempt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Per-world set of players that are exempt from the enchantment limit.
 * Stored in the overworld's data folder (data/enchantlimiter_exemptions.dat) so it belongs to
 * the save, not to the server config, and works for offline team members too.
 */
public class ExemptionData extends SavedData {
    private static final String NAME = "enchantlimiter_exemptions";
    private final Set<UUID> exempt = new HashSet<>();

    public static ExemptionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ExemptionData::load, ExemptionData::new, NAME);
    }

    private static ExemptionData load(CompoundTag tag) {
        ExemptionData data = new ExemptionData();
        for (Tag t : tag.getList("players", Tag.TAG_INT_ARRAY)) {
            data.exempt.add(NbtUtils.loadUUID(t));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (UUID id : exempt) list.add(NbtUtils.createUUID(id));
        tag.put("players", list);
        return tag;
    }

    public boolean contains(UUID id) {
        return exempt.contains(id);
    }

    /**
     * @return true if the stored value changed
     */
    public boolean set(UUID id, boolean value) {
        boolean changed = value ? exempt.add(id) : exempt.remove(id);
        if (changed) setDirty();
        return changed;
    }
}
