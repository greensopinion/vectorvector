package com.greensopinion.elevation.processor.elevation

import com.greensopinion.elevation.processor.ElevationTile
import com.greensopinion.elevation.processor.TileId
import com.greensopinion.elevation.processor.metrics.SingletonMetricsProvider
import java.io.File

val testBlockStore = CachingBlockStore(
    FilesystemBlockStore(
        blockExtent = 6000,
        folder = File("../../data/tif")
    ),
    metricsProvider = SingletonMetricsProvider()
)

fun referenceTerrariumTile(tileId: TileId): ElevationTile =
    TerrariumTileReader().read(File("src/it/resources/terrarium_${tileId.z}_${tileId.x}_${tileId.y}.png"))