package com.greensopinion.vectorvector.sink.contour

/**
 * Reduces the number of points in a line using the Ramer–Douglas–Peucker algorithm
 *
 * @param epsilon The tolerance value that determines the maximum allowed perpendicular distance from the line vector.
 */
class LineSimplifier(
    private val epsilon: Double
) {
    fun simplify(line: Line): Line {
        if (line.points.size <= 2) {
            return line
        }

        var maxDistance = 0.0
        var index = 0

        for (i in 1..<line.points.size - 1) {
            val distance = perpendicularDistance(line.points[i], line.points.first(), line.points.last())
            if (distance > maxDistance) {
                maxDistance = distance
                index = i
            }
        }

        if (maxDistance > epsilon) {
            val left = simplify(Line(line.points.take(index + 1)))
            val right = simplify(Line(line.points.drop(index)))
            return Line(left.points.dropLast(1) + right.points)
        } else {
            return Line(listOf(line.points.first(), line.points.last()))
        }
    }

    private fun perpendicularDistance(point: DoublePoint, lineStart: DoublePoint, lineEnd: DoublePoint): Double {
        if (lineStart == lineEnd) {
            return Math.sqrt(Math.pow(point.x - lineStart.x, 2.0) + Math.pow(point.y - lineStart.y, 2.0));
        }
        val a = lineStart.y - lineEnd.y
        val b = lineEnd.x - lineStart.x
        val c = lineStart.x * lineEnd.y - lineEnd.x * lineStart.y
        return Math.abs(a * point.x + b * point.y + c) / Math.hypot(a, b)
    }
}
