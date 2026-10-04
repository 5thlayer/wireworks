// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Solar Panel's buffer, on the network as a {@link WireworksTags#GENERATORS generator}: one tick
 * of {@link SolarPanelSpec#peakFePerTick() peak} times the {@link SolarDayCurve}'s multiplier, and
 * nothing while a roof hides the sky. A pole pulls it; nothing else takes it, and nothing can put FE in.
 */
public class SolarPanelBlockEntity extends BlockEntity {

    /** The footprint is two blocks tall, so the sky is asked one above its top layer's centre. */
    private static final int SKY_PROBE_HEIGHT = 2;

    private static final String ENERGY = "energy";

    private long stored;
    private double carry;
    private long output;
    private boolean skyHidden;
    private final LongSnapshotJournal journal = new LongSnapshotJournal(() -> stored, value -> stored = value, this::setChanged);
    private final EnergyHandler face = new Face();

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.SOLAR_PANEL_ENTITY.get(), pos, state);
    }

    public EnergyHandler energy() {
        return face;
    }

    /** Whole FE made last tick. */
    public long currentOutputFe() {
        return output;
    }

    /** Whether a block above the top layer hid the sky last tick. */
    public boolean skyHidden() {
        return skyHidden;
    }

    void serverTick() {
        skyHidden = !level.canSeeSky(worldPosition.above(SKY_PROBE_HEIGHT));
        double multiplier = skyHidden ? 0.0 : SolarDayCurve.multiplier(SolarDayCurve.fromClockFraction(DayFraction.of(level)));
        SolarOutput.Tick tick = SolarOutput.tick(SolarPanelSpec.peakFePerTick(), multiplier, carry);
        carry = tick.carry();
        output = tick.fe();
        long buffer = SolarPanelSpec.bufferFe();
        long filled = Math.min(buffer, stored + output);
        if (filled != stored) {
            stored = filled;
            setChanged();
        }
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
            return SolarPanelSpec.bufferFe();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            int given = (int) Math.min(amount, stored);
            if (given > 0) {
                journal.updateSnapshots(transaction);
                stored -= given;
            }
            return given;
        }
    }
}
