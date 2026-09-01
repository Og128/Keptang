# Keptang

A local-first, voice-first expense tracker prototype for Android. Tap a home-screen widget,
speak an expense, and it's transcribed, parsed, and saved on-device — no cloud APIs, no LLMs,
no backend.

> **Build environment note:** this project was authored in an environment without the Android
> SDK, a JDK, or Gradle installed, so the commands below have not been executed here. Everything
> was written and hand-traced for correctness (the parser especially — see
> `ExpenseParserTest.kt`), but you should run the verification steps yourself in Android Studio
> before treating this as done. See "Verification checklist" below.

## Setup

1. Install **Android Studio Koala (2024.1)** or newer, with SDK Platform 34 and a device or
   emulator running **API 26+** (a physical device is strongly recommended — see
   "Device/API limitations" for why the emulator's speech recognizer is unreliable).
2. Open the project root in Android Studio and let it sync (it will download Gradle 8.7 and the
   Android Gradle Plugin automatically via the checked-in wrapper).
3. Alternatively, from the command line:
   ```
   ./gradlew assembleDebug        # macOS/Linux
   gradlew.bat assembleDebug      # Windows
   ```
4. Install on a device: `./gradlew installDebug`, or run from Android Studio.
5. On first launch, grant microphone (and, on Android 13+, notification) permission when
   prompted by the onboarding screen, then follow its instructions to add the widget: long-press
   the home screen → Widgets → **Keptang** → drag the microphone widget onto the home screen.

## Using it

- Tap the widget: recording starts immediately (haptic buzz confirms it), the mic/foreground
  service notification appears with **Stop** and **Cancel** actions.
- Speak naturally, e.g. *"Yesterday I spent 550 baht on dinner and 25 baht for coffee."*
- Recording stops automatically after ~1.5s of silence, at 60s regardless, or on a second widget
  tap / the notification's Stop action.
- A result notification appears (e.g. *"2 expenses added"*) with an **Undo** action. Tapping the
  notification opens the app to that capture's detail screen.
- The app itself (Inbox / Expenses / Review / Settings tabs) is only needed to review ambiguous
  captures, browse history, retry failures, or change settings — the widget never opens it.

## Testing

**Parser unit tests** (plain JVM, no device needed) — this is where the 8 required examples
from the spec live, plus a few extra cases (spoken numbers, explicit dates, PromptPay/bank
transfer, unmatched categories):
```
./gradlew test
```
Report: `app/build/reports/tests/testDebugUnitTest/index.html`

**Instrumented tests** (need a connected device/emulator) — a real in-memory Room database
exercising the full CAPTURED → TRANSCRIBING → PARSING → PROCESSED/NEEDS_REVIEW/FAILED pipeline
with `FakeTranscriptionProvider`, covering duplicate-processing prevention, multiple expenses
saved as separate rows, retained audio on failure, and idempotent retries:
```
./gradlew connectedAndroidTest
```
Report: `app/build/reports/androidTests/connected/index.html`

### Verification checklist (manual, on a device)

- [ ] `./gradlew assembleDebug` builds without errors.
- [ ] `./gradlew test` — all parser/silence-detector unit tests pass.
- [ ] `./gradlew connectedAndroidTest` — Room schema + capture pipeline tests pass.
- [ ] Force-stop the app, then tap the widget from a cold state — recording starts within
      ~1s and **the main activity never opens**.
- [ ] Speak an expense; confirm the mic indicator/notification appears and recording stops on
      silence.
- [ ] Turn on airplane mode / disable the device's speech recognizer, tap the widget, and speak —
      confirm the capture is kept as `FAILED` with its audio file intact (check the Inbox tab),
      not silently dropped and not sent anywhere.
- [ ] Open a `FAILED`/`NEEDS_REVIEW` capture and tap **Retry**; confirm no duplicate expenses
      appear in the Expenses tab afterward.
- [ ] Speak a multi-expense sentence (e.g. example 5 below) and confirm each expense appears as
      its own row.
- [ ] Speak an amount-free sentence and confirm it lands in the Review tab / shows
      "Could not understand" rather than inventing a value.
- [ ] Watch the recording notification disappear (and check `adb shell dumpsys activity
      services com.keptang`) after a capture finishes, to confirm the foreground service always
      stops itself.

## Architecture

```
widget/            VoiceCaptureWidgetProvider — RemoteViews mic button, PendingIntent targets
                    the service directly (getForegroundService), never MainActivity.
capture/           VoiceCaptureService (foreground service, orchestrates one capture),
                    AudioRecorderController (AudioRecord -> WAV, own read loop),
                    SilenceDetector (pure Kotlin amplitude/duration state machine),
                    WavFileWriter (44-byte PCM header), AudioFileStore (app-private files),
                    CaptureProcessor (CAPTURED -> ... -> PROCESSED|NEEDS_REVIEW|FAILED).
transcription/     TranscriptionProvider interface, AndroidSpeechRecognitionProvider
                    (wraps android.speech.SpeechRecognizer), FakeTranscriptionProvider
                    (deterministic double used by tests).
parser/            ExpenseParser and its helpers (AmountExtractor, DateExpressions,
                    CategoryRules, AccountExtractor, PaymentMethodExtractor, NumberWords,
                    ConfidenceScorer) — pure Kotlin/JVM, zero Android imports.
data/db/           Room entities (CaptureEntity, ExpenseEntity), DAOs, Converters, database.
data/repository/   CaptureRepository, ExpenseRepository, SettingsRepository (DataStore).
notification/      NotificationHelper (channels, recording/result notifications),
                    NotificationActionReceiver (Stop/Cancel/Undo).
di/                ServiceLocator — a small hand-rolled dependency container (no DI framework;
                    the graph is small and this keeps the parser/repositories trivially
                    testable without generated code).
ui/                Compose + Navigation-Compose: onboarding, inbox, expenses, review,
                    capture detail, settings.
```

### The capture pipeline, end to end

1. **Widget tap** → `PendingIntent.getForegroundService` starts `VoiceCaptureService` directly.
   Widget-click PendingIntents are one of the documented exemptions to Android 12+'s
   background-foreground-service-start restriction, so this works even from a fully backgrounded
   app.
2. The service calls `startForeground()` immediately (required within seconds of
   `startForegroundService()`), gives a haptic buzz, and starts `AudioRecorderController`, which
   reads raw PCM from `AudioRecord` in ~100ms chunks straight into a `.wav` file on disk — the
   recording is durable the moment bytes hit the file, before any recognition is attempted.
3. **Concurrently**, if `TranscriptionProvider.isAvailable()` (on-device check), the service
   starts a live `SpeechRecognizer` listen session (see "Why transcription runs concurrently,
   not after" below).
4. Each chunk's RMS amplitude feeds `SilenceDetector`, a small pure state machine that reports
   "stop" after ~1.5s of trailing silence (ignoring leading silence before any speech). A second
   widget tap, the notification's Stop action, or a 60s hard cap can also end the loop.
5. Once recording stops, the row is marked `CAPTURED` (audio path + duration persisted) before
   anything else happens. The live recognizer is asked to stop and its result (or a timeout) is
   collected.
6. `CaptureProcessor` atomically claims the capture (`CAPTURED`/`FAILED`/`NEEDS_REVIEW` →
   `TRANSCRIBING`, guarded in one Room transaction) so the same capture can never be processed
   twice concurrently, stores the transcript, runs `ExpenseParser`, and replaces (never appends)
   that capture's expense rows — so retries are idempotent by construction.
7. A result notification is shown (with **Undo** if anything was auto-added), and the service
   calls `stopForeground()`/`stopSelf()` unconditionally on every exit path, including
   cancellation.

### Why transcription runs concurrently with recording, not after

Android's public `SpeechRecognizer` API only supports listening to the **live microphone** —
there is no supported API to hand it a previously-recorded audio file. To honor both "save the
recording before attempting transcription" and "use the on-device SpeechRecognizer", this
prototype runs its own `AudioRecord`-based capture (for guaranteed persistence and silence
detection) *alongside* a live `SpeechRecognizer` session, and stops both together. This is also
exactly why `TranscriptionProvider` is shaped as a live "listen" call rather than a
"transcribe(file)" call — see its KDoc for the full reasoning, and "Known issues" below for the
consequence this has for retrying a capture whose transcription failed.

### The parser

`ExpenseParser` is intentionally boring and deterministic: split the transcript into clauses on
`", and"` / `" and "` / `","`, track a running "current date" that a date phrase updates and
that carries forward onto undated clauses, and for each clause extract an amount (skip the
clause entirely if none is found — never invent one), then a category, account, payment method,
and merchant via independent regex-based extractors. See `ExpenseParser.kt`'s class doc for the
full splitting walkthrough and `ExpenseParserTest.kt` for all 8 required examples traced through
it with exact expected amounts/dates/categories/counts.

## Device/API limitations

- **On-device recognition coverage varies by device.** `SpeechRecognizer.isOnDeviceRecognitionAvailable`
  is only available from API 31+; below that, this app falls back to the general
  `isRecognitionAvailable` check plus `EXTRA_PREFER_OFFLINE`, which is a hint, not a guarantee —
  some OEM builds may have no offline model installed (Settings → System → Languages → On-device
  speech recognition, on stock Android). When unavailable, the capture is kept with status
  `FAILED` and its audio preserved; it is never sent anywhere.
- **Emulators frequently have no offline speech model at all**, making `AndroidSpeechRecognitionProvider`
  unreliable there. Prefer a physical device with Google app / Android System Intelligence
  updated, or use `FakeTranscriptionProvider` (already wired into the instrumented tests) to
  exercise the rest of the pipeline without a real recognizer.
- **`FOREGROUND_SERVICE_TYPE_MICROPHONE`** requires API 29+ to have any effect; on 26–28 the
  service still runs as an ordinary foreground service (no type enforcement, but also no
  microphone-specific system indicator beyond the OS-level mic dot, which itself only exists on
  API 29+).
- **Concurrent microphone access** (this app's own `AudioRecord` plus `SpeechRecognizer`'s
  internal capture) is not something Android formally guarantees across all OEMs — most modern
  devices support concurrent capture, but on some hardware/driver combinations one client may
  starve the other, degrading recognition accuracy or amplitude readings. This is the direct
  cost of durably saving audio independently of the recognizer; see "Known issues".
- **Widget preview/instructions** rely on the launcher supporting standard `AppWidgetProviderInfo`
  home-screen widgets (all mainstream launchers do); some third-party launchers handle the "Add
  widget" flow differently from stock Android.

## Known issues and recommended next steps

- **Retrying a capture whose live transcription itself failed re-opens the microphone**, since a
  saved `.wav` file cannot be fed back into `SpeechRecognizer` (see architecture note above).
  `CaptureProcessor.retry()` only replays the *existing transcript* through the parser for free
  when one was already captured (e.g. parsing was the problem); otherwise it has to call
  `listen()` again live. **Next step:** swap in a real offline ASR engine (e.g. Vosk or
  whisper.cpp via JNI) that accepts the saved WAV directly — `TranscriptionProvider` was shaped
  specifically so this swap doesn't touch `VoiceCaptureService` or `CaptureProcessor`.
- **Audio retention cleanup runs once per process start** (from `KeptangApp.onCreate`), not on a
  guaranteed schedule. **Next step:** move `CaptureRepository.purgeExpiredAudio` into a daily
  `WorkManager` periodic job.
- **Per-expense confidence scoring rarely routes an individual expense to review** in the
  current parser — the only hard trigger is "no amount found in the clause at all" (a whole
  clause is then dropped, and if a capture ends up with zero expenses it goes to
  `NEEDS_REVIEW`). Category/account/payment-method mismatches don't currently lower confidence
  below the auto-approve threshold. **Next step:** tune `ConfidenceScorer` once there's real
  transcript data to calibrate against.
- **No conflict handling for two widget taps in extremely quick succession** beyond the
  service's own `isRecording` flag; this is a single-process, single-service design and hasn't
  been stress-tested for rapid double-taps on a slow device.
- **Explicit date parsing** (`DateExpressions`) covers ISO (`2026-08-05`) and `Month Day` / `Day
  Month` forms but not relative phrases like "last Monday" or "next week". **Next step:** extend
  `DateExpressions` alongside more unit tests before relying on it for real use.
- **Amounts with thousands separators** ("1,000 baht") aren't recognized, since the clause
  splitter treats bare commas as clause boundaries. **Next step:** protect digit-group commas
  before splitting.
- The Compose UI is intentionally minimal (per spec) — no swipe gestures, animations, or
  empty-state illustrations. It's built to prove the data flow, not to be a finished product.
- This was authored without access to a JDK/Android SDK/emulator, so `./gradlew build`,
  `./gradlew test`, and `./gradlew connectedAndroidTest` have **not actually been run** against
  this code. Run them before relying on this as anything more than a well-reasoned first pass.
