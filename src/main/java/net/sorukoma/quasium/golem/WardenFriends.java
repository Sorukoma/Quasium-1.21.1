package net.sorukoma.quasium.golem;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class WardenFriends {
    private final Map<UUID, String> friends = new LinkedHashMap<>();
    private final Set<UUID> grudges = new HashSet<>();
    @Nullable
    private UUID owner;
    private String ownerName = "";

    @Nullable
    UUID owner() {
        return owner;
    }

    String ownerName() {
        return ownerName;
    }

    boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    boolean isFriend(UUID id) {
        return id.equals(owner) || friends.containsKey(id);
    }

    boolean bearsGrudge(UUID id) {
        return grudges.contains(id) && !isFriend(id);
    }

    void holdGrudge(UUID id) {
        if (!isFriend(id)) grudges.add(id);
    }

    void claim(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        friends.remove(owner);
    }

    Component edit(Player player, String name, boolean crossOut) {
        if (owner == null) return Component.translatable("quasium.warden.ownerless");
        if (!canManage(player)) return Component.translatable("quasium.warden.not_owner", ownerName);
        if (name.isEmpty() || !StringUtil.isValidPlayerName(name)) return Component.translatable("quasium.warden.bad_name");
        if (name.equalsIgnoreCase(ownerName)) return Component.translatable("quasium.warden.owner_always");
        if (crossOut) {
            boolean removed = friends.values().removeIf(friend -> friend.equalsIgnoreCase(name));
            return Component.translatable(removed ? "quasium.warden.friend_removed" : "quasium.warden.not_friend", name);
        }
        GameProfile profile = findProfile(player.level(), name);
        if (profile == null) return Component.translatable("quasium.warden.offline", name);
        if (profile.getId().equals(owner)) return Component.translatable("quasium.warden.owner_always");
        if (friends.put(profile.getId(), profile.getName()) != null) {
            return Component.translatable("quasium.warden.already_friend", profile.getName());
        }
        grudges.remove(profile.getId());
        return Component.translatable("quasium.warden.friend_added", profile.getName());
    }

    Component describeFor(Player player) {
        if (canManage(player)) {
            return friends.isEmpty() ? Component.translatable("quasium.warden.friends_none")
                    : Component.translatable("quasium.warden.friends", String.join(", ", friends.values()));
        }
        return Component.translatable(isFriend(player.getUUID()) ? "quasium.warden.you_friend" : "quasium.warden.you_stranger");
    }

    private boolean canManage(Player player) {
        return owner != null && (isOwner(player) || player.hasPermissions(2));
    }

    @Nullable
    private static GameProfile findProfile(Level level, String name) {
        for (Player player : level.players()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(name)) return player.getGameProfile();
        }
        MinecraftServer server = level.getServer();
        ServerPlayer elsewhere = server.getPlayerList().getPlayerByName(name);
        if (elsewhere != null) return elsewhere.getGameProfile();
        return server.usesAuthentication() ? null : UUIDUtil.createOfflineProfile(name);
    }

    void save(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        ListTag list = new ListTag();
        friends.forEach((id, name) -> {
            CompoundTag friend = new CompoundTag();
            friend.putUUID("Id", id);
            friend.putString("Name", name);
            list.add(friend);
        });
        tag.put("Friends", list);
    }

    void load(CompoundTag tag, @Nullable MinecraftServer server) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = owner == null ? "" : tag.contains("OwnerName") ? tag.getString("OwnerName") : nameOf(owner, server);
        friends.clear();
        for (Tag entry : tag.getList("Friends", Tag.TAG_COMPOUND)) {
            CompoundTag friend = (CompoundTag) entry;
            friends.put(friend.getUUID("Id"), friend.getString("Name"));
        }
        for (Tag entry : tag.getList("Whitelist", Tag.TAG_INT_ARRAY)) {
            UUID id = NbtUtils.loadUUID(entry);
            if (!id.equals(owner)) friends.putIfAbsent(id, nameOf(id, server));
        }
    }

    private static String nameOf(UUID id, @Nullable MinecraftServer server) {
        GameProfileCache cache = server == null ? null : server.getProfileCache();
        Optional<GameProfile> profile = cache == null ? Optional.empty() : cache.get(id);
        return profile.map(GameProfile::getName).orElse(id.toString().substring(0, 8));
    }
}
