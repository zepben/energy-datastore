/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.blobstore.itemwrappers.ByDateItemHandler;
import com.zepben.blobstore.itemwrappers.ByDateItemReader;
import com.zepben.energy.datastore.ErrorHandler;
import com.zepben.energy.datastore.ItemHandler;
import com.zepben.energy.model.Channel;
import com.zepben.energy.model.EnergyProfile;
import com.zepben.energy.model.EnergyProfileStat;
import com.zepben.energy.model.Readings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EnergyProfileStatReaderTest {

    @Mock private ErrorHandler onError;
    @Mock private ByDateItemReader<EnergyProfile> profileReader;
    @Mock private ByDatePartialProfileReader<EnergyProfileStat> partialReader;
    private EnergyProfileStatReader statReader;

    private String id1 = "id1";
    private String id2 = "id2";
    private LocalDate date = LocalDate.now();

    private EnergyProfile profile1 = EnergyProfile.of(
        id1,
        date,
        Readings.of(Channel.of(4, 6, 5)),
        Readings.of(Channel.of(1, 2, 3)));

    private EnergyProfile profile2 = EnergyProfile.of(
        id2,
        date,
        Readings.of(Channel.of(1, 2, 3)),
        Readings.of(Channel.of(6, 5, 4)));

    private EnergyProfileStat expectedStat1 = EnergyProfileStat.ofMax(profile1);
    private EnergyProfileStat expectedStat2 = EnergyProfileStat.ofMax(profile2);

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        doReturn(profileReader).when(partialReader).itemReader();
        statReader = new EnergyProfileStatReader(partialReader, profile -> {
            if (profile == profile1)
                return expectedStat1;
            else if (profile == profile2)
                return expectedStat2;
            else
                throw new AssertionError();
        });
    }

    @Test
    public void getExistingStat() throws Exception {
        doReturn(expectedStat1).when(partialReader).get(id1, date, onError);
        EnergyProfileStat stat = statReader.get(id1, date, onError);
        assertThat(stat, equalTo(expectedStat1));
        verify(partialReader).get(id1, date, onError);
    }

    @Test
    public void getCreatingStat() throws Exception {
        doReturn(profile1).when(profileReader).get(eq(id1), eq(date), any());
        EnergyProfileStat stat = statReader.get(id1, date, onError);
        assertThat(stat, equalTo(expectedStat1));
    }

    @Test
    public void forEach() throws Exception {
        List<String> ids = Arrays.asList(id1, id2);
        doAnswer(inv -> {
            ItemHandler<EnergyProfileStat> handler = inv.getArgument(2);
            handler.handle(id1, date, expectedStat1);
            return null;
        }).when(partialReader).forEach(eq(ids), eq(date), any(), any());

        doAnswer(inv -> {
            ByDateItemHandler<EnergyProfile> hander = inv.getArgument(2);
            hander.handle(id2, date, profile2);
            return null;
        }).when(profileReader).forEach(eq(Collections.singleton(id2)), eq(date), any(), any());

        List<EnergyProfileStat> stats = new ArrayList<>();
        statReader.forEach(ids, date, (id, dt, stat) -> stats.add(stat), onError);

        assertThat(stats, contains(expectedStat1, expectedStat2));
    }

    @Test
    public void forAll() throws Exception {
        doAnswer(inv -> {
            ByDateItemHandler<EnergyProfile> hander = inv.getArgument(1);
            hander.handle(id1, date, profile1);
            return null;
        }).when(profileReader).forAll(eq(date), any(), any());

        List<EnergyProfileStat> stats = new ArrayList<>();
        statReader.forAll(date, (id, dt, stat) -> stats.add(stat), onError);

        assertThat(stats, contains(expectedStat1));
    }

}