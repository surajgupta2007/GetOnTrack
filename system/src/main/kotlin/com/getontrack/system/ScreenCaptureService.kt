package com.getontrack.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.getontrack.core.SessionEvent
import com.getontrack.detector.ContentDetector
import com.getontrack.engine.SessionEngine
import kotlinx.coroutines.*

/**
 * Foreground service that captures screen frames for content detection.
 * 
 * RESPONSIBILITIES:
 * - Use MediaProjection to capture screen
 * - Sample frames at 1 FPS
 * - Convert frames to Bitmap
 * - Delegate to ContentDetector for analysis
 * - Process in background dispatcher
 * 
 * CONSTRAINTS:
 * - Never store frames permanently
 * - Never upload data
 * - Process on background thread
 * - Clean up resources properly
 */
class ScreenCaptureService : Service() {
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "screen_capture_channel"
        private const val NOTIFICATION_ID = 1001
        private const val FRAME_RATE_FPS = 1
        private const val FRAME_INTERVAL_MS = 1000L / FRAME_RATE_FPS
    }
    
    private var sessionEngine: SessionEngine? = null
    private var contentDetector: ContentDetector? = null
    private var mediaProjection: MediaProjection? = null
    
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var captureJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start as foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Start capturing in the intent contains projection data
        intent?.let { startCapture(it) }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Inject dependencies from app layer.
     */
    fun setDependencies(engine: SessionEngine, detector: ContentDetector) {
        this.sessionEngine = engine
        this.contentDetector = detector
    }
    
    /**
     * Start screen capture using MediaProjection.
     */
    private fun startCapture(intent: Intent) {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        // In real implementation, resultCode and data would come from intent
        // mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        
        // Setup virtual display and image reader
        setupImageReader()
        
        // Start frame sampling
        startFrameSampling()
    }
    
    /**
     * Setup ImageReader for frame capture.
     */
    private fun setupImageReader() {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi
        
        imageReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            2
        )
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }
    
    /**
     * Start sampling frames at 1 FPS.
     */
    private fun startFrameSampling() {
        captureJob = serviceScope.launch {
            while (isActive) {
                captureAndAnalyzeFrame()
                delay(FRAME_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Capture a single frame and analyze it.
     * Processes on background dispatcher.
     */
    private suspend fun captureAndAnalyzeFrame() = withContext(Dispatchers.Default) {
        try {
            val image: Image? = imageReader?.acquireLatestImage()
            image?.use {
                val bitmap = imageToBitmap(it)
                
                // Delegate to ContentDetector
                val result = contentDetector?.analyze(bitmap)
                
                // Forward detection result to SessionEngine
                result?.let {
                    sessionEngine?.handleEvent(SessionEvent.Detection(it.confidence))
                }
            }
        } catch (e: Exception) {
            // Handle capture errors - log in production
        }
    }
    
    /**
     * Convert Image to Bitmap.
     * Never stores the frame - only used for immediate analysis.
     */
    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }
    
    /**
     * Create notification for foreground service.
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("GetOnTrack Active")
            .setContentText("Monitoring for distractions")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    /**
     * Create notification channel for Android O+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen capture for distraction detection"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        captureJob?.cancel()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        serviceScope.cancel()
    }
}
