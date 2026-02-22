# GetOnTrack Architecture

A multi-module Android application for blocking distractions with clean architectural separation.

## Module Structure

### :core (Pure Kotlin - NO Android)
**Purpose:** Pure state machine logic

**Components:**
- `SessionState` - Sealed class representing all possible session states
- `SessionEvent` - Sealed class for state transition events
- `BlockingPolicy` - Interface defining blocking behavior
- `SessionStateMachine` - Deterministic state machine with no side effects

**Critical Constraints:**
- NO Android imports
- NO coroutines
- NO timers
- NO side effects
- Deterministic transitions only

### :engine (Pure Kotlin - NO Android)
**Purpose:** Runtime wrapper with coroutine-based timing

**Components:**
- `SessionObserver` - Functional interface for state change notifications
- `SessionEngine` - Thread-safe wrapper managing timers and observers

**Critical Constraints:**
- NO Android imports
- Uses Kotlin coroutines for timing
- Thread-safe event processing
- Automatic cooldown management

### :policy (Pure Kotlin - NO Android)
**Purpose:** Blocking policy implementations

**Components:**
- `LenientPolicy` - 70% threshold, 30s cooldown, 10 overrides/day
- `ModeratePolicy` - 80% threshold, 1min cooldown, 5 overrides/day
- `StrictPolicy` - 90% threshold, 3min cooldown, 2 overrides/day

### :detector (Android Library)
**Purpose:** Content detection interfaces and stubs

**Components:**
- `DetectionResult` - Data class for detection results
- `ContentDetector` - Interface for content analysis
- `StubContentDetector` - Stub implementation (random confidence)

**Note:** Replace StubContentDetector with real ML model in production

### :system (Android Library)
**Purpose:** System integration adapters

**Components:**
- `AccessibilityController` - AccessibilityService for app switch monitoring
- `ScreenCaptureService` - MediaProjection-based screen capture and analysis

**Features:**
- Samples at 1 frame/second
- Processes in background dispatcher
- Never stores frames
- No network uploads

### :overlay (Android Library)
**Purpose:** Blocking UI overlay

**Components:**
- `InterventionOverlayService` - Full-screen blocking overlay

**Features:**
- Displays countdown timer
- Shows override button (when available)
- Updates based on state changes
- Non-dismissible during cooldown

### :storage (Android Library)
**Purpose:** Encrypted local persistence

**Components:**
- `SessionStore` - EncryptedSharedPreferences wrapper

**Features:**
- Saves override count with daily reset
- Persists last session state
- No analytics
- No network calls

### :app (Android Application)
**Purpose:** Dependency wiring only

**Components:**
- `GetOnTrackApplication` - Dependency initialization
- `MainActivity` - Simple UI for session control

**Constraints:**
- NO business logic
- Only initialization and event delegation

## State Machine Flow

```
Idle
  └─> SessionStarted
      └─> Monitoring
          ├─> Detection (high confidence)
          │   └─> BlockTriggered
          │       ├─> OverrideRequested (overrides available)
          │       │   └─> Override
          │       │       └─> CooldownFinished
          │       │           └─> Monitoring
          │       └─> (auto-transition or no overrides)
          │           └─> Cooldown
          │               └─> CooldownFinished
          │                   └─> Monitoring
          └─> SessionStopped
              └─> Idle
```

## Permissions Required

- `FOREGROUND_SERVICE` - For screen capture service
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - For MediaProjection
- `POST_NOTIFICATIONS` - For foreground service notification
- `SYSTEM_ALERT_WINDOW` - For blocking overlay
- `BIND_ACCESSIBILITY_SERVICE` - For accessibility service

## Security & Privacy

- **No INTERNET permission** - Cannot upload data
- **No analytics SDK** - No telemetry
- **Encrypted storage** - AES256-GCM encryption
- **Local processing** - All detection happens on-device
- **No frame storage** - Captured frames immediately discarded

## Testing

Pure modules (:core, :engine, :policy) can be tested with standard JUnit.
Android modules require instrumented tests.

## Build

```bash
./gradlew build
```

## Architecture Principles

1. **Clean Separation** - Business logic in pure Kotlin modules
2. **No Android in Core** - State machine has zero Android dependencies
3. **Deterministic** - State transitions are pure functions
4. **Thread Safety** - Engine handles concurrency
5. **Testability** - Pure modules easily unit tested
6. **Maintainability** - Clear module boundaries
7. **Extensibility** - Easy to add new policies or detectors

## Future Enhancements

- Replace StubContentDetector with TensorFlow Lite model
- Add ML model for detecting social media/gaming content
- Implement focus session analytics (local only)
- Add customizable app blacklists
- Support for scheduled focus sessions
