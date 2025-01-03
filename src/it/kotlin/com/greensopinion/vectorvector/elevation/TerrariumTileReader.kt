package com.greensopinion.vectorvector.elevation

import com.greensopinion.vectorvector.Elevation
import com.greensopinion.vectorvector.ElevationTile
import java.io.File
import javax.imageio.ImageIO

class TerrariumTileReader {
    fun read(file: File): ElevationTile {
        val image = ImageIO.read(file)
        return object : ElevationTile() {
            override val extent: Int = image.width
            override val empty = false

            override fun get(x: Int, y: Int): Elevation {
                require(x < image.width && y < image.height) { "$x,$y is not in ${image.width},${image.height}" }
                val rgb = image.getRGB(x, y)
                val r = ((rgb shr 16) and 0xFF)
                val g = ((rgb shr 8) and 0xFF)
                val b = rgb and 0xFF

                return Elevation(meters = (((r.toDouble() * 256) + g.toDouble() + (b/256.0)) - 32768.0).toDouble())
            }
        }
    }
}