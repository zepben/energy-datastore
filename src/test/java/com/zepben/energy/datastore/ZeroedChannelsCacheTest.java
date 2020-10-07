/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore;

import com.zepben.energy.model.Channel;
import com.zepben.energy.model.ZeroedChannelsCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;


public class ZeroedChannelsCacheTest {

    @Test
    public void returnsCachedInstance() {
        Channel r1 = ZeroedChannelsCache.of(0);
        Channel r2 = ZeroedChannelsCache.of(0);
        assertEquals(r1.length(), 0);
        assertSame(r1, r2);

        r1 = ZeroedChannelsCache.of(48);
        r2 = ZeroedChannelsCache.of(48);
        assertEquals(r1.length(), 48);
        assertSame(r1, r2);

        r1 = ZeroedChannelsCache.of(96);
        r2 = ZeroedChannelsCache.of(96);
        assertEquals(r1.length(), 96);
        assertSame(r1, r2);

        r1 = ZeroedChannelsCache.of(5);
        r2 = ZeroedChannelsCache.of(5);
        assertEquals(r1.length(), 5);
        assertSame(r1, r2);
    }

}