/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zepben.energy.datastore.blobstore;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.blobstore.itemwrappers.ByDateItemWriter;
import com.zepben.blobstore.itemwrappers.ItemBlobWriter;
import com.zepben.energy.datastore.EnergyProfileWriter;
import com.zepben.energy.datastore.ErrorHandler;
import com.zepben.energy.datastore.blobstore.indexing.DateRangeIndex;
import com.zepben.energy.model.EnergyProfile;
import com.zepben.energy.model.EnergyProfileStat;
import com.zepben.energy.model.MissingReadings;
import com.zepben.energy.model.Readings;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import java.time.LocalDate;

import static com.zepben.energy.datastore.blobstore.EnergyProfileAttribute.*;

@EverythingIsNonnullByDefault
public class ByDateBlobEnergyProfileWriter implements EnergyProfileWriter {

    private final Serialisers serialisers;
    private final DateRangeIndex dateRangeIndex;
    private final ByDateItemWriter itemWriter;

    public ByDateBlobEnergyProfileWriter(DateRangeIndex dateRangeIndex,
                                         ByDateItemWriter itemWriter,
                                         Serialisers serialisers) {
        this.dateRangeIndex = dateRangeIndex;
        this.itemWriter = itemWriter;
        this.serialisers = serialisers;
    }

    @Override
    public boolean write(EnergyProfile profile, boolean writeStats, ErrorHandler onError) {
        if (!itemWriter.write(profile.id(), profile.date(), profile, getProfileWriteHandler(writeStats), onError::handle))
            return false;

        updateIndex(profile.id(), profile.date(), onError);
        return true;
    }

    @Override
    public boolean commit(ErrorHandler onError) {
        if (!itemWriter.commit(onError::handle))
            return false;

        dateRangeIndex.commit();
        return true;
    }

    @Override
    public boolean rollback(ErrorHandler onError) {
        if (!itemWriter.rollback(onError::handle))
            return false;

        dateRangeIndex.rollback();
        return true;
    }

    @Override
    public boolean writeKwIn(String id,
                             LocalDate date,
                             Readings readings,
                             ErrorHandler onError) {
        if (!itemWriter.write(id, date, readings, getReadingsWriteHandler(KW_IN, serialisers.kwInSx()), onError::handle))
            return false;

        updateIndex(id, date, onError);
        return true;
    }

    @Override
    public boolean writeKwOut(String id,
                              LocalDate date,
                              Readings readings,
                              ErrorHandler onError) {
        if (!itemWriter.write(id, date, readings, getReadingsWriteHandler(KW_OUT, serialisers.kwOutSx()), onError::handle))
            return false;

        updateIndex(id, date, onError);
        return true;
    }

    @Override
    public boolean writeCacheable(String id,
                                  LocalDate date,
                                  boolean cacheable,
                                  ErrorHandler onError) {
        if (!itemWriter.write(id, date, cacheable, this::writeCacheable, onError::handle))
            return false;

        updateIndex(id, date, onError);
        return true;
    }

    private Function2<ItemBlobWriter, EnergyProfile, Unit> getProfileWriteHandler(boolean writeStats) {
        return (writer, profile) -> {
            writeReadings(writer, KW_IN, profile.kwIn(), serialisers.kwInSx(), false);
            writeReadings(writer, KW_OUT, profile.kwOut(), serialisers.kwOutSx(), false);
            writeCacheable(writer, profile.cacheable());

            if (writeStats && !writer.anyFailed()) {
                writeStats(writer, profile);
            } else {
                deleteStats(writer);
            }
            return Unit.INSTANCE;
        };
    }

    private Function2<ItemBlobWriter, Readings, Unit> getReadingsWriteHandler(EnergyProfileAttribute tag,
                                                                              Serialiser<Readings> sx) {
        return (writer, readings) -> {
            writeReadings(writer, tag, readings, sx, true);
            return Unit.INSTANCE;
        };
    }

    private void writeReadings(ItemBlobWriter writer,
                               EnergyProfileAttribute tag,
                               Readings readings,
                               Serialiser<Readings> readingsSx,
                               boolean deleteStats) {
        if (readings instanceof MissingReadings) {
            writer.delete(tag.storeString());
        } else {
            writer.write(tag.storeString(), readingsSx.sx(readings), readingsSx.sxOffset(), readingsSx.sxLength());
        }

        if (deleteStats)
            deleteStats(writer);
    }

    private Unit writeCacheable(ItemBlobWriter writer, boolean cacheable) {
        if (cacheable) {
            Serialiser<Boolean> sx = serialisers.cacheableSx();
            writer.write(CACHEABLE.storeString(), sx.sx(true), sx.sxOffset(), sx.sxLength());
        } else {
            writer.delete(CACHEABLE.storeString());
        }
        return Unit.INSTANCE;
    }

    private void writeStats(ItemBlobWriter writer, EnergyProfile profile) {
        Serialiser<EnergyProfileStat> sx = serialisers.statSx();
        writer.write(MAXIMUMS.storeString(), sx.sx(EnergyProfileStat.ofMax(profile)), sx.sxOffset(), sx.sxLength());
    }

    private void deleteStats(ItemBlobWriter writer) {
        writer.delete(MAXIMUMS.storeString());
    }

    private void updateIndex(String id, LocalDate date, ErrorHandler onError) {
        if (!dateRangeIndex.extendRange(id, date))
            onError.handle(id, date, "Unable to extend date range in index", null);
    }

}
