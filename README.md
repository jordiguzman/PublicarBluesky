# Hubble Bluesky Publisher

A simple, robust Android application built in Kotlin that automatically fetches and posts images from the Hubble Space Telescope to the Bluesky social network (AT Protocol).

This project is designed as a hybrid system: it can run 100% automatically in the background or be triggered manually by the user, with both systems working perfectly together.

---

## 🚀 Features

* **Automatic Posting:** Uses `WorkManager` to schedule background jobs at specific times (e.g., 17:00 and 23:00) to post a new Hubble image.
* **Manual Posting:** The user can trigger a new post at any time via a button in the app.
* **Robust Hybrid System:**
    * Manual and automatic jobs are separated using WorkManager **Tags** (`automatic_chain` vs. `manual_job`).
    * Manual jobs post once and terminate without interfering with the automatic chain.
    * The automatic chain self-corrects and reschedules itself, even if the device reboots.
* **Duplicate Prevention:** The app saves a history of published image IDs (using `SharedPreferences`) to ensure that every post features a new image.
* **Multiple-Click Protection:** The manual-post button uses a `REPLACE` policy, so pressing it multiple times only results in **one** final publication.
* **Notifications:** Provides system notifications on the success or failure of each background post.

---

## 🛠️ How It Works

The app's logic is built around `WorkManager` and a simple DI (Dependency Injection) container (`AppContainer.kt`).

1.  **`AppContainer`:** A simple service locator that provides singleton instances of repositories and use cases (like `PostToBlueskyUseCase`, `GetFreshHubbleImageUseCase`, etc.) to the rest of the app.
2.  **`MyApplication.kt`:** On app startup, this class schedules the *first* job in the automatic chain. This job is tagged **`automatic_chain`**.
3.  **`HubblePostWorker.kt`:** This is the "brain" of the operation.
    * When the worker runs, it first checks its tags.
    * If it has the **`automatic_chain`** tag, it publishes an image and then **schedules the next job in the chain** (e.g., the 23:00 job schedules the 17:00 job for the next day).
    * If it has the **`manual_job`** tag, it simply publishes the image and **does not schedule any future jobs**.
4.  **`MainActivity.kt`:** The button for manual posting creates a new `OneTimeWorkRequest` with the **`manual_job`** tag and an `ExistingWorkPolicy` of `REPLACE` to prevent spamming.

This design ensures the two systems (manual and automatic) are completely separate and never interfere with each other's scheduling.

---

## 💻 Tech Stack

* **Language:** 100% [Kotlin](https://kotlinlang.org/)
* **Asynchronous:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
* **Background Tasks:** [Android WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Networking:** [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
* **Social Protocol:** [Bluesky API / AT Protocol](https://atproto.com/)

---

## ⚙️ Setup & Configuration

To run this project, you must provide your own Bluesky credentials.

1.  Clone the repository.
2.  Navigate to `app/src/main/java/.../constans/Constants.kt`.
3.  Fill in your credentials in the `Constants` object:

    ```kotlin
    object Constants {
        // ...
        const val BLUESKY_USERNAME = "your-bluesky-handle.bsky.social"
        const val BLUESKY_APP_PASSWORD = "xxxx-xxxx-xxxx-xxxx" // <-- Generate this in Bluesky settings
    }
    ```

4.  Build and run the project.
