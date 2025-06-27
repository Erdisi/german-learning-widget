# German Learning Widget 🇩🇪

A modern Android app that helps you learn German through interactive home screen widgets and personalized learning sessions.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)

## 📱 Features

### 🏠 Home Screen Widget
- **Interactive German Learning Widget** that displays German sentences with translations
- **Bookmark System** - Save your favorite sentences directly from the widget
- **Smart Content Delivery** - New sentences delivered based on your learning schedule
- **Beautiful Material Design 3** styling with adaptive colors

### 🎯 Personalized Learning
- **Learning Level Selection** - Choose from A1 (Beginner) to C2 (Proficient)
- **Topic Customization** - Select from various topics like Greetings, Travel, Food, Business, etc.
- **Flexible Scheduling** - Delivery frequency from every 30 minutes to daily
- **Native Language Support** - Currently supports English speakers learning German

### 📚 Learning Management
- **Bookmarks Screen** - Review all your saved sentences in one place
- **Progress Tracking** - Keep track of your learning journey
- **Smart Recommendations** - Content tailored to your level and interests

### 🔧 Technical Features
- **Offline-First** - Works without internet connection
- **Background Sync** - Automatic content updates using WorkManager
- **Data Persistence** - Settings and bookmarks saved locally with DataStore
- **Modern Architecture** - MVVM pattern with Repository design

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or newer
- Android SDK API 24+ (Android 7.0)
- Kotlin 1.8.0+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/german-learning-widget.git
   cd german-learning-widget
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned directory and select it

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use Android Studio's build button

4. **Add Widget to Home Screen**
   - Long press on your home screen
   - Select "Widgets"
   - Find "German Learning Widget"
   - Drag it to your desired location

## 🏗️ Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture with the following key components:

### 📁 Project Structure
```
app/src/main/java/com/germanleraningwidget/
├── data/
│   ├── model/           # Data models (GermanSentence, UserPreferences)
│   └── repository/      # Data access layer
├── ui/
│   ├── components/      # Reusable UI components
│   ├── screen/          # App screens (Home, Bookmarks, Setup)
│   ├── theme/           # Material Design 3 theming
│   └── viewmodel/       # ViewModels for state management
├── widget/              # Home screen widget implementation
└── worker/              # Background work scheduling
```

### 🔧 Key Technologies
- **Jetpack Compose** - Modern declarative UI toolkit
- **DataStore** - Modern data storage solution
- **WorkManager** - Background task scheduling
- **Coroutines** - Asynchronous programming
- **StateFlow** - Reactive state management
- **Material Design 3** - Latest design system

## 📖 Usage

### First Time Setup
1. Open the app and complete the onboarding process
2. Select your German learning level (A1-C2)
3. Choose your preferred topics
4. Set your learning frequency
5. Add the widget to your home screen

### Using the Widget
- **View Sentences** - New German sentences appear based on your schedule
- **Bookmark** - Tap the bookmark icon to save sentences
- **Open App** - Tap the widget to open the main app

### Managing Bookmarks
- Navigate to the "Saved Sentences" screen in the app
- View all your bookmarked sentences
- Remove bookmarks by tapping the bookmark icon

## 🛠️ Development

### Building
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

### Code Style
This project follows [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and uses:
- **ktlint** for code formatting
- **Android lint** for static analysis

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- German sentence data curated for language learning
- Material Design 3 guidelines from Google
- Android Jetpack libraries for modern development
- Community feedback and suggestions

## 📞 Support

If you encounter any issues or have questions:
- Open an issue on GitHub
- Check the [documentation](docs/)
- Contact: erdisdriza@gmail.com

---

**Happy Learning! 🎓📱**

Made with ❤️ for German language learners 