# Mindful Usage (BrainTrap)

A digital wellness Android application designed to reduce "brain rot" by helping users manage and limit their social media consumption through app blocking, time limits, and cognitive challenges.

## Features

### Core Functionality

1. **App Blocking System**
   - Select apps to block from installed applications
   - Pre-populated list of common social media apps
   - Real-time blocking using Android Accessibility Service
   - Visual overlay when blocked apps are accessed

2. **Time Limit Management**
   - Set daily time limits for each blocked app
   - Real-time tracking of usage time
   - Visual indicators showing remaining time
   - Separate limits for weekdays vs. weekends (configurable)
   - Focus Mode that immediately locks all apps

3. **Arithmetic Challenge System**
   - Progressive difficulty based on unlock attempts:
     - 1st unlock: Simple addition/subtraction
     - 2nd unlock: Multiplication/division
     - 3rd+ unlock: Multi-step problems
   - Must solve 3 consecutive problems correctly
   - Earns 15 minutes of additional time
   - 30-second timer per problem

4. **Statistics & Insights**
   - Daily/weekly/monthly usage statistics (UI ready, data collection implemented)
   - Streak tracking (consecutive days staying under limits)
   - Challenge completion tracking

## Technical Architecture

### Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Database**: Room (SQLite)
- **Navigation**: Navigation Compose
- **Background Processing**: WorkManager

### Project Structure

```
app/src/main/java/com/example/braintrap/
├── admin/                    # Device admin receiver
├── data/
│   ├── database/            # Room database, entities, DAOs
│   ├── model/               # Data models
│   └── repository/          # Repository layer
├── di/                      # Hilt dependency injection modules
├── service/                 # Background services
│   ├── AppBlockingService   # Accessibility service for blocking
│   └── UsageTrackingService # Usage stats tracking
├── ui/
│   ├── screen/              # Compose screens
│   ├── viewmodel/           # ViewModels
│   └── theme/               # App theme
└── util/                    # Utilities (ChallengeGenerator, AppInfoProvider)
```

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+ (Android 8.0+)
- Target SDK: 36 (Android 14)

### Installation

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run on an Android device or emulator

### Required Permissions

The app requires the following permissions:
- **Accessibility Service**: To detect and block app launches
- **Usage Access**: To track app usage time
- **Display Over Other Apps**: For showing blocking screens
- **Device Admin** (optional): For enhanced security

These permissions will be requested during first-time setup.

## Usage

1. **First Launch**: Grant required permissions when prompted
2. **Select Apps**: Navigate to app selection screen and choose apps to monitor
3. **Set Limits**: Configure daily time limits for each app in Settings
4. **Monitor Usage**: View usage statistics on the dashboard
5. **Unlock Apps**: When limits are reached, solve arithmetic challenges to earn additional time

## Key Components

### AppBlockingService
- Monitors app launches using Accessibility Service
- Blocks apps when daily limits are reached
- Shows blocking screen overlay

### UsageTrackingService
- Tracks app usage time using UsageStatsManager
- Updates database with daily statistics
- Calculates remaining time

### ChallengeGenerator
- Generates arithmetic problems with progressive difficulty
- Ensures cognitive friction before unlocking apps

## Database Schema

- **TimeLimitEntity**: Stores time limit configurations per app
- **UsageStatsEntity**: Daily usage statistics
- **StreakEntity**: Streak tracking data

## Future Enhancements

- Onboarding flow with permission requests
- Enhanced statistics dashboard with charts
- Scheduled blocking (time-based)
- Location-based blocking
- Website blocking in browsers
- Social features and challenges
- Parent controls

## Notes

- The app uses local storage only (no cloud sync in v1.0)
- All data is stored locally using Room database
- No network permissions required
- Privacy-first approach: no analytics or data collection

## License

This project is created as a reference implementation based on the provided PRD.

