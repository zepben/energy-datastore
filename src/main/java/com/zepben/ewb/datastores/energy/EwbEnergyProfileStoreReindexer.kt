/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.zepben.ewb.datastores.energy

import com.zepben.annotations.EverythingIsNonnullByDefault
import com.zepben.blobstore.BlobStoreException
import com.zepben.blobstore.itemwrappers.ByDateBlobReaderProvider
import com.zepben.energy.datastore.blobstore.indexing.BlobDateRangeIndex
import com.zepben.ewb.datastores.energy.EwbEnergyProfileStoreReindexer
import com.zepben.ewb.filepaths.EwbDataFilePaths
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import java.util.stream.Collectors

@EverythingIsNonnullByDefault
class EwbEnergyProfileStoreReindexer internal constructor(
    private val ewbPaths: EwbDataFilePaths,
    private val timeZone: ZoneId,
    private val byDateStoreProvider: ByDateBlobReaderProvider,
    private val indexSupplier: Supplier<BlobDateRangeIndex>,
    private val progressFactory: Progress.Factory,
) {

    private val logger: Logger = LoggerFactory.getLogger(EwbEnergyProfileStoreReindexer::class.java)

    @Throws(BlobStoreException::class)
    fun reindex() {
        val dates = availableDates
        val index = buildIndex(dates)
        writeIndex(index)
    }

    @Throws(BlobStoreException::class)
    private fun buildIndex(dates: List<LocalDate>): Map<String, Range>
    {
        val index: MutableMap<String, Range> = ConcurrentHashMap()
        val progress = progressFactory.create("Building index", dates.size)
        runBlocking { // this: CoroutineScope
            for (date in dates) {
                launch { // launch a new coroutine and continue
                    logger.info("Indexing {}", date)
                    byDateStoreProvider.getReader(date, timeZone).use { readingsStore ->
                        readingsStore?.ids { id: String ->
                            val range = index.computeIfAbsent(id) { i: String? -> Range(i, date, date) }
                            if (date.isBefore(range.from)) range.from = date
                            if (date.isAfter(range.to)) range.to = date
                        }
                    }
                }
                progress.step()
            }
        }
        return index
    }

    @Throws(BlobStoreException::class)
    private fun writeIndex(index: Map<String, Range>) {
        val progress = progressFactory.create("Saving index", index.size)
        val backupPath = Paths.get(ewbPaths.energyReadingsIndex().toString() + ".bak")
        backupIndex(backupPath)
        var status = true
        indexSupplier.get().use { indexStore ->
            for (range in index.values) {
                status = status and indexStore.save(range.id, range.from, range.to)
                progress.step()
            }
            status = status and indexStore.commit()
        }
        if (!status) {
            restoreIndex(backupPath)
            throw BlobStoreException("Failed to write to index database.", null)
        }
        deleteBackup(backupPath)
    }

    @get:Throws(BlobStoreException::class)
    private val availableDates: List<LocalDate>
        private get() {
            try {
                Files.walk(ewbPaths.baseDir(), 1).use { files ->
                    return files
                        .map { file: Path ->
                            try {
                                val dateStr = file.fileName.toString()
                                return@map LocalDate.parse(dateStr)
                            } catch (ex: DateTimeParseException) {
                                return@map null
                            }
                        }
                        .filter { obj: LocalDate? -> Objects.nonNull(obj) }
                        .sorted()
                        .collect(Collectors.toList()) as List<LocalDate>
                }
            } catch (ex: IOException) {
                throw BlobStoreException("Failed to read directory listing from disk", ex)
            }
        }

    @Throws(BlobStoreException::class)
    private fun backupIndex(backupPath: Path) {
        try {
            if (Files.exists(ewbPaths.energyReadingsIndex())) Files.move(ewbPaths.energyReadingsIndex(), backupPath)
        } catch (e: IOException) {
            throw BlobStoreException("Failed to backup index file", e)
        }
    }

    @Throws(BlobStoreException::class)
    private fun deleteBackup(backupPath: Path) {
        try {
            Files.deleteIfExists(backupPath)
        } catch (e: IOException) {
            throw BlobStoreException("Failed to delete backup file", e)
        }
    }

    @Throws(BlobStoreException::class)
    private fun restoreIndex(backupPath: Path) {
        try {
            Files.deleteIfExists(ewbPaths.energyReadingsIndex())
            if (Files.exists(backupPath)) Files.move(backupPath, ewbPaths.energyReadingsIndex())
        } catch (ex: IOException) {
            throw BlobStoreException(
                "Failed to restore index. You need to maually restore from backup file: $backupPath",
                ex
            )
        }
    }

    @EverythingIsNonnullByDefault
    private class Range(
        val id: String?,
        var from: LocalDate,
        var to: LocalDate
    )

    companion object {
        @JvmStatic
        fun create(
            ewbPaths: EwbDataFilePaths,
            timeZone: ZoneId,
            progressFactory: Progress.Factory
        ): EwbEnergyProfileStoreReindexer {
            return EwbEnergyProfileStoreReindexer(
                ewbPaths,
                timeZone,
                SqliteEwbEnergyProfileStore.createByDateBlobStoreCache(ewbPaths),
                { SqliteEwbEnergyProfileStore.createEnergyProfileIndex(ewbPaths) },
                progressFactory
            )
        }
    }
}