// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Optional;

/**
 * The Accumulator's buffer, on the network as an {@link WireworksTags#ACCUMULATORS accumulator}. It
 * moves {@link AccumulatorSpec#inputFePerTick()} a call each way at most, and holds at most
 * {@link AccumulatorSpec#capacityFe()}.
 */
public class AccumulatorBlockEntity extends BlockEntity {

    private static final String ENERGY = "energy";

    private long stored;
    private final LongSnapshotJournal journal = new LongSnapshotJournal(() -> stored, value -> stored = value, this::setChanged);
    private final EnergyHandler face = new Face();

    public AccumulatorBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.ACCUMULATOR_ENTITY.get(), pos, state);
    }

    public EnergyHandler energy() {
        return face;
    }

    /** What the Jade line says, from the network's last tick. */
    public Optional<AccumulatorStatus> status() {
        ElectricNetworks networks = ElectricNetworks.of(level);
        return AccumulatorStatus.of(networks.chargesFrom(getBlockPos()), networks.accumulatorFlow(getBlockPos()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong(ENERGY, stored);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stored = Math.max(0L, input.getLongOr(ENERGY, 0L));
    }

    private final class Face implements EnergyHandler {

        @Override
        public long getAmountAsLong() {
            return stored;
        }

        @Override
        public long getCapacityAsLong() {
            return AccumulatorSpec.capacityFe();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            long room = Math.max(0L, AccumulatorSpec.capacityFe() - stored);
            int taken = (int) Math.min(amount, Math.min(AccumulatorSpec.inputFePerTick(), room));
            if (taken > 0) {
                journal.updateSnapshots(transaction);
                stored += taken;
            }
            return taken;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            int given = (int) Math.min(amount, Math.min(AccumulatorSpec.outputFePerTick(), stored));
            if (given > 0) {
                journal.updateSnapshots(transaction);
                stored -= given;
            }
            return given;
        }
    }
}
