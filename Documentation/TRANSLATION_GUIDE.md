# Proposition Content Translation Guide

## Overview

The Messaging SDK includes automatic translation capabilities using [Google ML Kit Translation API](https://developers.google.com/ml-kit/language/translation/android). This feature automatically translates English proposition content (in-app messages, content cards, code-based experiences) into the device's native language.

## Features

- **Automatic Language Detection**: Detects the device's locale and determines if translation is needed
- **Selective Field Translation**: Only translates the `content` field within proposition item data
- **Multiple Content Type Support**: Translates text in HTML content, JSON content, and plain text
- **Smart Text Filtering**: Only translates plain text segments without newlines (preserves formatted text)
- **HTML Tag Preservation**: Maintains HTML structure and tags while translating text content
- **Recursive Translation**: Translates nested objects and arrays within the content field
- **Metadata Preservation**: Keeps IDs, schemas, and other metadata fields unchanged
- **Efficient Model Management**: Downloads translation models only when needed (over Wi-Fi)

## Requirements

- Android API level 21 or above
- ML Kit Translation library: `com.google.mlkit:translate:17.0.3`
- Wi-Fi connection (for initial model download)

## How It Works

1. **Async Initialization**: When enabled, initialization happens on a background thread with a small delay (500ms) to ensure Google Play Services is ready
2. **Locale Detection**: Detects the device language using `Locale.toLanguageTag()` (returns BCP 47 tag like "es-ES", "fr-FR") and extracts the language code ("es", "fr")
3. **Model Check**: Checks if the translation model for the detected language is already cached on device
4. **Model Download**: If not cached, downloads the appropriate translation model (~30MB) over Wi-Fi for the detected language
5. **Translation**: Automatically translates proposition content when:
   - Device language is not English (language code != "en")
   - Translation model is available (cached or downloaded successfully)
   - The target language is supported by ML Kit

**Model Caching:** Translation models are cached on device after the first download. Subsequent app launches will use the cached model, resulting in faster initialization (typically < 1 second vs 5-30 seconds for initial download).

**Async Initialization:** Translation initialization runs asynchronously to:
- Avoid blocking the main thread during app startup
- Give Google Play Services time to initialize properly
- Prevent race conditions with ML Kit model management

## Supported Languages

ML Kit Translation supports translation between English and 59 languages. The complete list includes:

**European Languages:**
Afrikaans, Albanian, Belarusian, Bulgarian, Catalan, Croatian, Czech, Danish, Dutch, English, Esperanto, Estonian, Finnish, French, Galician, German, Greek, Hungarian, Icelandic, Irish, Italian, Latvian, Lithuanian, Macedonian, Maltese, Norwegian, Polish, Portuguese, Romanian, Russian, Slovak, Slovenian, Spanish, Swedish, Turkish, Ukrainian, Welsh

**Asian Languages:**
Bengali, Chinese, Georgian, Gujarati, Hebrew, Hindi, Indonesian, Japanese, Kannada, Korean, Malay, Marathi, Persian, Tagalog, Tamil, Telugu, Thai, Urdu, Vietnamese

**Other Languages:**
Arabic, Haitian, Swahili

**Total:** 59 languages supported

See the [official ML Kit Supported Languages documentation](https://developers.google.com/ml-kit/language/translation/translation-language-support) for BCP-47 language codes and the most up-to-date list.

## Usage

### Enabling Translation

To enable automatic translation of propositions, call `enablePropositionTranslation()` on the `EdgePersonalizationResponseHandler` instance:

```java
// Access the EdgePersonalizationResponseHandler
EdgePersonalizationResponseHandler responseHandler = messagingExtension.getResponseHandler();

// Enable translation
responseHandler.enablePropositionTranslation();
```

**Important**: Call this method early in your application lifecycle to allow time for the translation model to download.

### Disabling Translation

To disable translation and free up resources:

```java
responseHandler.disablePropositionTranslation();
```

### Translating Individual Proposition Items

You can also manually translate a specific proposition item:

```java
PropositionItem originalItem = proposition.getItems().get(0);
PropositionItem translatedItem = responseHandler.translatePropositionItem(originalItem);
```

## What Gets Translated?

The translation utility specifically targets the **`content` field** within a `PropositionItem`'s `itemData`. This selective approach ensures that:

- ✅ **Content field is translated**: The actual user-facing content is localized
- ❌ **Metadata remains unchanged**: IDs, schemas, tracking data, and other metadata fields are preserved
- ❌ **Structure is maintained**: The proposition item structure and non-content fields remain intact

**Example PropositionItem Structure:**
```json
{
  "id": "abc123",
  "schema": "https://ns.adobe.com/personalization/html-content-item",
  "data": {
    "id": "message-001",
    "content": "<h1>Welcome!</h1>",  // ← Only this field is translated
    "format": "text/html",
    "characteristics": {
      "mobile": true
    }
  }
}
```

After translation, only the `content` field value changes; all other fields remain exactly as they were.

### What Text Gets Translated?

Within the `content` field, the translation utility applies smart filtering:

- ✅ **Plain text without newlines**: Single-line text is translated
- ✅ **Text within HTML tags**: Text between tags is translated (e.g., `<p>Hello</p>`)
- ❌ **Text with newlines**: Multi-line text blocks are preserved as-is
- ❌ **Very short strings**: Strings less than 2 characters are skipped
- ❌ **Empty or whitespace-only text**: Preserved unchanged

**Why skip text with newlines?**  
Text containing newlines often represents formatted code, structured data, or pre-formatted content that should not be translated to maintain its intended format.

## Content Translation Examples

### HTML Content

**Original English Content:**
```html
<h1>Welcome!</h1>
<p>Thank you for using our app.</p>
<button>Get Started</button>
```

**Translated to Spanish:**
```html
<h1>¡Bienvenido!</h1>
<p>Gracias por usar nuestra aplicación.</p>
<button>Comenzar</button>
```

### JSON Content

**Original English Content:**
```json
{
  "title": "Special Offer",
  "body": "Get 20% off your first purchase!",
  "actionTitle": "Shop Now",
  "metadata": {
    "category": "promotional",
    "priority": 1
  }
}
```

**Translated to French:**
```json
{
  "title": "Offre spéciale",
  "body": "Obtenez 20% de réduction sur votre premier achat!",
  "actionTitle": "Acheter maintenant",
  "metadata": {
    "category": "promotional",
    "priority": 1
  }
}
```

Note: Numeric values and non-text fields remain unchanged.

## Best Practices

### 1. Enable Early in Lifecycle

Enable translation during application startup to ensure the model is downloaded before propositions are received:

```java
@Override
public void onCreate() {
    super.onCreate();
    
    // Initialize Messaging SDK
    MobileCore.registerExtensions(
        Arrays.asList(Messaging.EXTENSION),
        o -> Log.debug("Extensions", "Registered")
    );
    
    // Enable translation
    // Note: You'll need to access the handler through the MessagingExtension
    responseHandler.enablePropositionTranslation();
}
```

### 2. Wi-Fi Recommendation

Translation models are ~30MB. The SDK automatically requires Wi-Fi for downloads to avoid using mobile data:

```java
// The SDK uses this internally:
DownloadConditions conditions = new DownloadConditions.Builder()
    .requireWifi()
    .build();
```

### 3. Handle Translation Failures Gracefully

If translation fails (no internet, unsupported language, etc.), the original English content is displayed:

```java
// The SDK handles this automatically - no action needed
// Original content is always preserved as fallback
```

### 4. Testing Different Locales

To test translation with different device locales:

```java
// On Android device/emulator:
// Settings → System → Languages & input → Languages
// Add and select the desired language
```

Or use ADB:
```bash
# Set device to Spanish
adb shell "setprop persist.sys.locale es-ES; stop; start"

# Set device to French  
adb shell "setprop persist.sys.locale fr-FR; stop; start"
```

## Technical Details

### Language Detection

The translation utility uses a **dual-source approach** to detect the device language:

1. **App Locale** (via `Locale.getDefault()`): Checks the app's current locale first
   - This is what gets set when using `LocaleHelper` in the test app
   - Set programmatically via `Locale.setDefault()`
   
2. **System Locale** (via `ServiceProvider`): Falls back to system device locale
   - This is the locale set in device Settings

The app locale takes precedence, allowing the app to override the system locale for testing purposes.

**Method**: `Locale.toLanguageTag()` returns BCP 47 language tag (e.g., "es-ES", "fr-FR", "en-US")
**Extraction**: Splits the tag on "-" and takes the first part (e.g., "es-ES" → "es")

**Example:**
```kotlin
// If LocaleHelper.setLocale(context, "es") was called:
Locale.getDefault().toLanguageTag()      // Returns "es-ES" ✅ (Used first)

// System locale might still be:
ServiceProvider...activeLocale.toLanguageTag()  // Returns "en-US" (ignored)

// Extracted language code: "es"
```

**With debug logging:**
```
Detected locale - App default: es-ES, System locale: en-US, Using: es-ES, Language code: es
Translation model downloaded successfully for language: es
```

The extracted language code is then passed to ML Kit's `TranslateLanguage.fromLanguageTag()` to create the appropriate translator.

## Performance Considerations

### Model Download
- **Size**: ~30MB per language pair
- **Network**: Wi-Fi only (enforced by SDK)
- **Time**: 5-30 seconds depending on connection speed
- **Storage**: Models are cached on device

### Translation Speed
- **Plain Text**: ~100-500ms per text segment
- **HTML Content**: Slightly longer due to tag parsing
- **Timeout**: 30 seconds per translation (configurable in code)

### Resource Management

The translation model remains in memory while enabled. Disable when not needed:

```java
// Disable during background operations
@Override
public void onPause() {
    super.onPause();
    responseHandler.disablePropositionTranslation();
}

// Re-enable when returning to foreground
@Override
public void onResume() {
    super.onResume();
    responseHandler.enablePropositionTranslation();
}
```

## Limitations

1. **Source Language**: Currently only translates from English to other languages
2. **Model Size**: Each language model is ~30MB
3. **Network Required**: Initial download requires internet connection
4. **Translation Quality**: Depends on ML Kit's translation accuracy (may not be perfect for all contexts)
5. **Processing Time**: Real-time translation adds latency (typically <1 second)

## Troubleshooting

### Translation Not Working

**Check logs for these messages:**

```
Translation disabled: Device language is already English.
Translation disabled: Unsupported device language: [code]
Failed to download translation model: [error]
```

**Common solutions:**
- Verify device is not set to English locale
- Ensure Wi-Fi is enabled
- Check that the device language is supported by ML Kit
- Verify the ML Kit dependency is included in your build.gradle

### Model Download Fails

If the model download fails:
1. Check internet connection
2. Ensure Wi-Fi is enabled (required for download)
3. Verify sufficient storage space (~30MB per model)
4. Check for Google Play Services availability

### Unexpected Translation Results

If translations seem incorrect:
1. ML Kit uses machine learning models that may not be perfect for all contexts
2. Consider adding custom string resources for critical UI elements
3. Use the translation utility as a fallback for dynamic content only

### Verifying Language Detection

If translation isn't working, check the logs to see what language is being detected:

```
Look for:
"Detected locale - App default: es-ES, System locale: en-US, Using: es-ES, Language code: es"
```

**Understanding the log output:**
- **App default**: Locale set via `Locale.setDefault()` (e.g., by `LocaleHelper`)
- **System locale**: Device system locale from Settings
- **Using**: The locale actually used for translation (app default takes precedence)
- **Language code**: Extracted language code passed to ML Kit

**If the language code is always "en" despite changing locale:**

1. **Using LocaleHelper in test app:**
   - Make sure you're calling `LocaleHelper.restartApp(context)` after changing locale
   - The app must restart for `Locale.getDefault()` to update

2. **Using system Settings:**
   - Go to Settings → System → Languages & input → Languages
   - Make sure the desired language is at the TOP of the list
   - Restart your app completely (force stop and relaunch)

3. **Debug the locale sources:**
   ```kotlin
   // Check what both sources are returning:
   Log.d("Translation", "App locale: ${Locale.getDefault().toLanguageTag()}")
   val systemLocale = ServiceProvider.getInstance().deviceInfoService.activeLocale
   Log.d("Translation", "System locale: ${systemLocale?.toLanguageTag()}")
   ```

4. **Common issues:**
   - App not restarted after locale change
   - `Locale.setDefault()` not being called
   - App caching old locale - try clearing app data

### Verifying Model Caching

To verify that translation models are being cached and reused:

**First app launch (model not cached):**
```
Enabling translation - starting background initialization
Detected locale - App default: es-ES, System locale: es-ES, Using: es-ES, Language code: es
Starting model check/download for language: es
Checking if model is downloaded...
Model check completed: isDownloaded = false
Translation model not found, starting download for language: es
Translation model downloaded successfully for language: es
Initialization completed: result = true, isTranslationEnabled = true
Proposition translation enabled for language: es
```

**Subsequent app launches (model cached):**
```
Enabling translation - starting background initialization
Detected locale - App default: es-ES, System locale: es-ES, Using: es-ES, Language code: es
Starting model check/download for language: es
Checking if model is downloaded...
Model check completed: isDownloaded = true
Translation model for language 'es' is already cached
Initialization completed: result = true, isTranslationEnabled = true
Proposition translation enabled for language: es
```

**If you see errors:**
```
Error checking if model is downloaded: [error message]
Model download failed: [error message]
Failed during initialization: [error message with stack trace]
```

**Note:** If you continue to see "downloading" messages on every app launch, or if initialization fails silently:
- Check the detailed logs above for specific error messages
- Ensure sufficient device storage space
- Verify app has storage permissions
- Confirm Google Play Services is up to date and working
- Try clearing app data and reinstalling

## Example Implementation

Here's a complete example of enabling translation in a messaging application:

```java
public class MessagingApplication extends Application {
    
    private EdgePersonalizationResponseHandler responseHandler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Register Messaging extension
        MobileCore.setApplication(this);
        MobileCore.registerExtensions(
            Arrays.asList(
                Messaging.EXTENSION,
                Edge.EXTENSION
            ),
            o -> {
                Log.debug("App", "Extensions registered");
                initializeTranslation();
            }
        );
    }
    
    private void initializeTranslation() {
        // Get the response handler from MessagingExtension
        // Note: This requires accessing the internal handler
        // through the extension's API
        
        // Enable translation for all proposition content
        responseHandler.enablePropositionTranslation();
        
        // Check if translation was successfully enabled
        if (responseHandler.isPropositionTranslationEnabled()) {
            Log.debug("App", "Translation enabled successfully");
        } else {
            Log.debug("App", "Translation not enabled - using English content");
        }
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // Free up resources during low memory conditions
        if (responseHandler != null) {
            responseHandler.disablePropositionTranslation();
        }
    }
}
```

## API Reference

### EdgePersonalizationResponseHandler

#### `void enablePropositionTranslation()`
Enables automatic translation of proposition content from English to the device's native language.

**Returns:** void

**Side Effects:**
- Downloads translation model if needed
- Initializes translator
- Sets translation enabled flag

#### `void disablePropositionTranslation()`
Disables automatic translation and releases resources.

**Returns:** void

**Side Effects:**
- Closes translator
- Frees translation model from memory
- Clears translation enabled flag

#### `PropositionItem translatePropositionItem(PropositionItem propositionItem)`
Manually translates a single proposition item.

**Parameters:**
- `propositionItem`: The PropositionItem to translate

**Returns:** A new PropositionItem with translated content, or the original if translation is disabled or fails

### PropositionTranslationUtility

#### `boolean initialize()`
Initializes the translation utility.

**Returns:** true if translation is enabled and ready, false otherwise

#### `void cleanup()`
Releases resources used by the translator.

#### `boolean isTranslationEnabled()`
Checks if translation is currently enabled.

**Returns:** true if enabled, false otherwise

#### `String getTargetLanguageCode()`
Gets the target language code being used for translation.

**Returns:** Language code (e.g., "es", "fr", "de") or null if not enabled

## Additional Resources

- [Google ML Kit Translation Documentation](https://developers.google.com/ml-kit/language/translation/android)
- [ML Kit Supported Languages](https://developers.google.com/ml-kit/language/translation/translation-language-support)
- [Adobe Experience Platform Mobile SDK Documentation](https://developer.adobe.com/client-sdks/documentation/)

