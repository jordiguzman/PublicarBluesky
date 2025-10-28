package mentat.music.com.publicarbluesky

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest // ¡Importante! Ya no es PeriodicWorkRequest
import androidx.work.WorkManager
import mentat.music.com.publicarbluesky.di.AppContainer
import mentat.music.com.publicarbluesky.work.CustomWorkerFactory
import mentat.music.com.publicarbluesky.work.HubblePostWorker
import java.util.Calendar // Necesario para calcular la hora
import java.util.concurrent.TimeUnit

class MyApplication : Application(), Configuration.Provider {

    // 1. El AppContainer vivirá aquí
    lateinit var appContainer: AppContainer
        private set

    /**
     * El onCreate se llama una sola vez cuando la app se inicia.
     * Es el lugar perfecto para crear el container y programar el trabajo.
     */
    override fun onCreate() {
        super.onCreate()

        // Crea el AppContainer
        appContainer = AppContainer(this)

        // Llama a nuestra nueva función para programar la PRIMERA tarea
        scheduleInitialWork()
    }

    /**
     * Le dice a WorkManager que use nuestra CustomWorkerFactory
     * para poder inyectar dependencias en los Workers.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(CustomWorkerFactory(appContainer))
            .build()


    /**
     * Esta función reemplaza a la antigua setupRecurringWork.
     * Calcula cuándo es la próxima ejecución (17h o 23h) y
     * programa UNA SOLA tarea para ese momento.
     */
    // Dentro de MyApplication.kt

    private fun scheduleInitialWork() {
        val workManager = WorkManager.getInstance(this)

        // 1. Calcular cuántos milisegundos faltan para la próxima hora clave
        val delay = calculateDelayToNextSlot()

        // 2. Definir que solo se ejecute si hay internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 3. Crear una petición de UNA SOLA VEZ (OneTimeWorkRequest)
        val initialWorkRequest = OneTimeWorkRequest.Builder(HubblePostWorker::class.java)
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS) // Pone el retraso calculado
            .addTag("automatic_chain") // <-- ¡¡ESTA ES LA LÍNEA NUEVA!!
            .build()

        // 4. Encolar el trabajo
        // Usamos enqueueUniqueWork con "KEEP" para asegurarnos de que esto
        // solo se programa UNA VEZ, aunque el usuario abra y cierre la app.
        // Si ya hay un trabajo "hubble-post-chain" programado, no hará nada.
        workManager.enqueueUniqueWork(
            "hubble-post-chain", // Nombre único para la cadena de trabajo
            ExistingWorkPolicy.KEEP,
            initialWorkRequest
        )
    }

    /**
     * Calcula los milisegundos que faltan desde AHORA hasta las 17:00 o 23:00.
     */
    private fun calculateDelayToNextSlot(): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis // Hora actual en milisegundos

        // --- Comprobar las 17:00 de HOY ---
        calendar.set(Calendar.HOUR_OF_DAY, 17)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val target17 = calendar.timeInMillis

        if (target17 > now) {
            // Si la hora actual es antes de las 17:00, programamos para las 17:00
            return target17 - now
        }

        // --- Comprobar las 23:00 de HOY ---
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val target23 = calendar.timeInMillis

        if (target23 > now) {
            // Si son más de las 17:00 pero antes de las 23:00, programamos para las 23:00
            return target23 - now
        }

        // --- Si no, programar para las 17:00 de MAÑANA ---
        // Si ya han pasado las 23:00, programamos para mañana a las 17:00
        calendar.add(Calendar.DAY_OF_YEAR, 1) // Mover al día siguiente
        calendar.set(Calendar.HOUR_OF_DAY, 17) // Poner la hora a las 17:00
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val target17Tomorrow = calendar.timeInMillis

        return target17Tomorrow - now
    }
}