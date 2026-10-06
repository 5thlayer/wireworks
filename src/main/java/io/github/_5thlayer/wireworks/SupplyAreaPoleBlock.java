// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A pole after Factorio's (ADR 0002, ADR 0003): it supplies every machine standing in its area, and
 * the poles its wires join are one Electric Network.
 *
 * <p>The wires are saved with the level ({@link LevelWires}); the networks are recomputed from them
 * and the poles standing. {@link ElectricNetworks} has the why.
 *
 * <p>One class serves all three tiers; they differ by the {@link PoleTier} handed to the
 * constructor and by nothing else. The column is {@link PoleBlock}'s.
 */
public class SupplyAreaPoleBlock extends PoleBlock implements EntityBlock {

    private final PoleTier tier;

    public SupplyAreaPoleBlock(PoleTier tier, BlockBehaviour.Properties props) {
        super(props);
        this.tier = tier;
    }

    public PoleTier tier() {
        return tier;
    }

    @Override
    public PoleKind kind() {
        return PoleKind.distribution(tier);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyAreaPoleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        // Server only: the pole has nothing to animate, and pushing energy on the client would be
        // pushing it into a copy of the world.
        if (level.isClientSide() || type != WireworksRegistries.SUPPLY_AREA_POLE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            // Only the base's block entity is ticked. An extension is given one by Minecraft --
            // a block entity is built from the blockstate, which cannot see the block below -- and
            // it stays inert. This is the gate that makes a five-tall pole cost what a one-tall
            // pole costs.
            if (be instanceof SupplyAreaPoleBlockEntity pole && PoleColumn.isBase(lvl, pos)) {
                pole.serverTick();
            }
        };
    }
}
