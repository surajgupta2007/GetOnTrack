package com.getontrack.detector

/**
 * Result of content detection analysis.
 *
 * @property confidence Detection confidence level (0.0 to 1.0).
 *                      Higher values indicate stronger match to distraction pattern.
 * @property inferenceTimeMs Time taken for inference in milliseconds.
 */
data class DetectionResult(
    val confidence: Float,
    val inferenceTimeMs: Long
)
