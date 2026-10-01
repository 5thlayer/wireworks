// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.network;

import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.ClientWires;
import io.github._5thlayer.wireworks.PoleNetworks;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Every wire with an end in one chunk, server to client (ADR 0004).
 *
 * <p>Sent when a player starts watching the chunk and again whenever a wire touching it is made or
 * cut. The client replaces the chunk's wires whole rather than applying a change, so a wire cut
 * while nobody watched either end cannot survive on a client that comes back.
 */
public record PoleWiresPacket(int chunkX, int chunkZ, List<PoleNetworks.Wire> wires) implements CustomPacketPayload {

    public static final Type<PoleWiresPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "pole_wires"));

    private static final StreamCodec<ByteBuf, PoleNetworks.Pos> POS = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PoleNetworks.Pos::x,
            ByteBufCodecs.VAR_INT, PoleNetworks.Pos::y,
            ByteBufCodecs.VAR_INT, PoleNetworks.Pos::z,
            PoleNetworks.Pos::new);

    private static final StreamCodec<ByteBuf, PoleNetworks.Wire> WIRE = StreamCodec.composite(
            POS, PoleNetworks.Wire::a,
            POS, PoleNetworks.Wire::b,
            PoleNetworks.Wire::new);

    public static final StreamCodec<ByteBuf, PoleWiresPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PoleWiresPacket::chunkX,
            ByteBufCodecs.VAR_INT, PoleWiresPacket::chunkZ,
            WIRE.apply(ByteBufCodecs.list()), PoleWiresPacket::wires,
            PoleWiresPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PoleWiresPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientWires.accept(packet.chunkX(), packet.chunkZ(), packet.wires()));
    }
}
