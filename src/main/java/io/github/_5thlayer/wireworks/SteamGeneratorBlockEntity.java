// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.Nullable;

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

    private final SteamEngineSpec spec;
    private final Footprint footprint;
    private long stored;
    private long made;
    private SteamEngineSpec.Carry carry = SteamEngineSpec.Carry.NONE;
    private boolean burning;
    private final LongSnapshotJournal journal = new LongSnapshotJournal(() -> stored, v -> stored = v, this::setChanged);
    private final SingleFluidTank tank;
    private final ResourceHandler<FluidResource> steamFace;
    private final ResourceHandler<FluidResource> enteredAtRowSide;
    private final ResourceHandler<FluidResource> enteredAtRowBack;
    private @Nullable Direction passOn;
    private final EnergyHandler energy = new Face();

    protected SteamGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        SteamEngineSpec spec, Fluid steam, Footprint footprint) {
        super(type, pos, state);
        this.spec = spec;
        this.footprint = footprint;
        tank = new SingleFluidTank(steam, spec.portCapacity(), this::setChanged);
        steamFace = new FluidFace(tank, true, false);
        enteredAtRowSide = new FluidFace(tank, true, false, () -> passOn = rowSide().getOpposite());
        enteredAtRowBack = new FluidFace(tank, true, false, () -> passOn = rowSide());
    }

    /**
     * The fluid face on {@code side}. At either end of the row it remembers which, so the steam it
     * takes goes on out of the other; on every other side, and for no side, it only fills the tank.
     */
    ResourceHandler<FluidResource> steamFace(@Nullable Direction side) {
        Direction rowSide = rowSide();
        if (side == rowSide) {
            return enteredAtRowSide;
        }
        return side == rowSide.getOpposite() ? enteredAtRowBack : steamFace;
    }

    private Direction rowSide() {
        return SteamFootprints.rowSide(getBlockState().getValue(Footprint.FACING));
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
        passSteamOn();
    }

    /** Offers what the tank holds to whatever stands against the row end the steam did not come in at. */
    private void passSteamOn() {
        if (passOn == null || tank.amount() <= 0) {
            return;
        }
        Direction facing = getBlockState().getValue(Footprint.FACING);
        for (BlockPos block : footprint.positions(worldPosition, facing)) {
            ResourceHandler<FluidResource> next =
                    level.getCapability(Capabilities.Fluid.BLOCK, block.relative(passOn), passOn.getOpposite());
            if (next != null) {
                ResourceHandlerUtil.move(tank, next, resource -> true, tank.amount(), null);
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
        passOn = Direction.byName(input.getStringOr("PassOn", ""));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", stored);
        output.putDouble("CarrySteam", carry.steam());
        output.putDouble("CarryEnergy", carry.energy());
        output.putInt("Steam", tank.amount());
        if (passOn != null) {
            output.putString("PassOn", passOn.getName());
        }
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
