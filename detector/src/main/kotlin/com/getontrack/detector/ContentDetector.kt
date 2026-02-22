package com.getontrack.detector

import android.graphics.Bitmap

/**
 * Interface for analyzing content to detect distractions.
 * Implementations should perform ML inference or pattern matching.
 */
interface ContentDetector {
    /**
     * Analyze a bitmap for distracting content.
     * @param bitmap The screen capture to analyze
     * @return Detection result with confidence score
     */
    fun analyze(bitmap: Bitmap): DetectionResult
}
