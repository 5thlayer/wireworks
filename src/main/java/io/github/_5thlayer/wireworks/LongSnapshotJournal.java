// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

/**
 * An energy buffer's undo, spelled once.
 *
 * <p>A pole measures a machine's room with an insert it then aborts, so every energy face the network
 * reaches has to be able to put a stored amount back. Each holds the same long, so each wants the
 * same journal: read it before the mutation, write it back on an abort, and mark the block entity
 * dirty once the outermost transaction commits. A face that skips the snapshot does not fail: it
 * keeps the probe's worth of energy the pole never meant to hand it.
 */
public final class LongSnapshotJournal extends SnapshotJournal<Long> {

    private final LongSupplier read;
    private final LongConsumer write;
    private final Runnable onChange;

    public LongSnapshotJournal(LongSupplier read, LongConsumer write, Runnable onChange) {
        this.read = read;
        this.write = write;
        this.onChange = onChange;
    }

    @Override
    protected Long createSnapshot() {
        return read.getAsLong();
    }

    @Override
    protected void revertToSnapshot(Long snapshot) {
        write.accept(snapshot);
    }

    @Override
    protected void onRootCommit(Long originalState) {
        if (read.getAsLong() != originalState) {
            onChange.run();
        }
    }
}
