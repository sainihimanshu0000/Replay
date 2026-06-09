# React Native Replay Buffer Implementation

This project implements a real-time screen recording buffer for React Native on Android. It maintains a rolling buffer of the last 5 minutes of screen activity and allows saving it as an MP4 file.

## Architecture Overview

```
React Native UI
       ↓
Native Module Bridge (Kotlin)
       ↓
Foreground Service (ReplayBufferService)
       ↓
MediaProjection API
       ↓
Segment Manager (30-second segments)
       ↓
Retention Manager (5-minute rolling buffer)
       ↓
Video Encoder (H.264)
       ↓
Replay Merger (MP4 output)
```

## Project Structure

### React Native (TypeScript/JavaScript)

```
src/
├── modules/
│   └── ReplayBuffer/
│       ├── index.ts              # Exports
│       ├── ReplayBuffer.ts        # JS Bridge to native
│       └── types.ts              # TypeScript interfaces
├── hooks/
│   └── useReplayBuffer.ts        # Custom hook for replay buffer
├── screens/
│   └── ReplayScreen.tsx          # Main UI screen
├── components/
│   ├── ReplayButton.tsx          # Reusable button component
│   └── RecordingIndicator.tsx    # Recording status indicator
└── services/
    └── ReplayService.ts          # Service wrapper
```

### Android Native (Kotlin)

```
android/app/src/main/java/com/app/replaybuffer/
├── ReplayBufferModule.kt         # React Native module
├── ReplayBufferPackage.kt        # Package registration
├── service/
│   └── ReplayBufferService.kt    # Foreground service
├── projection/
│   └── ProjectionManager.kt      # MediaProjection management
├── buffer/
│   ├── SegmentManager.kt         # 30-second segment recording
│   └── RetentionManager.kt       # Rolling buffer retention
├── encoder/
│   └── VideoEncoder.kt           # H.264 video encoding
└── merger/
    └── ReplayMerger.kt           # Segment merging
```

## Setup Instructions

### Prerequisites

- React Native 0.60+
- Android SDK 21+ (API level 21)
- Node.js 14+

### Installation Steps

1. **Install dependencies:**
   ```bash
   npm install
   cd android
   ./gradlew clean
   cd ..
   ```

2. **Build and run:**
   ```bash
   npx react-native run-android
   ```

3. **For development with live reload:**
   ```bash
   npx react-native start
   npx react-native run-android
   ```

## Native Module API

### TypeScript Interface

```typescript
interface ReplayBufferModule {
  startBuffer(options: { minutes: number }): Promise<void>;
  saveReplay(): Promise<string>;
  stopBuffer(): Promise<void>;
}
```

### Methods

#### `startBuffer(options: StartBufferOptions): Promise<void>`

Starts the replay buffer service with a specified duration.

**Parameters:**
- `options.minutes` - Duration to maintain in the buffer (1-60 minutes)

**Example:**
```typescript
import ReplayBuffer from './modules/ReplayBuffer';

await ReplayBuffer.startBuffer({ minutes: 5 });
```

#### `saveReplay(): Promise<string>`

Saves the current buffer contents as an MP4 file.

**Returns:** Path to the saved replay file
- Example: `/storage/emulated/0/Movies/replay_1234567890.mp4`

**Example:**
```typescript
const filePath = await ReplayBuffer.saveReplay();
console.log('Saved to:', filePath);
```

#### `stopBuffer(): Promise<void>`

Stops the replay buffer service and cleans up resources.

**Example:**
```typescript
await ReplayBuffer.stopBuffer();
```

## Usage

### Using the Hook

```typescript
import { useReplayBuffer } from './hooks/useReplayBuffer';

const { start, save, stop, isRecording, isLoading, error } = useReplayBuffer();

const handleStart = async () => {
  await start(5); // 5-minute buffer
};

const handleSave = async () => {
  const filePath = await save();
  console.log('Replay saved:', filePath);
};

const handleStop = async () => {
  await stop();
};
```

### Using the Screen Component

```typescript
import { ReplayScreen } from './screens/ReplayScreen';

export default function App() {
  return <ReplayScreen />;
}
```

### Direct Module Usage

```typescript
import ReplayBuffer from './modules/ReplayBuffer';

async function handleRecording() {
  try {
    // Start recording
    await ReplayBuffer.startBuffer({ minutes: 5 });
    
    // Do some activity...
    
    // Save replay
    const path = await ReplayBuffer.saveReplay();
    console.log('Replay saved:', path);
    
    // Stop recording
    await ReplayBuffer.stopBuffer();
  } catch (error) {
    console.error('Replay error:', error);
  }
}
```

