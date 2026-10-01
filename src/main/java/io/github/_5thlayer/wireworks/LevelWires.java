// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import com.mojang.serialization.Codec;

import io.github._5thlayer.wireworks.network.PoleWiresPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A level's wires, saved with it (ADR 0004). The rules are {@link PoleWiring}'s and the storage
 * {@link WireSet}'s; this is where the two meet a world.
 */
public final class LevelWires extends SavedData {

    private static final Codec<LevelWires> CODEC = WireSet.CODEC
            .xmap(LevelWires::new, data -> data.wires)
            .fieldOf("wires")
            .codec();

    public static final SavedDataType<LevelWires> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "pole_wires"),
            LevelWires::new, CODEC);

    private final WireSet wires;

    public LevelWires() {
        this(new WireSet());
    }

    private LevelWires(WireSet wires) {
        this.wires = wires;
    }

    public static LevelWires of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public WireSet wires() {
        return wires;
    }

    public boolean contains(BlockPos a, BlockPos b) {
        return wires.contains(pos(a), pos(b));
    }

    /** A wire tool's second click, from the pole at {@code anchor} to the pole at {@code target}. */
    public PoleWiring.Click click(ServerLevel level, BlockPos anchor, BlockPos target) {
        PoleNetworks.Pole a = poleAt(level, anchor);
        PoleNetworks.Pole b = poleAt(level, target);
        if (a == null || b == null) {
            return PoleWiring.Click.REFUSED;
        }
        PoleWiring.Click click = PoleWiring.click(a, b, wires);
        if (click == PoleWiring.Click.WIRED || click == PoleWiring.Click.CUT) {
            changed(level, anchor, target);
        }
        return click;
    }

    /**
     * A pole's base was placed at {@code pos}: it wires itself as {@link PoleWiring#onPlace} says.
     * An extension added on top of a column changes no wire, since a wire names a column's base.
     */
    public void placed(ServerLevel level, BlockPos pos) {
        PoleNetworks.Pole placed = poleAt(level, pos);
        if (placed == null) {
            return;
        }
        // A pole placed under a standing column of the same tier is that column growing downwards,
        // not a new pole: its wires move to the new base and it adds none of its own. Where the
        // placement also joins a column below, the new base is that lower column's.
        BlockPos above = pos.above();
        if (level.getBlockState(above).is(level.getBlockState(pos).getBlock())) {
            BlockPos base = PoleColumn.baseOf(level, pos);
            rekeyed(level, pos(above), pos(base));
            return;
        }
        if (!PoleColumn.isBase(level, pos)) {
            return;
        }
        List<PoleNetworks.Pole> wired = PoleWiring.onPlace(placed, standingNear(level, pos, placed.tier()), wires);
        List<BlockPos> touched = new ArrayList<>();
        touched.add(pos);
        for (PoleNetworks.Pole other : wired) {
            wires.add(pos(pos), new PoleNetworks.Pos(other.x(), other.y(), other.z()));
            touched.add(new BlockPos(other.x(), other.y(), other.z()));
        }
        changed(level, touched.toArray(BlockPos[]::new));
    }

    /** Moves a column's wires to its new base, telling every end's chunk about it. */
    private void rekeyed(ServerLevel level, PoleNetworks.Pos from, PoleNetworks.Pos to) {
        List<BlockPos> touched = wiredTo(from);
        touched.add(block(to));
        wires.rekey(from, to);
        changed(level, touched.toArray(BlockPos[]::new));
    }

    /** The base of a pole column at {@code pos} was broken: its wires go with it. */
    public void broken(ServerLevel level, BlockPos pos) {
        List<BlockPos> touched = wiredTo(pos(pos));
        wires.removeAllOf(pos(pos));
        changed(level, touched.toArray(BlockPos[]::new));
    }

    /** The pole at {@code end} and every pole a wire joins it to. */
    private List<BlockPos> wiredTo(PoleNetworks.Pos end) {
        List<BlockPos> ends = new ArrayList<>();
        ends.add(block(end));
        for (PoleNetworks.Wire wire : wires.all()) {
            if (wire.a().equals(end)) {
                ends.add(block(wire.b()));
            } else if (wire.b().equals(end)) {
                ends.add(block(wire.a()));
            }
        }
        return ends;
    }

    /**
     * Every pole base within the placed pole's reach, found through the loaded chunks' block
     * entities rather than by walking blocks. A wire's reach is the shorter of its two ends', so the
     * placed pole's own reach bounds the search.
     */
    private static List<PoleNetworks.Pole> standingNear(ServerLevel level, BlockPos pos, PoleTier tier) {
        int reach = (int) Math.ceil(tier.wireReach());
        List<PoleNetworks.Pole> found = new ArrayList<>();
        for (int cx = SectionPos.blockToSectionCoord(pos.getX() - reach);
             cx <= SectionPos.blockToSectionCoord(pos.getX() + reach); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(pos.getZ() - reach);
                 cz <= SectionPos.blockToSectionCoord(pos.getZ() + reach); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos at = be.getBlockPos();
                    if (be instanceof SupplyAreaPoleBlockEntity pole && !at.equals(pos)
                            && PoleColumn.isBase(level, at)) {
                        found.add(pole.shape());
                    }
                }
            }
        }
        return found;
    }

    /** Save, rebuild the networks, and resend each touched chunk's wires to whoever watches it. */
    private void changed(ServerLevel level, BlockPos... touched) {
        setDirty();
        ElectricNetworks.of(level).wiresChanged();
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        for (BlockPos p : touched) {
            chunks.add(ChunkPos.containing(p));
        }
        for (ChunkPos chunk : chunks) {
            PoleWiresPacket packet = new PoleWiresPacket(chunk.x(), chunk.z(), wires.touching(chunk.x(), chunk.z()));
            for (ServerPlayer player : watching(level, chunk)) {
                send(player, packet);
            }
        }
    }

    private static List<ServerPlayer> watching(ServerLevel level, ChunkPos chunk) {
        return level.getChunkSource().chunkMap.getPlayers(chunk, false);
    }

    /** Only to a connection that negotiated the channel; sending to one that didn't throws. */
    private static void send(ServerPlayer player, PoleWiresPacket packet) {
        if (player.connection.hasChannel(packet)) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    /** A chunk reached a player: send the wires with an end in it. */
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ChunkPos chunk = event.getPos();
        List<PoleNetworks.Wire> touching = of(event.getLevel()).wires.touching(chunk.x(), chunk.z());
        send(event.getPlayer(), new PoleWiresPacket(chunk.x(), chunk.z(), touching));
    }

    private static BlockPos block(PoleNetworks.Pos p) {
        return new BlockPos(p.x(), p.y(), p.z());
    }

    static PoleNetworks.Pos pos(BlockPos p) {
        return new PoleNetworks.Pos(p.getX(), p.getY(), p.getZ());
    }

    /** The pole of {@code tier} whose column's base is {@code base}. */
    static PoleNetworks.Pole pole(BlockPos base, PoleTier tier) {
        return new PoleNetworks.Pole(base.getX(), base.getY(), base.getZ(), tier);
    }

    /** The pole whose column holds {@code pos}, named by its base, or null if there is none. */
    private static PoleNetworks.Pole poleAt(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof SupplyAreaPoleBlock pole)) {
            return null;
        }
        return pole(PoleColumn.baseOf(level, pos), pole.tier());
    }
}
