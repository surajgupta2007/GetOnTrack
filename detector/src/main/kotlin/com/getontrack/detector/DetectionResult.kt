package com.getontrack.detector

/**
 * Result from content detection analysis.
 * @param confidence Detection confidence score (0.0-1.0)
 * @param inferenceTimeMs Time taken for inference in milliseconds
 */
data class DetectionResult(
    val confidence: Float,
    val inferenceTimeMs: Long
)
