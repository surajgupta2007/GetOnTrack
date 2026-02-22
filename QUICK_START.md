# GetOnTrack - Quick Start Guide

## Project Structure

```
GetOnTrack/
├── :core       → Pure Kotlin state machine (NO Android)
├── :engine     → Coroutine runtime wrapper (NO Android)
├── :policy     → Strictness implementations
├── :detector   → Content detection interfaces
├── :system     → Android services (Accessibility, MediaProjection)
├── :overlay    → Blocking UI overlay
├── :storage    → Encrypted persistence
└── :app        → Dependency wiring
```

## Module Dependencies

```
:app
 ├─ :core
 ├─ :engine ─┬─ :core
 ├─ :policy ─┘
 ├─ :detector
 ├─ :system ──┬─ :core
 │            ├─ :engine
 │            └─ :detector
 ├─ :overlay ─┬─ :core
 │            └─ :engine
 └─ :storage ─┴─ :core
```

## Key Constraints

### ❌ NEVER Do This
- Import Android classes in `:core` or `:engine`
- Add business logic to Activities/Services
- Create global mutable state
- Add INTERNET permission
- Store frames or upload data
- Use reflection or dynamic loading

### ✅ ALWAYS Do This
- Keep state machine pure and deterministic
- Use coroutines only in `:engine` layer
- Make state transitions explicit
- Handle threading properly with @Volatile
- Recycle bitmaps immediately
- Use EncryptedSharedPreferences for storage

## State Machine Flow

```
┌─────┐ SessionStarted   ┌────────────┐
│Idle │─────────────────→│ Monitoring │
└─────┘                   └─────┬──────┘
   ↑                            │
   │ SessionStopped             │ Detection
   │                            ↓
   │                     ┌──────────────┐
   └─────────────────────┤BlockTriggered│
                         └──────┬───────┘
                                │
                    ┌───────────┴────────────┐
                    │                        │
             OverrideRequested         (auto/max)
                    ↓                        ↓
             ┌──────────┐              ┌──────────┐
             │ Override │              │ Cooldown │
             └────┬─────┘              └────┬─────┘
                  │                         │
                  │   CooldownFinished      │
                  └────────┬────────────────┘
                           ↓
                     Back to Monitoring
```

## Building

```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :core:build

# Run tests
./gradlew test

# Install app
./gradlew installDebug
```

## Testing Pure Modules

```kotlin
// :core module - Standard JUnit
@Test
fun `idle to monitoring transition`() {
    val policy = ModeratePolicy()
    val machine = SessionStateMachine(policy)
    
    val newState = machine.handle(SessionEvent.SessionStarted)
    
    assertTrue(newState is SessionState.Monitoring)
}
```

## Adding a New Policy

```kotlin
// In :policy module
class CustomPolicy : BlockingPolicy {
    override fun threshold(): Float = 0.85f
    override fun cooldownDurationMs(): Long = 90_000L // 1.5 min
    override fun maxOverridesPerDay(): Int = 3
}
```

## Integrating Real ML Model

```kotlin
// In :detector module
class TFLiteContentDetector(
    private val context: Context
) : ContentDetector {
    
    private val interpreter: Interpreter = // Load TFLite model
    
    override fun analyze(bitmap: Bitmap): DetectionResult {
        val startTime = System.currentTimeMillis()
        
        // Preprocess bitmap
        val input = preprocessBitmap(bitmap)
        
        // Run inference
        val output = FloatArray(1)
        interpreter.run(input, output)
        
        return DetectionResult(
            confidence = output[0],
            inferenceTimeMs = System.currentTimeMillis() - startTime
        )
    }
}
```

## Common Gotchas

1. **Date Comparison Bug** ✅ Fixed
   - Calendar.MONTH is 0-based
   - Use SimpleDateFormat instead

2. **Thread Safety** ✅ Fixed
   - Service dependencies need @Volatile
   - SessionEngine uses synchronized blocks

3. **Policy Mismatch** ✅ Fixed
   - Overlay now receives maxOverrides from caller
   - No hardcoded values

4. **Build Configuration** ✅ Fixed
   - Use kotlin("android") not id("org.jetbrains.kotlin.android")
   - Repository config in settings.gradle.kts

## Security Checklist

- [x] No INTERNET permission
- [x] EncryptedSharedPreferences for storage
- [x] No analytics SDKs
- [x] No frame retention
- [x] Local processing only
- [x] Thread-safe dependency injection
- [x] No hardcoded secrets

## Performance Tips

- Screen capture @ 1 FPS (adjustable in ScreenCaptureService)
- Bitmap recycling immediately after analysis
- Background dispatcher for heavy work
- Coroutine-based timers for efficiency

## Documentation

- `README.md` - Project overview
- `ARCHITECTURE.md` - Detailed architecture guide  
- `IMPLEMENTATION_SUMMARY.md` - Implementation details
- This file - Quick reference

## Support

For issues or questions, refer to:
1. ARCHITECTURE.md for design decisions
2. Code comments for implementation details
3. Test files for usage examples
