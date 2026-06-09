# Quick Start Guide - Replay Buffer

## 30-Second Setup

### 1. Start Recording
```typescript
import { useReplayBuffer } from './hooks/useReplayBuffer';

const { start } = useReplayBuffer();
await start(5); // 5-minute buffer
```

### 2. Save Last 5 Minutes
```typescript
const { save } = useReplayBuffer();
const filePath = await save();
// Returns: /storage/emulated/0/Movies/replay_TIMESTAMP.mp4
```

### 3. Stop Recording
```typescript
const { stop } = useReplayBuffer();
await stop();
```

---

## File Locations

| Component | Path |
|-----------|------|
| React Native UI | `src/screens/ReplayScreen.tsx` |
| Native Module | `android/app/src/main/java/com/app/replaybuffer/` |
| Permissions | `android/app/src/main/AndroidManifest.xml` |
| Registration | `android/app/src/main/java/com/replay/MainApplication.kt` |
| Output Files | `/storage/emulated/0/Movies/` |

---

## Common Tasks

### Change Buffer Duration
```typescript
// In ReplayScreen.tsx
const BUFFER_DURATION = 10; // Change to 10 minutes
```

### Adjust Video Quality
```kotlin
// In VideoEncoder.kt
const val BIT_RATE = 5_000_000  // Change bitrate (5 Mbps default)
```

### Change Segment Length
```kotlin
// In SegmentManager.kt
const val SEGMENT_DURATION = 30_000L  // 30 seconds (milliseconds)
```

---

## Testing

### Start Recording
```bash
npx react-native run-android
# Tap "Start Recording"
```

### Simulate Activity
- Open apps
- Navigate screens
- Record some actions

### Save Replay
- Tap "Save Last 5 Minutes"
- Check `/storage/emulated/0/Movies/`

### Verify Output
```bash
adb shell ls -la /storage/emulated/0/Movies/
adb pull /storage/emulated/0/Movies/replay_*.mp4 ./
```

---

## Debugging

### View Logs
```bash
adb logcat | grep "ReplayBuffer\|SegmentManager\|ReplayMerger"
```

### Check Permissions
```bash
adb shell pm list permissions | grep -i record
```

### Clear Cache
```bash
adb shell rm -rf /storage/emulated/0/Android/data/com.replay/
```

### Monitor Service
```bash
adb shell dumpsys activity services | grep ReplayBufferService
```

---

## Architecture Quick Reference

```
JS Call: ReplayBuffer.startBuffer()
    ↓
Bridge: ReplayBufferModule.kt
    ↓
Service: ReplayBufferService.kt
    ↓
Manager: ProjectionManager.kt
    ↓
Encoder: SegmentManager.kt (30-sec chunks)
    ↓
Retention: RetentionManager.kt (5-min rolling)
    ↓
Output: Saved as MP4
```

---

## Key Files Summary

| File | Purpose |
|------|---------|
| `ReplayBufferModule.kt` | React Native bridge |
| `ReplayBufferService.kt` | Main service |
| `SegmentManager.kt` | Recording logic |
| `RetentionManager.kt` | Buffer management |
| `ReplayMerger.kt` | File merging |
| `useReplayBuffer.ts` | React hook |
| `ReplayScreen.tsx` | UI component |

---

## Common Errors & Fixes

| Error | Fix |
|-------|-----|
| Module not found | Check MainApplication.kt registration |
| Permission denied | Grant RECORD_AUDIO at runtime |
| Service crashes | Check AndroidManifest.xml service declaration |
| No audio | Verify RECORD_AUDIO permission |
| Merge fails | Check disk space & file permissions |

---

## Performance Stats

| Metric | Value |
|--------|-------|
| Codec | H.264 (AVC) |
| Bitrate | 5 Mbps |
| Frame Rate | 30 FPS |
| Segment Duration | 30 seconds |
| Buffer Duration | 5 minutes |
| Sample Output | replay_1622548800000.mp4 |
| Approx Size (5 min) | ~187.5 MB |

---

## Commands Reference

```bash
# Development
npx react-native run-android
npx react-native start

# Build
cd android && ./gradlew clean && cd ..

# Debug
adb logcat | grep ReplayBuffer
adb shell dumpsys activity services

# File Access
adb shell ls -la /storage/emulated/0/Movies/
adb pull /storage/emulated/0/Movies/ ./replays/
```

---

## Next Steps

1. ✅ Set up folder structure
2. ✅ Implement React Native files
3. ✅ Create Kotlin native module
4. ✅ Register in MainApplication.kt
5. ⬜ Add permissions at runtime (if needed)
6. ⬜ Test on device
7. ⬜ Optimize bitrate for your needs
8. ⬜ Add error handling UI

---

For detailed setup, see `REPLAY_BUFFER_SETUP.md`
