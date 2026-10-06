// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One tick of an Electric Network's books (ADR 0003): generators before accumulators, only
 * generator surplus charges, and generators share their load in proportion to what each can give.
 */
class NetworkBalanceTest {

    private static final long[] NONE = {};

    @Test
    void generatorsMeetDemandAndAccumulatorsAreUntouched() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{450}, new long[]{150}, new long[]{0}, new long[]{90});
        assertArrayEquals(new long[]{90}, s.consumerGrants());
        assertArrayEquals(new long[]{90}, s.generatorDraws());
        assertArrayEquals(new long[]{0}, s.accumulatorDischarges());
    }

    @Test
    void accumulatorsCoverOnlyTheShortfall() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{50}, new long[]{150}, new long[]{0}, new long[]{90});
        assertArrayEquals(new long[]{90}, s.consumerGrants());
        assertArrayEquals(new long[]{50}, s.generatorDraws());
        assertArrayEquals(new long[]{40}, s.accumulatorDischarges());
    }

    @Test
    void onlyGeneratorSurplusCharges() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{450}, new long[]{0}, new long[]{150}, new long[]{90});
        assertArrayEquals(new long[]{150}, s.accumulatorCharges());
        assertArrayEquals(new long[]{240}, s.generatorDraws());
    }

    @Test
    void anAccumulatorNeverChargesFromAnother() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                NONE, new long[]{150, 0}, new long[]{0, 150}, NONE);
        assertArrayEquals(new long[]{0, 0}, s.accumulatorDischarges());
        assertArrayEquals(new long[]{0, 0}, s.accumulatorCharges());
    }

    @Test
    void generatorsShareLoadInProportionToWhatTheyCanGive() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{300, 100}, NONE, NONE, new long[]{200});
        assertArrayEquals(new long[]{150, 50}, s.generatorDraws());
    }

    @Test
    void proportionalRemaindersAreNotLost() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{100, 100, 100}, NONE, NONE, new long[]{100});
        assertEquals(100, Arrays.stream(s.generatorDraws()).sum());
    }

    @Test
    void aShortfallIsSharedAcrossConsumers() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{100}, NONE, NONE, new long[]{90, 90});
        assertArrayEquals(new long[]{50, 50}, s.consumerGrants());
    }

    @Test
    void whatIsDrawnEqualsWhatIsDelivered() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{70, 30}, new long[]{40, 40}, new long[]{0, 0}, new long[]{90, 45, 20});
        long in = Arrays.stream(s.generatorDraws()).sum()
                + Arrays.stream(s.accumulatorDischarges()).sum();
        long out = Arrays.stream(s.consumerGrants()).sum()
                + Arrays.stream(s.accumulatorCharges()).sum();
        assertEquals(in, out);
        assertEquals(155, out);
    }

    // ---- The two-level balance (ADR 0008): Districts settle first, then the Electric Network ----

    private static NetworkBalance.District district(long[] gen, long[] accOffers, long[] rooms,
                                                    long[] demands) {
        return new NetworkBalance.District(gen, accOffers, rooms, demands);
    }

    @Test
    void aSelfSufficientDistrictStaysFedWhileAnotherIsShort() {
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(new long[]{100}, NONE, NONE, new long[]{60}),
                district(new long[]{0}, NONE, NONE, new long[]{80}));
        assertArrayEquals(new long[]{60}, s[0].consumerGrants());
        assertArrayEquals(new long[]{40}, s[1].consumerGrants());
        assertArrayEquals(new long[]{100}, s[0].generatorDraws());
    }

    @Test
    void accumulatorsExportOnlyOnceEveryDistrictsGeneratorSurplusIsSpent() {
        // District 1 is short by 100. District 0's generators give 30 and its accumulator 500;
        // District 2's accumulator has 500 too. The 30 of surplus goes first, accumulators cover 70.
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(new long[]{30}, new long[]{500}, new long[]{0}, NONE),
                district(NONE, NONE, NONE, new long[]{100}),
                district(NONE, new long[]{500}, new long[]{0}, NONE));
        assertArrayEquals(new long[]{100}, s[1].consumerGrants());
        assertArrayEquals(new long[]{30}, s[0].generatorDraws());
        long exported = s[0].accumulatorDischarges()[0] + s[2].accumulatorDischarges()[0];
        assertEquals(70, exported);
        assertArrayEquals(new long[]{35}, s[0].accumulatorDischarges());
    }

    @Test
    void accumulatorsDoNotExportWhileGeneratorSurplusCoversTheShortfall() {
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(new long[]{100}, NONE, NONE, NONE),
                district(NONE, NONE, NONE, new long[]{100}),
                district(NONE, new long[]{500}, new long[]{0}, NONE));
        assertArrayEquals(new long[]{100}, s[1].consumerGrants());
        assertArrayEquals(new long[]{0}, s[2].accumulatorDischarges());
    }

    @Test
    void generatorSurplusChargesAccumulatorsInOtherDistrictsOnceDemandIsMet() {
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(new long[]{100}, NONE, NONE, new long[]{10}),
                district(NONE, new long[]{0}, new long[]{60}, NONE));
        assertArrayEquals(new long[]{60}, s[1].accumulatorCharges());
        assertArrayEquals(new long[]{70}, s[0].generatorDraws());
    }

    @Test
    void networkChargingIsSharedInProportionToEachDistrictsRoom() {
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(new long[]{80}, NONE, NONE, NONE),
                district(NONE, new long[]{0}, new long[]{100, 100}, NONE),
                district(NONE, new long[]{0}, new long[]{200}, NONE));
        assertArrayEquals(new long[]{20, 20}, s[1].accumulatorCharges());
        assertArrayEquals(new long[]{40}, s[2].accumulatorCharges());
    }

    @Test
    void accumulatorsNeverChargeAccumulatorsAcrossDistricts() {
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(NONE, new long[]{150}, new long[]{0}, NONE),
                district(NONE, new long[]{0}, new long[]{150}, NONE));
        assertArrayEquals(new long[]{0}, s[0].accumulatorDischarges());
        assertArrayEquals(new long[]{0}, s[1].accumulatorCharges());
    }

    @Test
    void aShortfallAcrossDistrictsIsSharedInProportionToEachDistrictsShortfall() {
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(new long[]{100}, NONE, NONE, NONE),
                district(NONE, NONE, NONE, new long[]{300}),
                district(NONE, NONE, NONE, new long[]{100}));
        assertArrayEquals(new long[]{75}, s[1].consumerGrants());
        assertArrayEquals(new long[]{25}, s[2].consumerGrants());
    }

    @Test
    void whatAnImportingDistrictTakesJoinsItsOwnSupplyInOneWaterFill() {
        // The District makes 10 itself and imports 40. Machines ask 40 and 40: 25 each, not a
        // local 5/5 topped up afterwards.
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(new long[]{10}, NONE, NONE, new long[]{40, 40}),
                district(new long[]{40}, NONE, NONE, NONE));
        assertArrayEquals(new long[]{25, 25}, s[0].consumerGrants());
    }

    @Test
    void aDistrictChargesNothingWhileAnotherDistrictIsShort() {
        // A makes 1000 with nothing to feed and room for 1000. B asks 1000. B is fed first.
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(new long[]{1000}, new long[]{0}, new long[]{1000}, NONE),
                district(NONE, NONE, NONE, new long[]{1000}));
        assertArrayEquals(new long[]{1000}, s[1].consumerGrants());
        assertArrayEquals(new long[]{0}, s[0].accumulatorCharges());
        assertArrayEquals(new long[]{1000}, s[0].generatorDraws());
    }

    @Test
    void surplusChargesADistrictsOwnRoomBeforeAnotherDistrictsRoom() {
        // Both Districts make 50 and need nothing. Each has room for 100: each charges its own 50.
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(new long[]{50}, new long[]{0}, new long[]{100}, NONE),
                district(new long[]{50}, new long[]{0}, new long[]{100}, NONE));
        assertArrayEquals(new long[]{50}, s[0].accumulatorCharges());
        assertArrayEquals(new long[]{50}, s[1].accumulatorCharges());
        // A District with more surplus than room spills the rest into the other's room.
        s = NetworkBalance.settleNetwork(
                district(new long[]{150}, new long[]{0}, new long[]{100}, NONE),
                district(NONE, new long[]{0}, new long[]{100}, NONE));
        assertArrayEquals(new long[]{100}, s[0].accumulatorCharges());
        assertArrayEquals(new long[]{50}, s[1].accumulatorCharges());
    }

    @Test
    void aNetworkOfOneDistrictSettlesAsTheFlatBalanceDoes() {
        long[][][] cases = {
                {{450}, {150}, {0}, {90}},
                {{50}, {150}, {0}, {90}},
                {{450}, {0}, {150}, {90}},
                {NONE, {150, 0}, {0, 150}, NONE},
                {{300, 100}, NONE, NONE, {200}},
                {{100, 100, 100}, NONE, NONE, {100}},
                {{100}, NONE, NONE, {90, 90}},
                {{70, 30}, {40, 40}, {0, 0}, {90, 45, 20}},
                {{70, 30}, {40, 40}, {25, 25, 25}, {10, 5}},
        };
        for (long[][] c : cases) {
            NetworkBalance.Settlement flat = NetworkBalance.settle(c[0], c[1], c[2], c[3]);
            NetworkBalance.Settlement net =
                    NetworkBalance.settleNetwork(district(c[0], c[1], c[2], c[3]))[0];
            assertArrayEquals(flat.generatorDraws(), net.generatorDraws());
            assertArrayEquals(flat.accumulatorDischarges(), net.accumulatorDischarges());
            assertArrayEquals(flat.accumulatorCharges(), net.accumulatorCharges());
            assertArrayEquals(flat.consumerGrants(), net.consumerGrants());
        }
    }

    @Test
    void energyIsConservedToTheFeOnEveryNetworkCase() {
        java.util.Random random = new java.util.Random(12);
        for (int round = 0; round < 500; round++) {
            NetworkBalance.District[] districts = new NetworkBalance.District[1 + random.nextInt(4)];
            for (int i = 0; i < districts.length; i++) {
                districts[i] = district(randoms(random), randoms(random), randoms(random),
                        randoms(random));
            }
            NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(districts);
            long in = 0;
            long out = 0;
            for (int i = 0; i < districts.length; i++) {
                in += Arrays.stream(s[i].generatorDraws()).sum()
                        + Arrays.stream(s[i].accumulatorDischarges()).sum();
                out += Arrays.stream(s[i].consumerGrants()).sum()
                        + Arrays.stream(s[i].accumulatorCharges()).sum();
                assertWithin(districts[i].generatorOffers(), s[i].generatorDraws());
                assertWithin(districts[i].accumulatorOffers(), s[i].accumulatorDischarges());
                assertWithin(districts[i].accumulatorRooms(), s[i].accumulatorCharges());
                assertWithin(districts[i].consumerDemands(), s[i].consumerGrants());
            }
            assertEquals(in, out, "round " + round);
        }
    }

    private static long[] randoms(java.util.Random random) {
        long[] values = new long[random.nextInt(4)];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextInt(4) == 0 ? 0 : random.nextInt(500);
        }
        return values;
    }

    private static void assertWithin(long[] limits, long[] flows) {
        assertEquals(limits.length, flows.length);
        for (int i = 0; i < limits.length; i++) {
            org.junit.jupiter.api.Assertions.assertTrue(flows[i] >= 0 && flows[i] <= limits[i]);
        }
    }

    @Test
    void oneDistrictsGeneratorSurplusFeedsAnothersShortfall() {
        NetworkBalance.Settlement[] s = NetworkBalance.settleNetwork(
                district(new long[]{100}, NONE, NONE, new long[]{20}),
                district(new long[]{30}, NONE, NONE, new long[]{90}));
        assertArrayEquals(new long[]{90}, s[1].consumerGrants());
        assertArrayEquals(new long[]{80}, s[0].generatorDraws());
        assertArrayEquals(new long[]{30}, s[1].generatorDraws());
    }
}
