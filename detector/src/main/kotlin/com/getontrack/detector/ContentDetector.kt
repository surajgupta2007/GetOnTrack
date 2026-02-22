package com.getontrack.detector

import android.graphics.Bitmap

/**
 * Interface for content detection.
 * 
 * Implementations analyze screen content to detect distracting patterns.
 * This is a stub interface - real ML implementation would use TensorFlow Lite
 * or similar on-device inference.
 */
interface ContentDetector {
    /**
     * Analyze a bitmap for distraction patterns.
     * 
     * @param bitmap The screen capture to analyze.
     * @return Detection result with confidence and timing.
     */
    fun analyze(bitmap: Bitmap): DetectionResult
}

/**
 * Stub implementation for testing purposes.
 * 
 * DOES NOT implement real machine learning.
 * In production, this would use an on-device ML model.
 */
class StubContentDetector : ContentDetector {
    override fun analyze(bitmap: Bitmap): DetectionResult {
        // Stub implementation - always returns low confidence
        // Real implementation would run ML inference here
        return DetectionResult(
            confidence = 0.1f,
            inferenceTimeMs = 50L
        )
    }
}
