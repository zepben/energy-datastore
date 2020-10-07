/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import java.util.function.DoubleBinaryOperator;

@EverythingIsNonnullByDefault
public class EnergyProfileStat {

    private final double kwIn;
    private final double kwOut;
    private final double kwNet;

    public EnergyProfileStat(double kwIn, double kwOut, double kwNet) {
        this.kwIn = kwIn;
        this.kwOut = kwOut;
        this.kwNet = kwNet;
    }

    public static EnergyProfileStat ofMax(EnergyProfile profile) {
        return accumulating(profile, Math::max, (l, v) -> v, 0, 0, Double.NEGATIVE_INFINITY);
    }

    @SuppressWarnings("WeakerAccess")
    public static EnergyProfileStat ofMin(EnergyProfile profile) {
        double inf = Double.POSITIVE_INFINITY;
        return accumulating(profile, Math::min, (l, v) -> v, inf, inf, inf);
    }

    @SuppressWarnings("WeakerAccess")
    public static EnergyProfileStat ofAvg(EnergyProfile profile) {
        return accumulating(profile, (l, r) -> l + r, (l, v) -> v / l, 0, 0, 0);
    }

    public double kwIn() {
        return kwIn;
    }

    public double kwOut() {
        return kwOut;
    }

    public double kwNet() {
        return kwNet;
    }

    @EverythingIsNonnullByDefault
    private interface AccumulatingFinalizer {

        double finalize(int count, double value);

    }

    private static EnergyProfileStat accumulating(EnergyProfile profile,
                                                  DoubleBinaryOperator accumulator,
                                                  AccumulatingFinalizer finalizer,
                                                  double initKwIn,
                                                  double initKwOut,
                                                  double initKwNet) {
        int len = profile.kwIn().length();
        if (len == 0)
            throw new IllegalArgumentException("profile must have readings with a length");

        double kwIn = initKwIn;
        double kwOut = initKwOut;
        double kwNet = initKwNet;
        for (int i = 0; i < len; ++i) {
            kwIn = accumulator.applyAsDouble(kwIn, profile.kwIn().get(i));
            kwOut = accumulator.applyAsDouble(kwOut, profile.kwOut().get(i));
            kwNet = accumulator.applyAsDouble(kwNet, profile.kwIn().get(i) - profile.kwOut().get(i));
        }

        return new EnergyProfileStat(
            finalizer.finalize(len, kwIn),
            finalizer.finalize(len, kwOut),
            finalizer.finalize(len, kwNet));
    }

}
