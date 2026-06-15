package net.sorukoma.quasium.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record HookahPipeAttachmentData(BlockPos hookahPos, UUID attachedPlayerUUID) {

    private static final String TAG_HOOKAH_POS = "HookahPos";
    private static final String TAG_PLAYER_UUID = "PlayerUUID";

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong(TAG_HOOKAH_POS, hookahPos.asLong());
        nbt.putUUID(TAG_PLAYER_UUID, attachedPlayerUUID);
        return nbt;
    }

    public static HookahPipeAttachmentData fromNBT(CompoundTag nbt) {
        if (nbt.contains(TAG_HOOKAH_POS) && nbt.contains(TAG_PLAYER_UUID)) {
            BlockPos pos = BlockPos.of(nbt.getLong(TAG_HOOKAH_POS));
            UUID uuid = nbt.getUUID(TAG_PLAYER_UUID);
            return new HookahPipeAttachmentData(pos, uuid);
        }
        return null;
    }
}
