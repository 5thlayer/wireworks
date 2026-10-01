// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.wireworks.Wireworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Set;

/**
 * A machine for the tests to feed, since Wireworks ships none: a block whose FE face takes up to
 * {@link #CAPACITY} and gives nothing back. Registered only when game tests are enabled.
 */
final class TestConsumer {

    static final long CAPACITY = 1000;

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Wireworks.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Wireworks.MOD_ID);

    static final DeferredBlock<ConsumerBlock> BLOCK = BLOCKS.registerBlock("gametest_consumer", ConsumerBlock::new);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConsumerEntity>> TYPE =
            BLOCK_ENTITIES.register("gametest_consumer",
                    () -> new BlockEntityType<>(ConsumerEntity::new, Set.of(BLOCK.get())));

    private TestConsumer() {
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(TestConsumer::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TYPE.get(), (entity, side) -> entity.face);
    }

    static final class ConsumerBlock extends BaseEntityBlock {

        private static final MapCodec<ConsumerBlock> CODEC = simpleCodec(ConsumerBlock::new);

        ConsumerBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ConsumerEntity(pos, state);
        }
    }

    static final class ConsumerEntity extends BlockEntity {

        long stored;
        final Face face = new Face();

        ConsumerEntity(BlockPos pos, BlockState state) {
            super(TYPE.get(), pos, state);
        }

        /** Journalled, so a pole's aborted probe leaves nothing behind. */
        final class Face extends SnapshotJournal<Long> implements EnergyHandler {

            @Override
            public long getAmountAsLong() {
                return stored;
            }

            @Override
            public long getCapacityAsLong() {
                return CAPACITY;
            }

            @Override
            public int insert(int amount, TransactionContext transaction) {
                int taken = (int) Math.min(Math.max(amount, 0), CAPACITY - stored);
                if (taken > 0) {
                    updateSnapshots(transaction);
                    stored += taken;
                }
                return taken;
            }

            @Override
            public int extract(int amount, TransactionContext transaction) {
                return 0;
            }

            @Override
            protected Long createSnapshot() {
                return stored;
            }

            @Override
            protected void revertToSnapshot(Long snapshot) {
                stored = snapshot;
            }
        }
    }
}
