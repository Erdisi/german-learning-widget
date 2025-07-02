# AI CONTEXT GUIDE FOR GERMAN LEARNING WIDGET

**Version:** 1.0  
**Last Updated:** December 2024  
**Optimized for:** Claude 4 Sonnet (Max Mode)

---

## **PURPOSE**

This guide provides a systematic approach for AI assistance on the German Learning Widget project, ensuring consistent, professional, production-level results through optimized context management.

---

## **RAPID ASSESSMENT TIER (0-30 seconds)**
*For quick questions, use parallel tool calls to scan these simultaneously:*
- `app/src/main/java/com/germanleraningwidget/MainActivity.kt` (lines 1-100) - Architecture overview
- `app/src/main/java/com/germanleraningwidget/GermanLearningApplication.kt` (lines 1-100) - App initialization
- `app/build.gradle.kts` (entire file) - Dependencies and configuration
- `app/src/main/AndroidManifest.xml` (entire file) - App structure

---

## **CORE CONTEXT MATRIX** *(Optimized for parallel loading)*

### **TIER 1A: CRITICAL ARCHITECTURE** *(Load in parallel - always complete files)*
1. `app/src/main/java/com/germanleraningwidget/MainActivity.kt` - Navigation, lifecycle, theme orchestration
2. `app/src/main/java/com/germanleraningwidget/GermanLearningApplication.kt` - DI setup, initialization, performance monitoring  
3. `app/src/main/java/com/germanleraningwidget/di/AppModule.kt` - Dependency graph, repository patterns
4. `app/src/main/java/com/germanleraningwidget/data/model/GermanSentence.kt` - Core data contracts

### **TIER 1B: PRIMARY BUSINESS LOGIC** *(Load in parallel with 1A)*
5. `app/src/main/java/com/germanleraningwidget/data/repository/SentenceRepository.kt` - Data layer, business rules
6. `app/src/main/java/com/germanleraningwidget/widget/GermanLearningWidget.kt` - Primary feature implementation
7. `app/src/main/java/com/germanleraningwidget/data/model/UserPreferences.kt` - State management, user contracts

---

## **SPECIALIZED CONTEXT CLUSTERS** *(Load based on task domain)*

### **WIDGET ECOSYSTEM** *(For widget-related tasks)*
- `app/src/main/java/com/germanleraningwidget/widget/WidgetCustomizationHelper.kt` - Theming engine
- `app/src/main/java/com/germanleraningwidget/widget/BookmarksWidget.kt` - Bookmarks widget variant
- `app/src/main/java/com/germanleraningwidget/widget/BookmarksHeroWidget.kt` - Hero widget variant
- `app/src/main/java/com/germanleraningwidget/data/model/WidgetCustomization.kt` - Widget contracts
- `app/src/main/java/com/germanleraningwidget/util/AutoTextSizer.kt` - Widget utilities

### **DATA & STATE MANAGEMENT** *(For data flow/persistence tasks)*
- `app/src/main/java/com/germanleraningwidget/data/repository/UserPreferencesRepository.kt` - Settings persistence
- `app/src/main/java/com/germanleraningwidget/data/repository/WidgetCustomizationRepository.kt` - Widget state
- `app/src/main/java/com/germanleraningwidget/data/repository/AppSettingsRepository.kt` - App-level state
- `app/src/main/java/com/germanleraningwidget/worker/SentenceDeliveryWorker.kt` - Background sync

### **UI & EXPERIENCE** *(For UI/UX tasks)*
- `app/src/main/java/com/germanleraningwidget/ui/screen/HomeScreen.kt` - Main experience
- `app/src/main/java/com/germanleraningwidget/ui/screen/SettingsScreen.kt` - Settings experience
- `app/src/main/java/com/germanleraningwidget/ui/screen/BookmarksScreen.kt` - Bookmarks experience
- `app/src/main/java/com/germanleraningwidget/ui/theme/Theme.kt` - Design system
- `app/src/main/java/com/germanleraningwidget/ui/screen/OnboardingScreen.kt` - User journey

### **SYSTEM & INFRASTRUCTURE** *(For architecture/performance tasks)*
- `app/src/main/java/com/germanleraningwidget/util/DebugUtils.kt` - Debug utilities
- `app/src/main/java/com/germanleraningwidget/util/OptimizationUtils.kt` - Performance utilities
- `app/src/main/java/com/germanleraningwidget/util/AppConstants.kt` - Configuration contracts
- `gradle/libs.versions.toml` - Dependency management

---

## **CLAUDE-SPECIFIC OPTIMIZATION INSTRUCTIONS**

### **1. PARALLEL CONTEXT LOADING STRATEGY**
**ALWAYS use parallel tool calls for:**
- Initial context gathering (4-6 files simultaneously)
- Related file clusters (e.g., all widget files together)
- Cross-cutting concerns (repositories, models, utilities)

