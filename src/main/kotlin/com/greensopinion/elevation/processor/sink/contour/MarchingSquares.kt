package com.greensopinion.elevation.processor.sink.contour
import com.greensopinion.elevation.processor.Elevation
import com.greensopinion.elevation.processor.ElevationTile
import kotlin.math.abs

class MarchingSquares(
    private val tile: ElevationTile,
    extent: Int,
    private val buffer: Int
) {
    private val factor: Double = extent.toDouble() / tile.extent

    fun generateIsolines(interval: Int): Map<Int, List<List<DoublePoint>>> {
        val numRows = tile.extent
        val numCols = tile.extent
        val contourLevels = contourLevels(interval)
        val isolinesByContourLevel = mutableMapOf<Int, List<List<DoublePoint>>>()

        for (contourLevel in contourLevels) {
            val endToContour = mutableMapOf<DoublePoint, MutableList<DoublePoint>>()
            val rings = mutableListOf<List<DoublePoint>>()

            for (y in -buffer until numRows - 1 + buffer) {
                for (x in -buffer until numCols - 1 + buffer) {
                    val bottomLeft = tile.get(x, y)
                    val bottomRight = tile.get(x + 1, y)
                    val topRight = tile.get(x + 1, y + 1)
                    val topLeft = tile.get(x, y + 1)

                    if (!bottomLeft.valid || !bottomRight.valid || !topRight.valid || !topLeft.valid) continue

                    var caseIndex = 0
                    if (bottomLeft > contourLevel) caseIndex = caseIndex or 1
                    if (bottomRight > contourLevel) caseIndex = caseIndex or 2
                    if (topRight > contourLevel) caseIndex = caseIndex or 4
                    if (topLeft > contourLevel) caseIndex = caseIndex or 8

                    val contour = handleCase(x, y, bottomLeft, bottomRight, topRight, topLeft, contourLevel, caseIndex)
                    if (contour.isNotEmpty()) {
                        var firstContour = findContour(endToContour, contour.first())
                        var lastContour = findContour(endToContour, contour.last())

                        when {
                            firstContour != null -> {
                                if (lastContour != null && firstContour == lastContour) {
                                    if (!close(contour.first(), contour.last())) {
                                        // connect a ring and remove it for future joins
                                        endToContour.remove(firstContour.first())
                                        endToContour.remove(firstContour.last())
                                        endToContour.remove(lastContour.first())
                                        endToContour.remove(lastContour.last())
                                        firstContour.add(firstContour.first())
                                        rings.add(firstContour)
                                    }
                                } else if (lastContour != null) {
                                    // join contours
                                    endToContour.remove(firstContour.first())
                                    endToContour.remove(firstContour.last())
                                    endToContour.remove(lastContour.first())
                                    endToContour.remove(lastContour.last())
                                    if (close(firstContour.first(), contour.first())) {
                                        firstContour = firstContour.asReversed()
                                    }
                                    if (close(lastContour.last(), contour.last())) {
                                        lastContour = lastContour.asReversed()
                                    }
                                    val newContour = (firstContour + lastContour).toMutableList()
                                    endToContour[newContour.first()] = newContour
                                    endToContour[newContour.last()] = newContour
                                } else {
                                    // extend the contour
                                    endToContour.remove(firstContour.first())
                                    endToContour.remove(firstContour.last())
                                    if (close(firstContour.first(), contour.first())) {
                                        firstContour.add(0, contour.last())
                                    } else {
                                        firstContour.add(contour.last())
                                    }
                                    endToContour[firstContour.first()] = firstContour
                                    endToContour[firstContour.last()] = firstContour
                                }
                            }
                            lastContour != null -> {
                                // extend the contour
                                endToContour.remove(lastContour.first())
                                endToContour.remove(lastContour.last())
                                if (close(lastContour.first(), contour.last())) {
                                    lastContour.add(0, contour.first())
                                } else {
                                    lastContour.add(contour.first())
                                }
                                endToContour[lastContour.first()] = lastContour
                                endToContour[lastContour.last()] = lastContour
                            }
                            else -> {
                                endToContour[contour.first()] = contour.toMutableList()
                                endToContour[contour.last()] = contour.toMutableList()
                            }
                        }
                    }
                }
            }

            if (endToContour.isNotEmpty() || rings.isNotEmpty()) {
                isolinesByContourLevel[contourLevel] = endToContour.values.toSet().toList() + rings
            }
        }
        return isolinesByContourLevel
    }

    private fun handleCase(
        x: Int,
        y: Int,
        bottomLeft: Elevation,
        bottomRight: Elevation,
        topRight: Elevation,
        topLeft: Elevation,
        contourLevel: Int,
        caseIndex: Int
    ): List<DoublePoint> {
        val contour = mutableListOf<DoublePoint>()

        fun interpolate(p1: DoublePoint, p2: DoublePoint, v1: Elevation, v2: Elevation): DoublePoint {
            return if (v1 == v2) {
                ((p1 + p2) / 2.0).round()
            } else {
                val t = (contourLevel - v1.meters).toDouble() / (v2 - v1).meters
                val intersection = DoublePoint(
                    p1.x + t * (p2.x - p1.x),
                    p1.y + t * (p2.y - p1.y)
                )
                (intersection * factor).round()
            }
        }

        val pointBottomLeft = DoublePoint(x.toDouble(), y.toDouble())
        val pointBottomRight = DoublePoint((x + 1).toDouble(), y.toDouble())
        val pointTopRight = DoublePoint((x + 1).toDouble(), (y + 1).toDouble())
        val pointTopLeft = DoublePoint(x.toDouble(), (y + 1).toDouble())

        when (caseIndex) {
            0, 15 -> {} // completely inside or outside
            1 -> {
                contour.add(interpolate(pointBottomLeft, pointBottomRight, bottomLeft, bottomRight))
                contour.add(interpolate(pointBottomLeft, pointTopLeft, bottomLeft, topLeft))
            }
            2 -> {
                contour.add(interpolate(pointBottomLeft, pointBottomRight, bottomLeft, bottomRight))
                contour.add(interpolate(pointBottomRight, pointTopRight, bottomRight, topRight))
            }
            3 -> {
                contour.add(interpolate(pointBottomLeft, pointTopLeft, bottomLeft, topLeft))
                contour.add(interpolate(pointBottomRight, pointTopRight, bottomRight, topRight))
            }
            4 -> {
                contour.add(interpolate(pointTopRight, pointBottomRight, topRight, bottomRight))
                contour.add(interpolate(pointTopLeft, pointTopRight, topLeft, topRight))
            }
            5 -> {
                contour.add(interpolate(pointBottomLeft, pointBottomRight, bottomLeft, bottomRight))
                contour.add(interpolate(pointTopRight, pointTopLeft, topRight, topLeft))
            }
            6 -> {
                contour.add(interpolate(pointBottomLeft, pointBottomRight, bottomLeft, bottomRight))
                contour.add(interpolate(pointTopLeft, pointTopRight, topLeft, topRight))
            }
            7 -> {
                contour.add(interpolate(pointBottomLeft, pointTopLeft, bottomLeft, topLeft))
                contour.add(interpolate(pointTopRight, pointTopLeft, topRight, topLeft))
            }
            8 -> {
                contour.add(interpolate(pointBottomLeft, pointTopLeft, bottomLeft, topLeft))
                contour.add(interpolate(pointTopLeft, pointTopRight, topLeft, topRight))
            }
            9 -> {
                contour.add(interpolate(pointBottomLeft, pointBottomRight, bottomLeft, bottomRight))
                contour.add(interpolate(pointTopLeft, pointTopRight, topLeft, topRight))
            }
            10 -> {
                contour.add(interpolate(pointBottomLeft, pointTopLeft, bottomLeft, topLeft))
                contour.add(interpolate(pointBottomRight, pointTopRight, bottomRight, topRight))
            }
            11 -> {
                contour.add(interpolate(pointBottomRight, pointTopRight, bottomRight, topRight))
                contour.add(interpolate(pointTopLeft, pointTopRight, topLeft, topRight))
            }
            12 -> {
                contour.add(interpolate(pointBottomLeft, pointTopLeft, bottomLeft, topLeft))
                contour.add(interpolate(pointBottomRight, pointTopRight, bottomRight, topRight))
            }
            13 -> {
                contour.add(interpolate(pointBottomLeft, pointBottomRight, bottomLeft, bottomRight))
                contour.add(interpolate(pointBottomRight, pointTopRight, bottomRight, topRight))
            }
            14 -> {
                contour.add(interpolate(pointBottomLeft, pointBottomRight, bottomLeft, bottomRight))
                contour.add(interpolate(pointBottomLeft, pointTopLeft, bottomLeft, topLeft))
            }
        }

        return contour
    }

    private fun contourLevels(interval: Int): List<Int> {
        val bounds = tile.elevationBounds
        val lower = (bounds.min.meters / interval) * interval
        val upper = (bounds.max.meters / interval) * interval
        return (lower..upper step interval).toList()
    }

    private fun close(p1: DoublePoint, p2: DoublePoint): Boolean {
        val delta = 0.01
        return p1 == p2 || (abs(p1.x - p2.x) <= delta && abs(p1.y - p2.y) <= delta)
    }

    private fun findContour(endToContour: Map<DoublePoint, MutableList<DoublePoint>>, end: DoublePoint): MutableList<DoublePoint>? {
        return endToContour[end] ?: endToContour.entries.firstOrNull { close(it.key, end) }?.value
    }

}