## Permissions

The following permissions are required in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAPTURE_VIDEO_OUTPUT" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

At runtime, you'll also need to request **RECORD_AUDIO** permission on Android 6.0+.

## File Storage

Replay files are stored in the app's external files directory:

- **Location:** `/storage/emulated/0/Android/data/com.replay/files/Movies/`
- **Format:** MP4 (H.264 video, AAC audio)
- **Naming:** `replay_TIMESTAMP.mp4`

You can access these files through:
- File Manager (if app is granted permission)
- Gallery app (Media Store)
- Other media players

## Key Components Explained

### ReplayBufferService

The foreground service that manages the entire recording process:
- Requests MediaProjection permissions
- Manages segment recording (30-second chunks)
- Enforces retention policy (keeps last N minutes)
- Merges segments when saving
- Runs with persistent notification

### SegmentManager

Handles recording video in 30-second segments:
- Each segment is a separate MP4 file
- Segments are stored in app's cache directory
- Automatically creates next segment when current completes
- Manages the recording lifecycle

### RetentionManager

Maintains the rolling buffer:
- Tracks creation time of each segment
- Removes segments older than retention window
- Keeps specified minutes of content
- Cleans up space efficiently

### ReplayMerger

Combines segments into final output:
- Uses MediaMuxer to merge MP4 files
- Preserves video and audio tracks
- Handles timing correctly
- Produces single MP4 file

### VideoEncoder

Encodes video frames:
- H.264 codec (AVC)
- Adjustable bitrate (5 Mbps default)
- 30 FPS frame rate
- 10-second I-frame interval

## Performance Considerations

### Bitrate and Quality

- Default: 5 Mbps (adjustable in `VideoEncoder.kt`)
- For higher quality: increase `BIT_RATE`
- For lower file size: decrease `BIT_RATE`

### Buffer Duration

- Longer duration = more disk space required
- 5 minutes at 5 Mbps ≈ 187.5 MB
- Estimate: `(BIT_RATE_Mbps × Duration_minutes × 60) / 8` MB

### Segment Size

- 30-second segments at 5 Mbps ≈ 18.75 MB each
- Default: 5 minutes = 10 segments retained

## Troubleshooting

### Service Not Starting

- Check that `ReplayBufferPackage` is registered in `MainApplication.kt`
- Verify `ReplayBufferService` is declared in `AndroidManifest.xml`
- Ensure foreground service permission is granted

### Permission Denied

- Request RECORD_AUDIO permission at runtime
- For Android 12+, also request RECORD_MEDIA_OUTPUT
- Check `AndroidManifest.xml` for required permissions

### Merge Failed

- Ensure segments aren't deleted during merge
- Check file paths are valid
- Verify disk space is available

### No Audio

- Ensure `RECORD_AUDIO` permission is granted
- Check `setAudioSource()` in `SegmentManager.kt`
- Verify device supports audio recording

## Build Commands

```bash
# Clean build
cd android
./gradlew clean
cd ..

# Run on device
npx react-native run-android

# Run specific activity
npx react-native run-android --deviceId <device_id>

# Debug build
cd android
./gradlew assembleDebug
cd ..

# Release build (requires keystore)
cd android
./gradlew assembleRelease
cd ..
```

## Development Tips

1. **Use Android Studio logcat for debugging:**
   ```bash
   adb logcat | grep ReplayBuffer
   ```

2. **Monitor storage usage:**
   ```bash
   adb shell df | grep emulated
   ```

3. **Test with short duration:**
   ```typescript
   await ReplayBuffer.startBuffer({ minutes: 1 });
   ```

4. **Check file output:**
   ```bash
   adb shell ls -la /storage/emulated/0/Movies/
   ```

## Future Enhancements

- [ ] Audio/video codec selection
- [ ] Custom bitrate adjustment
- [ ] Pause/resume recording
- [ ] Real-time encoding optimization
- [ ] Compression options
- [ ] Cloud storage integration
- [ ] Batch export capability
- [ ] Advanced retention policies

## License

This implementation is provided as-is for educational and development purposes.

## Notes

- This implementation is optimized for Android
- iOS support would require different MediaRecorder APIs
- The native module uses React Native's bridge for communication
- Service runs continuously in foreground to prevent termination
