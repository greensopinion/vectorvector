package com.greensopinion.elevation.processor

import com.greensopinion.elevation.processor.elevation.*
import com.greensopinion.elevation.processor.metrics.DefaultMetrics
import com.greensopinion.elevation.processor.metrics.PeriodicMetrics
import com.greensopinion.elevation.processor.metrics.SingletonMetricsProvider
import com.greensopinion.elevation.processor.sink.CompositeTileSink
import com.greensopinion.elevation.processor.sink.FilesystemTileRepository
import com.greensopinion.elevation.processor.sink.TerrariumSink
import com.greensopinion.elevation.processor.sink.VectorTileSink
import io.github.oshai.kotlinlogging.KotlinLogging
import picocli.CommandLine
import vector_tile.VectorTile
import java.time.Duration

private val log = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val options = parseCommandLine(args)
    if (options.validateData) {
        validateData(options)
        return
    }
    val tileExtent = 256
    val metricsProvider = SingletonMetricsProvider()
    val dataStore = BlockElevationDataStore(
        blockSize = Degrees(5.0),
        blockExtent = 6000,
        tileExtent = tileExtent,
        blockStore = CachingBlockStore(FilesystemBlockStore(folder = options.dataDir!!), metricsProvider)
    )
    val repository = FilesystemTileRepository(outputFolder = options.outputDir!!)
    PeriodicMetrics(
        interval = Duration.ofSeconds(30),
        metrics = metricsProvider.metrics
    ).use {
        Processor(
            tileRange = options.toTileRange(),
            metricsProvider = metricsProvider,
            sink = CompositeTileSink(
                listOf(
                    TerrariumSink(
                        extent = tileExtent,
                        repository = repository,
                        elevationDataStore = dataStore,
                        metricsProvider = metricsProvider
                    ),
                    VectorTileSink(
                        repository = repository,
                        elevationDataStore = dataStore,
                        metricsProvider = metricsProvider
                    )
                )
            )
        ).process()
    }
}

private fun validateData(options: CliOptions) {
    log.info { "Validating data only" }
    val blockStore = FilesystemBlockStore(
        folder = options.dataDir!!
    )
    blockStore.validateAll()
    log.info { "Done" }
}

private fun parseCommandLine(args: Array<String>): CliOptions {
    val options = CliOptions();
    CommandLine(options).parseArgs(*args)
    return options
}

private fun CliOptions.toTileRange() = TileRange(
    minZ = this.minZ,
    maxZ = this.maxZ,
    minX = this.minX,
    maxX = this.maxX,
    minY = this.minY,
    maxY = this.maxY,
)