package com.greensopinion.vectorvector.sink.contour

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LineSimplifierTest {
    private val simplifier = LineSimplifier(2.0)

    @Test
    fun `behavior with 0 points does not simplify`() {
        val input = Line(listOf())
        assertThat(simplifier.simplify(input).points).isEqualTo(input.points)
    }

    @Test
    fun `behavior with 1 point does not simplify`() {
        val input = Line(listOf(DoublePoint(1.0, 1.0)))
        assertThat(simplifier.simplify(input).points).isEqualTo(input.points)
    }

    @Test
    fun `behavior with 2 points does not simplify`() {
        val input = Line(listOf(DoublePoint(1.0, 1.0), DoublePoint(5.0, 2.0)))
        assertThat(simplifier.simplify(input).points).isEqualTo(input.points)
    }

    @Test
    fun `straight line simplifies to start and end points`() {
        val input = Line(listOf(DoublePoint(1.0, 1.0), DoublePoint(2.0, 2.0), DoublePoint(3.0, 3.0), DoublePoint(4.0, 4.0)))
        val expectedOutput = Line(listOf(DoublePoint(1.0, 1.0), DoublePoint(4.0, 4.0)))
        assertThat(simplifier.simplify(input).points).isEqualTo(expectedOutput.points)
    }

    @Test
    fun `small curve does simplify`() {
        val input = Line(listOf(DoublePoint(1.0, 1.0), DoublePoint(2.0, 1.9), DoublePoint(3.0, 1.0)))
        val expectedOutput = Line(listOf(DoublePoint(1.0, 1.0), DoublePoint(3.0, 1.0)))
        assertThat(simplifier.simplify(input).points).isEqualTo(expectedOutput.points)
    }

    @Test
    fun `large curve does not simplify`() {
        val input = Line(listOf(DoublePoint(1.0, 1.0), DoublePoint(2.0, 3.1), DoublePoint(3.0, 1.0)))
        assertThat(simplifier.simplify(input).points).isEqualTo(input.points)
    }
}