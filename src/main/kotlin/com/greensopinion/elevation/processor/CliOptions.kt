package com.greensopinion.elevation.processor

import picocli.CommandLine.Option
import java.io.File

class CliOptions {
    @Option(names = ["-d", "--data"], required = true, description = ["The data directory containing elevation data in tiff format."])
    var dataDir: File? = null

    @Option(names = ["-o", "--output"], required = true, description = ["The directory containing output files."])
    var outputDir: File? = null

    @Option(names = ["-minZ"])
    var minZ: Int = 3
    @Option(names = ["-minX"])
    var minX: Int = 1
    @Option(names = ["-minY"])
    var minY: Int = 2
    @Option(names = ["-maxZ"])
    var maxZ: Int = 12
    @Option(names = ["-maxX"])
    var maxX: Int = 2
    @Option(names = ["-maxY"])
    var maxY: Int = 3
}