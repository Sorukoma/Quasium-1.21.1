package net.sorukoma.quasium.ritual;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;

import java.util.Map;

public record RitualRecipesPacket(Map<ResourceLocation, RitualRecipe> recipes) implements CustomPacketPayload {
    public static final Type<RitualRecipesPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ritual_recipes"));
    public static final StreamCodec<ByteBuf, RitualRecipesPacket> STREAM_CODEC =
            ByteBufCodecs.fromCodec(RitualRecipes.MAP_CODEC).map(RitualRecipesPacket::new, RitualRecipesPacket::recipes);

    public static RitualRecipesPacket current() {
        return new RitualRecipesPacket(RitualRecipes.all());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RitualRecipesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> RitualRecipes.set(packet.recipes()));
    }
}
