# Locale Testing Guide

## Overview

Test ML Kit translation by switching between 59 supported languages using the built-in `LocaleHelper`, without changing device settings.

## Supported Languages

All 59 languages from [ML Kit Translation API](https://developers.google.com/ml-kit/language/translation/translation-language-support):

| Language | Code | Native Name |
|----------|------|-------------|
| Afrikaans | af | Afrikaans |
| Albanian | sq | Shqip |
| Arabic | ar | العربية |
| Belarusian | be | Беларуская |
| Bengali | bn | বাংলা |
| Bulgarian | bg | Български |
| Catalan | ca | Català |
| Chinese | zh | 中文 |
| Croatian | hr | Hrvatski |
| Czech | cs | Čeština |
| Danish | da | Dansk |
| Dutch | nl | Nederlands |
| English | en | English |
| Esperanto | eo | Esperanto |
| Estonian | et | Eesti |
| Finnish | fi | Suomi |
| French | fr | Français |
| Galician | gl | Galego |
| Georgian | ka | ქართული |
| German | de | Deutsch |
| Greek | el | Ελληνικά |
| Gujarati | gu | ગુજરાતી |
| Haitian | ht | Kreyòl |
| Hebrew | he | עברית |
| Hindi | hi | हिंदी |
| Hungarian | hu | Magyar |
| Icelandic | is | Íslenska |
| Indonesian | id | Bahasa Indonesia |
| Irish | ga | Gaeilge |
| Italian | it | Italiano |
| Japanese | ja | 日本語 |
| Kannada | kn | ಕನ್ನಡ |
| Korean | ko | 한국어 |
| Latvian | lv | Latviešu |
| Lithuanian | lt | Lietuvių |
| Macedonian | mk | Македонски |
| Malay | ms | Bahasa Melayu |
| Maltese | mt | Malti |
| Marathi | mr | मराठी |
| Norwegian | no | Norsk |
| Persian | fa | فارسی |
| Polish | pl | Polski |
| Portuguese | pt | Português |
| Romanian | ro | Română |
| Russian | ru | Русский |
| Slovak | sk | Slovenčina |
| Slovenian | sl | Slovenščina |
| Spanish | es | Español |
| Swahili | sw | Kiswahili |
| Swedish | sv | Svenska |
| Tagalog | tl | Tagalog |
| Tamil | ta | தமிழ் |
| Telugu | te | తెలుగు |
| Thai | th | ไทย |
| Turkish | tr | Türkçe |
| Ukrainian | uk | Українська |
| Urdu | ur | اردو |
| Vietnamese | vi | Tiếng Việt |
| Welsh | cy | Cymraeg |

## How to Use

1. **Select Language**: Choose from the dropdown (shows English and native names)
2. **Apply**: Click "Apply Locale" (saves preference, updates locale, restarts app)
3. **Verify**: After restart, English propositions translate to the selected language

## Testing Flow

1. Launch app (English) → Fetch propositions → Note English content
2. Switch language → App restarts → Fetch again → Verify translation

**First Use**: Model downloads over Wi-Fi (5-30 seconds, ~30MB)  
**Subsequent**: Cached model loads instantly

**Log Verification**:
```
Enabling translation - starting background initialization
Detected locale - App default: es-ES, Using: es-ES, Language code: es
Translation model for language 'es' is already cached / starting download
Proposition translation enabled for language: es
```

## Recommended Test Languages

| Language | Code | Test Case |
|----------|------|-----------|
| Spanish | es | Common US second language |
| French/German | fr/de | European markets |
| Japanese/Korean/Chinese | ja/ko/zh | Complex characters |
| Arabic | ar | Right-to-left text |
| Hindi | hi | Devanagari script |
| Russian | ru | Cyrillic script |

## Troubleshooting

| Issue | Solutions |
|-------|-----------|
| **Locale not changing** | Click "Apply Locale" → Verify restart → Check logs → Force-stop app |
| **Translation not working** | Enable Wi-Fi → Check storage (~30MB/language) → Update Google Play Services → Check logs |
| **Model download fails** | Connect to Wi-Fi (required) → Verify storage → Update Play Services → Try different language |

## Programmatic Usage

```kotlin
// Set locale
LocaleHelper.setLocale(context, "es")
LocaleHelper.restartApp(context)

// Get current locale
val currentLanguage = LocaleHelper.getCurrentLocale(context)

// Get all available locales
val allCodes = LocaleHelper.getAllLocaleCodes()  // ["af", "sq", "ar", ...]
val allNames = LocaleHelper.getAllLocaleNames()  // ["Afrikaans", "Albanian", ...]
```

## Performance Notes

- **Model Size**: ~30MB per language
- **Storage**: Models cached between sessions (clear app data to free space)
- **Network**: Wi-Fi required for downloads (enforced by ML Kit)

## Resources

- [ML Kit Translation Docs](https://developers.google.com/ml-kit/language/translation/android)
- [Supported Languages](https://developers.google.com/ml-kit/language/translation/translation-language-support)
- [BCP-47 Language Tags](https://en.wikipedia.org/wiki/IETF_language_tag)

