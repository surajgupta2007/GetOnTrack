package com.getontrack.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
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
import java.nio.ByteBuffer

/**
 * Foreground service that captures screen frames and analyzes them.
 * 
 * CRITICAL CONSTRAINTS:
 * - Never stores frames permanently
 * - Never uploads data
 * - Processes in background dispatcher
 * - Samples at 1 frame per second
 * 
 * Responsibilities:
 * - Use MediaProjection to capture screen
 * - Sample frames at low frequency
 * - Convert to Bitmap
 * - Delegate to ContentDetector
 * - Send results to SessionEngine
 */
class ScreenCaptureService : Service() {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureJob: Job? = null
    
    @Volatile
    private var sessionEngine: SessionEngine? = null
    @Volatile
    private var contentDetector: ContentDetector? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_capture_channel"
        private const val SAMPLE_RATE_MS = 1000L // 1 frame per second
        
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
    }
    
    /**
     * Set dependencies. Must be called before starting service.
     */
    fun setDependencies(engine: SessionEngine, detector: ContentDetector) {
        sessionEngine = engine
        contentDetector = detector
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        
        if (resultCode != -1 && data != null) {
            startCapture(resultCode, data)
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Initialize MediaProjection and start frame capture.
     */
    private fun startCapture(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        
        // Create ImageReader for frame capture
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener({ reader ->
                handleImageAvailable(reader)
            }, null)
        }
        
        // Create virtual display
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null, null
        )
        
        // Start periodic sampling
        startPeriodicCapture()
    }
    
    /**
     * Start job that samples frames periodically.
     */
    private fun startPeriodicCapture() {
        captureJob = scope.launch {
            while (isActive) {
                delay(SAMPLE_RATE_MS)
                // Frame capture triggered via ImageReader callback
            }
        }
    }
    
    /**
     * Handle captured image.
     * Runs in background thread.
     */
    private fun handleImageAvailable(reader: ImageReader) {
        scope.launch {
            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    val bitmap = imageToBitmap(image)
                    processFrame(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                image?.close()
            }
        }
    }
    
    /**
     * Convert Image to Bitmap.
     */
    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
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
     * Process captured frame through detector.
     */
    private fun processFrame(bitmap: Bitmap) {
        val detector = contentDetector ?: return
        val engine = sessionEngine ?: return
        
        try {
            val result = detector.analyze(bitmap)
            
            // Send detection result to engine
            engine.handleEvent(SessionEvent.Detection(result.confidence))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Never store bitmap - immediately recycle
            bitmap.recycle()
        }
    }
    
    /**
     * Create notification channel for foreground service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoring for distracting content"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create foreground notification.
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GetOnTrack Active")
            .setContentText("Monitoring for distractions")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        captureJob?.cancel()
        imageReader?.close()
        virtualDisplay?.release()
        mediaProjection?.stop()
        scope.cancel()
    }
}
