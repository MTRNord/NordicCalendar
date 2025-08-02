# Nordic Calendar

A modern, Material 3 calendar application for Android that integrates seamlessly with your existing
calendar accounts.

## Features

- **Material 3 Design** - Clean, modern interface following Google's latest design guidelines
- **Multiple View Types** - Month, week, and day views for different planning needs
- **Calendar Integration** - Works with your existing calendar accounts (CalDAV, Google Calendar,
  etc.)
- **Smart Navigation** - Intuitive navigation between different time periods
- **Event Management** - View and interact with your calendar events
- **Dark Mode Support** - Automatically adapts to your system theme
- **Accessibility** - Built with accessibility in mind

## Screenshots

<!-- TODO: Add screenshots when available -->

## Requirements

- Android 8.0 (API level 26) or higher
- Calendar permissions for reading existing events

## Installation

### From Releases

1. Download the latest APK from the [Releases](https://github.com/MTRNord/NordicCalendar/releases)
   page
2. Install the APK on your Android device
3. Grant necessary permissions when prompted

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/MTRNord/NordicCalendar.git
   cd NordicCalendar
   ```

2. Build with Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

3. Install on your device:
   ```bash
   ./gradlew installDebug
   ```

## Development

### Prerequisites

- Android Studio Hedgehog | 2023.1.1 or newer
- JDK 17 or higher
- Android SDK with API level 34

### Tech Stack

- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern UI toolkit
- **Material 3** - Design system and components
- **Hilt** - Dependency injection
- **Navigation Compose** - Navigation framework

### Architecture

The app follows Android's recommended architecture patterns:

- **MVVM** - Model-View-ViewModel pattern
- **Repository Pattern** - Data layer abstraction
- **Single Activity** - Navigation handled by Compose Navigation

### Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open
an issue first to discuss what you would like to change.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Ensure proper null safety

## License

This project is licensed under the European Union Public License 1.2 (EUPL-1.2) - see
the [LICENSE](LICENSE) file for details.

## Privacy

Nordic Calendar respects your privacy:

- All calendar data remains on your device
- No data is collected or transmitted to external servers
- Only necessary permissions are requested

## Support

If you encounter any issues or have suggestions:

- Open an issue on [GitHub Issues](https://github.com/MTRNord/NordicCalendar/issues)
- Check existing issues before creating a new one
- Provide detailed information about your device and Android version

## Acknowledgments

- [Material Design](https://material.io/) for design guidelines
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for the modern UI framework
- The Android development community for continuous inspiration
