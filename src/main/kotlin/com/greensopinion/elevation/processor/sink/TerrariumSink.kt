package com.greensopinion.elevation.processor.sink

import com.greensopinion.elevation.processor.Elevation
import com.greensopinion.elevation.processor.INVALID_ELEVATION
import com.greensopinion.elevation.processor.Tile
import com.greensopinion.elevation.processor.TileSink
import com.greensopinion.elevation.processor.elevation.ElevationDataStore
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class TerrariumSink(
    val extent: Int,
    val outputFolder: File,
    val elevationDataStore: ElevationDataStore,
    private val log: KLogger = KotlinLogging.logger {}
) : TileSink {
    override fun accept(tile: Tile) {
        val elevationTile = elevationDataStore.get(tile.id)
        val file = File(outputFolder, "${tile.id.z}/${tile.id.x}/${tile.id.y}.png")
        file.parentFile.mkdirs()
        val image = BufferedImage(extent, extent, BufferedImage.TYPE_INT_RGB)
        for (x in 0..<extent) {
            for (y in 0..<extent) {
                if (x == 255 && y == 255) {
                    log.info { "elevation at $x,$y ${tile.id}=${elevationTile.get(x, y)}" }
                }
                image.setRGB(x, y, encodeTerrarium(elevationTile.get(x, y)))
            }
        }
        log.info { "writing $file" }
        ImageIO.write(image, "png", file)
    }

    private fun encodeTerrarium(elevation: Elevation): Int {
        if (elevation == INVALID_ELEVATION) {
            return 0
        }
        val shiftedElevation = (elevation.meters + 32768.0)
        val red = (shiftedElevation / 256).toInt()
        val green = (shiftedElevation % 256).toInt()
        val blue = ((shiftedElevation - shiftedElevation.toInt()) * 256).toInt()

        return ((red and 0xFF) shl 16) or
                ((green and 0xFF) shl 8) or
                (blue and 0xFF)
    }
}