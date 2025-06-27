# German Learning Widget

A **standalone Android app** for learning German with an interactive home screen widget that delivers German sentences throughout the day.

## ✨ Features

- **📱 Native Android App**: Built with Jetpack Compose
- **🏠 Home Screen Widget**: Learn German without opening the app
- **📚 Progressive Learning**: Supports A1, A2, B1, B2, C1, C2 levels
- **🔖 Bookmarks**: Save your favorite sentences
- **⚙️ Customizable**: Choose topics and delivery frequency
- **📱 Offline-First**: No internet connection required
- **🎯 Smart Delivery**: Background notifications at your preferred times

## 🏗️ Architecture

**100% Local & Offline:**
- **Local Data**: Hardcoded German sentences with translations
- **DataStore**: Local preferences and bookmarks storage
- **WorkManager**: Background sentence delivery
- **No Backend Required**: Self-contained Android app

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK 24+ (Android 7.0)
- Kotlin 1.8+

### Installation

1. **Clone the repository:**
```bash
git clone https://github.com/yourusername/german-learning-widget.git
cd german-learning-widget
```

2. **Open in Android Studio:**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the project folder

3. **Build and Run:**
   - Wait for Gradle sync to complete
   - Click the Run button or press `Shift + F10`

### Adding the Widget

1. Long-press on your home screen
2. Select "Widgets"
3. Find "German Learning Widget"
4. Drag to your desired location
5. Configure your learning preferences

## 📊 Sample Content

The app includes 15+ sample German sentences across different levels:
- **A1 Level**: Basic greetings and introductions
- **A2 Level**: Daily conversations, food, weather
- **B1 Level**: Work, travel, detailed conversations

Topics include: Greetings, Introductions, Food, Weather, Travel, Work, Daily Life, Language

## 🔧 Development

### Project Structure
```
app/
├── src/main/java/com/germanleraningwidget/
│   ├── data/
│   │   ├── model/          # Data models (GermanSentence, UserPreferences)
│   │   └── repository/     # Local data management
│   ├── ui/
│   │   ├── components/     # Reusable UI components
│   │   ├── screen/         # App screens (Home, Bookmarks, Settings)
│   │   ├── theme/          # Material Design theme
│   │   └── viewmodel/      # State management
│   ├── widget/             # Home screen widget
│   └── worker/             # Background tasks
```

### Key Technologies
- **Jetpack Compose**: Modern UI toolkit
- **Navigation Compose**: Screen navigation
- **DataStore**: Local preferences storage
- **WorkManager**: Background task scheduling
- **Material Design 3**: UI components and theming

### Building Release APK
```bash
./gradlew assembleRelease
```

## 📱 App Screenshots

- **Onboarding**: Select your German level and learning preferences
- **Home Screen**: View daily sentences and track progress
- **Bookmarks**: Save and review your favorite sentences
- **Widget**: Quick German learning from your home screen

## 🎯 Roadmap

- [ ] Add more sentence categories (business, travel, etc.)
- [ ] Implement spaced repetition algorithm
- [ ] Add pronunciation guides
- [ ] Support for other languages
- [ ] Advanced progress tracking
- [ ] Dark mode theme improvements

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

If you encounter any issues:
1. Check the [Issues](https://github.com/yourusername/german-learning-widget/issues) page
2. Create a new issue with detailed information
3. Include your Android version and device model

---

**Happy Learning! 🇩🇪📚** 