// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Optional;

/**
 * The anchor of a generator that burns one steam: a tank of that steam and an FE buffer. The Steam
 * Engine burns Steam and the Steam Turbine Superheated Steam; each refuses the other's.
 *
 * <p>It burns its {@link SteamEngineSpec}'s rate from its own tank, which a pipe, a Boiler's steam
 * port or a Reactor fills through the fluid face on every block of the footprint. Steam that comes in
 * at one end of its row leaves by the other, so generators in a row share it (ADR-0011). A pole
 * reaches the buffer through the energy face, extract-only, as a {@link WireworksTags#GENERATORS generator}.
 */
public abstract class SteamGeneratorBlockEntity extends BlockEntity {

    /** How long a generator shows itself running after the last burn: a second. */
    private static final int RUNNING_HOLD_TICKS = 20;

    private final SteamEngineSpec spec;
    private final Footprint footprint;
    private long stored;
    private long made;
    private SteamEngineSpec.Carry carry = SteamEngineSpec.Carry.NONE;
    private boolean burning;
    /** Ticks since the generator last burnt, up to {@link #RUNNING_HOLD_TICKS}; it starts still. */
    private int ticksSinceBurnt = RUNNING_HOLD_TICKS;
    private final LongSnapshotJournal journal = new LongSnapshotJournal(() -> stored, v -> stored = v, this::setChanged);
    private final SingleFluidTank tank;
    private final SteamPorts.Row ports;
    private final EnergyHandler energy = new Face();

    protected SteamGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        SteamEngineSpec spec, Fluid steam, Footprint footprint) {
        super(type, pos, state);
        this.spec = spec;
        this.footprint = footprint;
        tank = new SingleFluidTank(steam, spec.portCapacity(), this::setChanged);
        ports = SteamPorts.row(this, footprint, tank);
    }

    /** The row's Ports, whose faces every block of the footprint answers with (ADR-0011). */
    SteamPorts.Row ports() {
        return ports;
    }

    /** The FE face on every block of the footprint. Journalled so a pole's aborted probe takes nothing. */
    public EnergyHandler energy() {
        return energy;
    }

    /** The FE the last tick made, which Jade shows as the generator's output. */
    public long lastMade() {
        return made;
    }

    void serverTick() {
        ports.pullIn();
        long room = spec.bufferCapacity() - stored;
        SteamEngineSpec.Request asked = spec.request(carry, room);
        SteamEngineSpec.Tick burnt;
        try (Transaction transaction = Transaction.openRoot()) {
            int drawn = asked.steam() > 0 ? tank.extract(tank.fluid(), asked.steam(), transaction) : 0;
            burnt = spec.burn(drawn, asked.carry(), room);
            transaction.commit();
        }
        boolean changed = burnt.energy() > 0L || !burnt.carry().equals(carry);
        carry = burnt.carry();
        stored += burnt.energy();
        made = burnt.energy();
        burning = burnt.steam() > 0;
        if (changed) {
            setChanged();
        }
        ticksSinceBurnt = burning ? 0 : Math.min(ticksSinceBurnt + 1, RUNNING_HOLD_TICKS);
        showRunning(ticksSinceBurnt < RUNNING_HOLD_TICKS);
        ports.passOn();
    }

    /**
     * Sets {@link SteamGeneratorBlock#RUNNING} while the generator burns and for
     * {@link #RUNNING_HOLD_TICKS} after, so one at part load, which burns on some ticks and not others,
     * does not flicker. It is set on every block of the footprint that carries it: the Turbine's anchor
     * alone, whose fan is the only thing that moves, and each of the Engine's blocks, since its engine
     * is drawn across all four. Each block is asked, not the anchor alone, since a turn lays the parts
     * afresh, still. A block of the footprint that is not a generator's, as after a part was replaced,
     * is left, even one with a {@code running} property of its own.
     */
    private void showRunning(boolean running) {
        if (level == null) {
            return;
        }
        Direction facing = getBlockState().getValue(Footprint.FACING);
        for (BlockPos pos : footprint.positions(worldPosition, facing)) {
            BlockState state = level.getBlockState(pos);
            boolean generators = state.getBlock() instanceof SteamGeneratorBlock
                    || state.getBlock() instanceof SteamEnginePartBlock;
            if (generators && state.getValue(SteamGeneratorBlock.RUNNING) != running) {
                level.setBlock(pos, state.setValue(SteamGeneratorBlock.RUNNING, running), Block.UPDATE_ALL);
            }
        }
    }

    /** What the Jade line says: no steam, or steam with no pole drawing it. */
    public Optional<SteamEngineStatus> status() {
        return SteamEngineStatus.of(burning || tank.amount() > 0, ElectricNetworks.of(level).drawsFrom(worldPosition));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stored = Math.max(0L, input.getLongOr("Energy", 0L));
        carry = new SteamEngineSpec.Carry(input.getDoubleOr("CarrySteam", 0.0), input.getDoubleOr("CarryEnergy", 0.0));
        tank.fill(input.getIntOr("Steam", 0));
        ports.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", stored);
        output.putDouble("CarrySteam", carry.steam());
        output.putDouble("CarryEnergy", carry.energy());
        output.putInt("Steam", tank.amount());
        ports.save(output);
    }

    private final class Face implements EnergyHandler {

        @Override
        public long getAmountAsLong() {
            return stored;
        }

        @Override
        public long getCapacityAsLong() {
            return spec.bufferCapacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            int taken = (int) Math.min(amount, stored);
            if (taken > 0) {
                journal.updateSnapshots(transaction);
                stored -= taken;
            }
            return taken;
        }
    }
}
