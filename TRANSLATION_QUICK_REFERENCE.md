# Translation Quick Reference

## Quick Start

```kotlin
// 1. Dependency (already added)
implementation("com.google.mlkit:translate:17.0.3")

// 2. Enable translation
handler.enablePropositionTranslation()

// 3. Done - automatic translation for all propositions
```

## API

```java
handler.enablePropositionTranslation();    // Enable
handler.disablePropositionTranslation();   // Disable
handler.isPropositionTranslationEnabled(); // Check status
handler.translatePropositionItem(item);    // Manual translation
```

## What Gets Translated?

**Only `itemData.content` field**. All other fields unchanged.

| Translates | Skips |
|-----------|-------|
| Plain text | Text with newlines |
| HTML text (tags preserved) | Strings < 2 chars |
| JSON strings (structure preserved) | IDs, schemas, metadata |
| | Numbers, booleans |

## Language Support

- **Detection**: App locale first, then system locale
- **Supported**: [59 languages](https://developers.google.com/ml-kit/language/translation/translation-language-support)
- **English devices**: Automatically disabled
- **Fallback**: Original English if translation fails

## Performance

| Metric | Value |
|--------|-------|
| Model size | ~30MB |
| First download | 5-30s (Wi-Fi only) |
| Cached init | < 1s |
| Translation speed | 100-500ms/string |

## Important Notes

- Enable during app initialization
- Wi-Fi required for initial download
- Models cached after first download
- Auto-fallback to English on failure
- Disable during low memory

## Testing

**Change device language:**
```bash
adb shell "setprop persist.sys.locale es-ES; stop; start"  # Spanish
adb shell "setprop persist.sys.locale en-US; stop; start"  # English
```

**Key log messages:**
```
✅ "Translation model for language 'es' is already cached"  # Cached
✅ "Translation model downloaded successfully"              # Downloaded
❌ "Translation disabled: Device language is already English"
❌ "Model download failed: [error]"
```

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Not translating | Check device language |
| Download fails | Enable Wi-Fi |
| Slow first launch | Model downloading (wait 30s) |
| Wrong language | Check locale setting |

## Code Examples

**Basic setup:**
```java
MobileCore.registerExtensions(Arrays.asList(Messaging.EXTENSION), o -> {
    handler.enablePropositionTranslation();
});
```

**Memory management:**
```java
@Override
public void onLowMemory() {
    handler.disablePropositionTranslation();
}
```

## Translation Example

**Before (English):**
```json
{"content": "<h1>Welcome!</h1><button>Get Started</button>"}
```

**After (Spanish):**
```json
{"content": "<h1>¡Bienvenido!</h1><button>Comenzar</button>"}
```

## Resources

- [ML Kit Docs](https://developers.google.com/ml-kit/language/translation/android)
- [Supported Languages](https://developers.google.com/ml-kit/language/translation/translation-language-support)
- Full guide: `Documentation/TRANSLATION_GUIDE.md`

