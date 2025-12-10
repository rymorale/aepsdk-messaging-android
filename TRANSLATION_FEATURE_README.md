# Proposition Content Translation Feature

## Overview

Automatic translation of English proposition content (in-app messages, content cards, code-based experiences) to the device's native language using [Google ML Kit Translation API](https://developers.google.com/ml-kit/language/translation/android).

## New Components

### 1. PropositionTranslationManager (Kotlin)
Orchestrates translation operations with enable/disable, item translation, and state management.

**Location:** `code/messaging/src/main/java/com/adobe/marketing/mobile/messaging/PropositionTranslationManager.kt`

### 2. PropositionTranslationUtility (Kotlin)
Handles low-level translation: locale detection, ML Kit model management, recursive JSON translation, HTML-aware translation with tag preservation.

**Location:** `code/messaging/src/main/java/com/adobe/marketing/mobile/messaging/PropositionTranslationUtility.kt`

### 3. EdgePersonalizationResponseHandler (Enhanced)
New methods: `enablePropositionTranslation()`, `disablePropositionTranslation()`, `translatePropositionItem()`, `isPropositionTranslationEnabled()`

**Location:** `code/messaging/src/main/java/com/adobe/marketing/mobile/messaging/EdgePersonalizationResponseHandler.java`

### 4. Dependency
```kotlin
implementation("com.google.mlkit:translate:17.0.3")
```

## Key Features

- **Auto Language Detection**: Detects device locale (non-English only)
- **Selective Translation**: Only translates `content` field, preserves metadata
- **Smart Content Handling**: 
  - HTML: Translates text, preserves tags
  - JSON: Recursive string translation
  - Plain text: Direct translation
- **Efficient Model Management**: Downloads ~30MB model over Wi-Fi, caches on device
- **59+ Supported Languages**: See [ML Kit languages](https://developers.google.com/ml-kit/language/translation/translation-language-support)

## Quick Start

```java
// Enable in Application
MobileCore.registerExtensions(Arrays.asList(Messaging.EXTENSION), o -> {
    handler.enablePropositionTranslation();
});

// Manage resources
handler.disablePropositionTranslation(); // Disable when not needed
handler.isPropositionTranslationEnabled(); // Check status
```

## Translation Example

**Before (English):**
```json
{
  "title": "Special Offer",
  "body": "Get 20% off!",
  "price": 29.99
}
```

**After (French):**
```json
{
  "title": "Offre spéciale",
  "body": "Obtenez 20% de réduction!",
  "price": 29.99
}
```

## Performance

| Metric | Value |
|--------|-------|
| Model size | ~30MB |
| First download | 5-30s (Wi-Fi only) |
| Cached init | < 1s |
| Translation speed | 100-500ms/string |

## Best Practices

✅ **DO:**
- Enable early in app lifecycle
- Allow time for model download
- Disable during low memory
- Test with multiple locales

❌ **DON'T:**
- Enable/disable frequently
- Expect instant translation
- Force translation on English devices

## Testing

```bash
# Change device language
adb shell "setprop persist.sys.locale es-ES; stop; start"  # Spanish
adb shell "setprop persist.sys.locale en-US; stop; start"  # English
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Not translating | Check device language setting |
| No internet | Enable Wi-Fi for first download |
| Unsupported language | Check [supported languages](https://developers.google.com/ml-kit/language/translation/translation-language-support) |
| Model download failed | Verify Wi-Fi and storage space |

**Check logs:**
```java
MobileCore.setLogLevel(LoggingMode.VERBOSE);
```

## Limitations

1. **Source Language**: English only
2. **Model Size**: ~30MB per language
3. **Network**: Wi-Fi required for download
4. **Translation Quality**: ML-based, may not be perfect
5. **Processing Time**: Adds < 1s latency

## Resources

- [ML Kit Translation Docs](https://developers.google.com/ml-kit/language/translation/android)
- [Supported Languages](https://developers.google.com/ml-kit/language/translation/translation-language-support)
- [Complete Guide](./Documentation/TRANSLATION_GUIDE.md)

---

Copyright 2025 Adobe. Licensed under Apache License 2.0.
