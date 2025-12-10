# Translation Model Cache Management

## Overview

Automatic cache management with **max 10 models** using LRU (Least Recently Used) eviction.

## Why?

- **Model Size:** ~30MB each
- **Without Limit:** 59 languages = ~1.77GB
- **With Limit:** 10 models = ~300MB (83% savings)

## How It Works

### LRU Eviction

1. **Track Access:** Timestamp recorded when model accessed/downloaded
2. **Check Limit:** Before download, check if 10+ models cached
3. **Remove Oldest:** Delete least recently used model to make room
4. **Persist:** Timestamps stored in SharedPreferences

### Example

```
Cache (10/10): [en, es, fr, de, it, pt, ja, ko, ar, hi]
Switch to Chinese (zh):
  1. Check: zh not in cache
  2. At limit: 10/10
  3. Find LRU: 'ar' (oldest)
  4. Delete 'ar' (~30MB freed)
  5. Download 'zh' (~30MB)
Result: [en, es, fr, de, it, pt, ja, ko, hi, zh] (10/10)
```

## API

```kotlin
// In TranslationModelCacheManager
fun recordModelAccess(languageCode: String)
suspend fun cleanupOldModelsIfNeeded(newLanguageCode: String): Boolean
fun getTrackedModels(): Map<String, Long>
fun clearAllTracking()
```

## Configuration

```kotlin
companion object {
    private const val MAX_CACHED_MODELS = 10  // Adjust if needed
}
```

**Considerations:**
- Lower (5-7): Saves storage, more downloads
- Higher (15-20): Fewer downloads, more storage
- Recommended: 10 (good balance)

## Storage Format

SharedPreferences (`aep_translation_model_cache`):
```
model_es → 1704123456789 (timestamp)
model_fr → 1704123457890
model_de → 1704123458991
```

## Logging

```
DEBUG: Current cached models count: 10, max: 10
DEBUG: Cache limit reached. Removing 1 oldest model(s)
DEBUG: Removed old translation model: ar (1/1)
DEBUG: Recorded access for translation model: zh
```

## Testing

### Check Cache State
```bash
adb shell run-as com.adobe.marketing.mobile.messagingsample \
  cat shared_prefs/aep_translation_model_cache.xml
```

### Clear Cache
```bash
adb shell run-as com.adobe.marketing.mobile.messagingsample \
  rm shared_prefs/aep_translation_model_cache.xml
```

## Performance

| Scenario | Time |
|----------|------|
| First launch (download) | 5-30s |
| Cached launch | ~15-55ms |
| Cache eviction | ~100-500ms + download |

## Edge Cases

- **Protected Model:** Won't delete model currently being downloaded
- **Cleanup Failures:** Logged, download proceeds anyway
- **Thread Safety:** Single background thread access (no concurrency issues)

## Best Practices

- Monitor logs for frequent evictions
- Test with 10+ languages to verify eviction
- Clear cache between test runs for consistency
- Adjust limit based on use case (dev: 10, production: 5-7)

---

For implementation details, see `TranslationModelCacheManager.kt`.
