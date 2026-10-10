// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Optional;

/**
 * The Steam Engine's anchor: a steam tank and an FE buffer.
 *
 * <p>It burns {@link SteamEngineSpec}'s rate from its own tank, which a pipe or a Boiler's steam port
 * fills through the fluid face on every block of the footprint. A pole reaches the buffer through the
 * energy face, extract-only, as a {@link WireworksTags#GENERATORS generator}.
 */
public class SteamEngineBlockEntity extends BlockEntity {

    private static final SteamEngineSpec SPEC = SteamEngineSpec.FACTORIO;

    private long stored;
    private SteamEngineSpec.Carry carry = SteamEngineSpec.Carry.NONE;
    private boolean burning;
    private final LongSnapshotJournal journal = new LongSnapshotJournal(() -> stored, v -> stored = v, this::setChanged);
    private final SteamTank tank = new SteamTank(SteamFluids.STEAM_SOURCE.get(), SPEC.portCapacity(), this::setChanged);
    private final ResourceHandler<FluidResource> steamFace = new FluidFace(tank, true, false);
    private final EnergyHandler energy = new Face();

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.STEAM_ENGINE_ENTITY.get(), pos, state);
    }

    public static SteamEngineSpec spec() {
        return SPEC;
    }

    ResourceHandler<FluidResource> steamFace() {
        return steamFace;
    }

    /** The FE face on every block of the engine. Journalled so a pole's aborted probe takes nothing. */
    public EnergyHandler energy() {
        return energy;
    }

    void serverTick() {
        long room = SPEC.bufferCapacity() - stored;
        SteamEngineSpec.Request asked = SPEC.request(carry, room);
        SteamEngineSpec.Tick made;
        try (Transaction transaction = Transaction.openRoot()) {
            int drawn = asked.steam() > 0 ? tank.extract(tank.fluid(), asked.steam(), transaction) : 0;
            made = SPEC.burn(drawn, asked.carry(), room);
            transaction.commit();
        }
        boolean changed = made.energy() > 0L || !made.carry().equals(carry);
        carry = made.carry();
        stored += made.energy();
        burning = made.steam() > 0;
        if (changed) {
            setChanged();
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
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", stored);
        output.putDouble("CarrySteam", carry.steam());
        output.putDouble("CarryEnergy", carry.energy());
        output.putInt("Steam", tank.amount());
    }

    private final class Face implements EnergyHandler {

        @Override
        public long getAmountAsLong() {
            return stored;
        }

        @Override
        public long getCapacityAsLong() {
            return SPEC.bufferCapacity();
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
