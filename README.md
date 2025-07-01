# German Learning Widget 🇩🇪

A modern Android app that helps you learn German through interactive home screen widgets and personalized learning sessions.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://docker.com)

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

### Build Environment Setup

#### Option 1: Host System Build (Recommended for Apple Silicon Macs)

##### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or newer
- Android SDK API 24+ (Android 7.0)
- Kotlin 1.8.0+

##### Installation
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

#### Option 2: Docker Environment (Recommended for Intel/Linux/CI)

##### Prerequisites
- Docker Desktop installed and running
- Docker Compose V2
- At least 8GB RAM allocated to Docker
- 10GB+ free disk space

##### Quick Start with Docker
```bash
# Clone the repository
git clone https://github.com/yourusername/german-learning-widget.git
cd german-learning-widget

# Quick development setup
./docker-dev.sh setup

# Build debug APK
./docker-dev.sh build-debug

# Run tests
./docker-dev.sh test

# Start development environment
./docker-dev.sh dev
```

##### Manual Docker Setup
```bash
# Build development environment
docker-compose build dev

# Run debug build
docker-compose run --rm ci-build ./gradlew assembleDebug

# Run tests with reports
docker-compose run --rm test ./gradlew test lint

# Production build (requires signing setup)
docker-compose run --rm production-build
```

### 🖥️ Platform-Specific Recommendations

| Platform | Recommended Setup | Reason |
|----------|------------------|---------|
| **Apple Silicon Mac (M1/M2/M3)** | Host System Build | AAPT2 compatibility issue with Docker |
| **Intel Mac** | Docker or Host | Both work well |
| **Linux (x86_64)** | Docker Preferred | Consistent environment |
| **Windows with WSL2** | Docker Preferred | Better Android SDK support |
| **CI/CD Servers** | Docker Always | Automation and consistency |

### 📚 Documentation

#### Docker Documentation
- **[🚀 Docker Quick Reference](DOCKER_QUICK_REFERENCE.md)** - Fast commands and platform compatibility
- **[📖 Complete Docker Guide](DOCKER_GUIDE.md)** - Comprehensive Docker usage instructions  
- **[⚙️ Docker Installation Guide](DOCKER_INSTALLATION_GUIDE.md)** - Platform-specific installation
- **[🔧 Docker Implementation Summary](DOCKER_IMPLEMENTATION_SUMMARY.md)** - Technical overview

#### Project Documentation  
- **[📋 Project Requirements Document](PRD.md)** - Complete technical specification
- **[🎨 Widget Customization Guide](WIDGET_CUSTOMIZATION_GUIDE.md)** - Widget configuration

### 🔧 Development Tools

#### Interactive Docker Helper
```bash
# Get list of all available commands
./docker-dev.sh help

# Check project and Docker status
./docker-dev.sh status

# Clean build with cache clearing
./docker-dev.sh rebuild

# Monitor build performance
./docker-dev.sh stats
```

#### Manual Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Lint analysis
./gradlew lint
```

### 4. Widget Setup
After installation:
1. Long press on your home screen
2. Select "Widgets"
3. Find "German Learning Widget"
4. Drag it to your desired location
5. Configure your learning preferences in the app

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
├── worker/              # Background work scheduling
└── di/                  # Dependency injection
```

### 🔧 Key Technologies
- **Jetpack Compose** - Modern declarative UI toolkit
- **DataStore** - Modern data storage solution
- **WorkManager** - Background task scheduling
- **Coroutines** - Asynchronous programming
- **StateFlow** - Reactive state management
- **Material Design 3** - Latest design system

### 🐳 Docker Infrastructure
- **Multi-stage Dockerfile** - Optimized for different environments
- **Docker Compose** - Multiple services (dev, ci-build, production, test)
- **GitHub Actions** - Automated CI/CD pipeline
- **Build Caching** - Gradle and Docker layer optimization

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

#### Host System
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

#### Docker Environment
```bash
# Using helper script
./docker-dev.sh build-debug
./docker-dev.sh build-release
./docker-dev.sh test

# Using docker-compose directly
docker-compose run --rm ci-build ./gradlew assembleDebug
docker-compose run --rm production-build
docker-compose run --rm test ./gradlew test
```

### Code Style
This project follows [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and uses:
- **ktlint** for code formatting
- **Android lint** for static analysis

### Testing
```bash
# Unit tests
./gradlew testDebugUnitTest

# UI tests (requires emulator/device)
./gradlew connectedDebugAndroidTest

# Docker testing
./docker-dev.sh test
```

## 🐳 Docker Features

### ✅ What Works with Docker
- **CI/CD Automation** - GitHub Actions with automated builds
- **Team Collaboration** - Consistent development environments
- **Release Builds** - Automated APK/AAB generation
- **Testing** - Automated testing and linting
- **x86_64 Platforms** - Intel Mac, Linux, Windows WSL2

### ⚠️ Known Limitations
- **Apple Silicon Macs** - AAPT2 architectural incompatibility
  - **Workaround**: Use host system builds (work perfectly)
  - **Alternative**: Use CI/CD pipeline for final builds

### 🔧 Docker Commands Quick Reference
```bash
# Setup and status
./docker-dev.sh setup      # Initial setup
./docker-dev.sh status     # Check status
./docker-dev.sh help       # Show all commands

# Development
./docker-dev.sh dev        # Start development container
./docker-dev.sh build-debug  # Build debug APK
./docker-dev.sh test       # Run tests

# Maintenance
./docker-dev.sh cleanup    # Clean unused resources
./docker-dev.sh rebuild    # Rebuild from scratch
./docker-dev.sh stats      # Monitor performance
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Choose appropriate build environment:
   - Apple Silicon Mac: Use host system
   - Other platforms: Docker recommended
4. Commit your changes (`git commit -m 'Add some amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

### Development Environment Guidelines
- **Code Style**: Follow existing patterns and lint rules
- **Testing**: Add tests for new features
- **Documentation**: Update docs for significant changes
- **Build Verification**: Test on your platform before submitting

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- German sentence data curated for language learning
- Material Design 3 guidelines from Google
- Android Jetpack libraries for modern development
- Docker community for containerization best practices
- Community feedback and suggestions

## 📞 Support

If you encounter any issues or have questions:

### Platform-Specific Help
- **Apple Silicon Mac users**: Use host system builds, see build issues section
- **Docker users**: Check [Docker Guide](DOCKER_GUIDE.md) and [Installation Guide](DOCKER_INSTALLATION_GUIDE.md)
- **General development**: See [Project Requirements Document](PRD.md)

### Getting Help
- Open an issue on GitHub
- Check the [documentation](docs/)
- Review platform-specific guides
- Contact: erdisdriza@gmail.com

## 🚀 Project Status

- ✅ **Production Ready** - Comprehensive Android application
- ✅ **Docker Infrastructure** - Full CI/CD pipeline (90% compatibility)
- ✅ **Cross-Platform** - Supports multiple development environments
- ✅ **Well Documented** - Extensive documentation and guides
- ✅ **Quality Assured** - Automated testing and linting
- ✅ **Modern Architecture** - MVVM with Jetpack Compose

---

**Happy Learning! 🎓📱**

Made with ❤️ for German language learners

*Leverage Docker for team collaboration and CI/CD, use host builds for optimal Apple Silicon performance* 