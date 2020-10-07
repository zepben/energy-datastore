/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.model;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class Matchers {

    public static Matcher<DoubleArrayView> hasEqualValues(DoubleArrayView channel) {
        return new TypeSafeMatcher<DoubleArrayView>() {
            @Override
            protected boolean matchesSafely(DoubleArrayView item) {
                return item.valuesEqual(channel);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has values " + channel.toString());
            }
        };
    }

    public static Matcher<Readings> hasEqualChannels(Readings readings) {
        return new TypeSafeMatcher<Readings>() {
            @Override
            protected boolean matchesSafely(Readings item) {
                if (item.numChannels() != readings.numChannels())
                    return false;

                for (int i = 1; i <= item.numChannels(); ++i) {
                    if (!item.channel(i).valuesEqual(readings.channel(i)))
                        return false;
                }

                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has values " + readings.toString());
            }
        };
    }

}
