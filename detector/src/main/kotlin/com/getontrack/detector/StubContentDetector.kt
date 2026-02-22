package com.getontrack.detector

import android.graphics.Bitmap

/**
 * Stub implementation of ContentDetector for testing.
 * Returns random confidence scores.
 * 
 * IMPORTANT: This is NOT a real ML implementation.
 * Replace with actual TensorFlow Lite or ONNX model in production.
 */
class StubContentDetector : ContentDetector {
    override fun analyze(bitmap: Bitmap): DetectionResult {
        val startTime = System.currentTimeMillis()
        
        // Stub: Return random confidence
        // In production, this would run actual ML inference
        val confidence = kotlin.random.Random.nextFloat()
        
        val inferenceTime = System.currentTimeMillis() - startTime
        
        return DetectionResult(
            confidence = confidence,
            inferenceTimeMs = inferenceTime
        )
    }
}