**NEVER load sequentially unless:**
- One file's content determines which others to read
- Following specific error traces
- Deep-diving into implementation details

### **2. ADAPTIVE CONTEXT DEPTH**
**RAPID SCAN MODE** (First 50-100 lines):
- Quick architecture questions
- Feature availability checks
- Dependency verification

**FOCUSED READ MODE** (Specific sections):
- Implementation details
- Bug investigation
- Code review scenarios

**COMPLETE ANALYSIS MODE** (Entire files):
- Major refactoring
- Security reviews
- Performance optimization

### **3. INTELLIGENT CONTEXT EXPANSION**
**AUTO-EXPAND when detecting:**
- Missing dependency context (auto-load related repositories)
- Incomplete data flow understanding (load model + repository + UI)
- Widget customization issues (load full widget ecosystem)
- Performance concerns (load utilities + workers + core logic)

**SEARCH-FIRST for:**
- Specific function/class references
- Error messages or stack traces
- Configuration values
- Design patterns implementation

### **4. PRODUCTION-LEVEL QUALITY GATES**
**BEFORE providing solutions, ensure complete understanding of:**
- ✅ Complete data flow understanding
- ✅ Error handling patterns context
- ✅ Performance implications awareness
- ✅ Testing implications (check for test files)
- ✅ Configuration dependencies verified
- ✅ Security considerations understood

**ESCALATION TRIGGERS** (request more context):
- Solution involves >3 files not yet reviewed
- Performance implications unclear
- Breaking changes potential
- Security-sensitive operations

### **5. AUTONOMOUS OPTIMIZATION METRICS**
**TRACK SUCCESS INDICATORS:**
- First-response completeness (no follow-up file requests needed)
- Solution accuracy (no corrections required)
- Context efficiency (minimal files for maximum insight)
- Proactive issue identification (spotting related problems)

**ADAPTATION TRIGGERS:**
- >2 follow-up context requests = insufficient initial context
- Multiple correction rounds = missing critical files
- Performance questions without utility context = expand infrastructure tier
- UI questions without theme context = include design system

---

## **TASK-SPECIFIC CONTEXT PRESETS**

### **"WIDGET_FULL"**
Tiers 1A+1B + Widget Ecosystem + System Utils
*Use for: Widget functionality, customization, theming issues*

### **"DATA_FLOW"** 
Tiers 1A+1B + Data Management + relevant UI screens
*Use for: Repository issues, data persistence, state management*

### **"ARCHITECTURE"**
Tiers 1A+1B + System Infrastructure + build configs
*Use for: Structural changes, dependency updates, major refactoring*

### **"BUG_HUNT"**
Rapid scan + targeted search + debug utilities + error logs
*Use for: Debugging, error investigation, troubleshooting*

### **"PERFORMANCE"**
Core logic + System Infrastructure + Workers + optimization utils
*Use for: Performance optimization, memory issues, background tasks*

### **"SECURITY"**
Complete architecture + manifest + repositories + settings
*Use for: Security reviews, permission changes, data protection*

---

## **CONVERSATION CONTINUITY**

### **MAINTAIN CONTEXT AWARENESS:**
- Track which files have been reviewed in conversation
- Note any architectural assumptions made
- Remember discovered patterns or issues
- Build on previous context rather than re-requesting

### **PROACTIVE CONTEXT MANAGEMENT:**
- Suggest context updates when architecture changes discussed
- Flag when discussed changes would affect unreviewed files
- Recommend tier adjustments based on conversation patterns

---

## **USAGE EXAMPLES**

### **Quick Question Example:**
"How does the widget update system work?"
→ Load: Rapid Assessment Tier + GermanLearningWidget.kt + SentenceDeliveryWorker.kt

### **Feature Development Example:**
"Add new widget customization option"
→ Load: WIDGET_FULL preset

### **Bug Investigation Example:**
"Widget not updating with new sentences"
→ Load: BUG_HUNT preset + specific widget files

### **Performance Review Example:**
"App is slow on startup"
→ Load: PERFORMANCE preset + GermanLearningApplication.kt

---

## **MAINTENANCE INSTRUCTIONS**

### **QUARTERLY REVIEW CHECKLIST:**
- [ ] Verify all file paths are current
- [ ] Update tier assignments based on usage patterns
- [ ] Add new critical files to appropriate tiers
- [ ] Remove deprecated/deleted files
- [ ] Update task-specific presets based on common patterns
- [ ] Review and update optimization metrics

### **EVOLUTION TRIGGERS:**
- Major architectural changes
- New feature additions requiring new file patterns
- Performance optimization results showing new critical paths
- User feedback on AI assistance quality

---

**Remember:** This guide is a living document. Update it as the project evolves to maintain optimal AI assistance quality. 