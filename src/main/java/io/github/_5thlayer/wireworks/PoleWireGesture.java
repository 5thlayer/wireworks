// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;


import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Wiring and unwiring poles by hand with any item in {@link WireworksTags#WIRE_TOOLS}.
 *
 * <p>The first click on a pole holds its base on the tool; the second is {@link LevelWires#click}.
 * Feedback is in the world and never text: a sound per outcome, and particles along a wire made or
 * cut. The rules are {@link PoleWiring} and {@link PendingEnd}; this is where they meet a click.
 */
public final class PoleWireGesture {

    /** Particles per block of wire. */
    private static final double PARTICLES_PER_BLOCK = 2.0;

    private PoleWireGesture() {
    }

    /** A main-hand right-click with a wire tool on a pole; any other click is left alone. */
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().is(WireworksTags.WIRE_TOOLS)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos clicked = event.getPos();
        if (!(level.getBlockState(clicked).getBlock() instanceof SupplyAreaPoleBlock)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(level instanceof ServerLevel server
                ? click(server, event.getItemStack(), clicked, event.getEntity())
                : InteractionResult.SUCCESS);
    }

    private static InteractionResult click(ServerLevel server, ItemStack pick, BlockPos clicked, Player player) {
        Level level = server;
        BlockPos base = PoleColumn.baseOf(level, clicked);
        GlobalPos pending = pick.get(WireworksRegistries.PENDING_WIRE.get());
        if (pending == null || !pending.dimension().equals(level.dimension())) {
            pick.set(WireworksRegistries.PENDING_WIRE.get(), GlobalPos.of(level.dimension(), base));
            return InteractionResult.SUCCESS_SERVER;
        }
        PoleWiring.Click click = LevelWires.of(server).click(server, pending.pos(), base);
        switch (click) {
            case WIRED -> {
                pick.remove(WireworksRegistries.PENDING_WIRE.get());
                play(server, base, SoundEvents.TRIPWIRE_ATTACH);
                alongWire(server, pending.pos(), base, ParticleTypes.ELECTRIC_SPARK);
            }
            case CUT -> {
                pick.remove(WireworksRegistries.PENDING_WIRE.get());
                play(server, base, SoundEvents.TRIPWIRE_DETACH);
                alongWire(server, pending.pos(), base, ParticleTypes.SMOKE);
            }
            case CANCELLED -> pick.remove(WireworksRegistries.PENDING_WIRE.get());
            // The end stays held: a refusal changes nothing, the held end included.
            // Not DISPENSER_FAIL: it plays random/click, the same file as TRIPWIRE_ATTACH. Played at
            // the player, not the pole: vanilla attenuates the crafter's fail over 3 blocks, and a
            // pole beyond wire reach is always further than that.
            case REFUSED -> {
                Vec3 at = player.position();
                server.playSound(null, at.x, at.y, at.z, SoundEvents.CRAFTER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Lets go of every held end the moment {@link PendingEnd} says it is no longer held. */
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getEntity();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.has(WireworksRegistries.PENDING_WIRE.get())) {
                release(stack, level, player, stack == player.getMainHandItem());
            }
        }
    }

    private static void release(ItemStack pick, ServerLevel level, Player holder, boolean mainHand) {
        GlobalPos pending = pick.get(WireworksRegistries.PENDING_WIRE.get());
        if (pending == null) {
            return;
        }
        boolean sameDimension = pending.dimension().equals(level.dimension());
        BlockPos anchor = pending.pos();
        boolean standing = sameDimension && level.isLoaded(anchor)
                && level.getBlockState(anchor).getBlock() instanceof SupplyAreaPoleBlock
                && PoleColumn.isBase(level, anchor);
        PoleTier tier = standing
                ? ((SupplyAreaPoleBlock) level.getBlockState(anchor).getBlock()).tier()
                : PoleTier.SMALL;
        PoleLinks.Pole pole = new PoleLinks.Pole(anchor.getX(), anchor.getY(), anchor.getZ(), tier);
        double range = holder.blockInteractionRange();
        if (!PendingEnd.stillHeld(pole, holder.getX(), holder.getY(), holder.getZ(), range,
                standing, mainHand, sameDimension)) {
            pick.remove(WireworksRegistries.PENDING_WIRE.get());
            // The snap: dropping an end is heard at the player, since nothing else shows it. A
            // chain's break, so it is none of the made, cut or refused sounds.
            level.playSound(null, holder.blockPosition(), SoundEvents.CHAIN_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void play(ServerLevel level, BlockPos at, SoundEvent sound) {
        level.playSound(null, at, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void alongWire(ServerLevel level, BlockPos fromBase, BlockPos toBase, ParticleOptions particle) {
        Vec3 from = attachPoint(level, fromBase);
        Vec3 to = attachPoint(level, toBase);
        int count = Math.max(2, (int) Math.ceil(from.distanceTo(to) * PARTICLES_PER_BLOCK));
        for (int i = 0; i <= count; i++) {
            Vec3 p = from.lerp(to, (double) i / count);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** The top of a column, where a wire hangs from. */
    private static Vec3 attachPoint(Level level, BlockPos base) {
        BlockPos top = PoleColumn.topOf(level, base);
        if (top == null) {
            top = base;
        }
        return new Vec3(top.getX() + 0.5, top.getY() + 0.9, top.getZ() + 0.5);
    }
}
