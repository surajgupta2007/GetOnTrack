# GetOnTrack

Block distractions and retain your attention and increase focus and concentration.

## Architecture

GetOnTrack follows a strict multi-module architecture with clean separation of concerns:

### Modules

- **:core** - Pure Kotlin state machine (NO Android imports)
- **:engine** - Runtime wrapper around state machine (NO Android imports)  
- **:policy** - Strictness implementations (pure Kotlin)
- **:detector** - Detection interfaces and models
- **:system** - Android Accessibility + MediaProjection adapters
- **:overlay** - Overlay enforcement UI
- **:storage** - Encrypted local persistence
- **:app** - Dependency wiring only

### State Machine

The core state machine manages 5 states:
- **Idle** - No active monitoring
- **Monitoring** - Actively tracking user activity
- **BlockTriggered** - Distraction detected above threshold
- **Cooldown** - Enforced waiting period
- **Override** - User bypassed block (limited uses)

### Key Constraints

✓ **:core and :engine** have NO Android dependencies  
✓ **No INTERNET permission** - fully offline  
✓ **No business logic** in Activities or Services  
✓ **Deterministic state transitions** only  
✓ **Thread-safe** implementation  
✓ **Encrypted local storage** for privacy  

## Building

This project requires Android SDK. Pure Kotlin modules (:core, :engine, :policy) can be built independently:

```bash
./gradlew :core:build :engine:build :policy:build
```

Full Android build:
```bash
./gradlew assembleDebug
```

## Security & Privacy

- All data stored locally using EncryptedSharedPreferences
- No network calls or data uploading
- No analytics SDK or tracking
- Open source and auditable

## Implementation Status

✓ Complete architecture implementation  
✓ State machine with deterministic transitions  
✓ Coroutine-based timer management  
✓ Accessibility service integration  
✓ Screen capture service (MediaProjection)  
✓ Full-screen overlay system  
✓ Encrypted persistence layer  

⚠️ ContentDetector is stub only - ML implementation needed  
⚠️ No unit tests yet - test infrastructure in place  

## License

[Add your license here]
