package mentat.music.com.publicarbluesky.work

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mentat.music.com.publicarbluesky.MainActivity

class HubblePostWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "HubblePostWorker"
        const val ACTION_AUTO_POST_HUBBLE =
            "mentat.music.com.publicarbluesky.ACTION_AUTO_POST_HUBBLE"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "HubblePostWorker iniciado. Lanzando MainActivity para postear.")

        try {
            // Crea el Intent para iniciar MainActivity
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                // Correcta asignación de la acción
                this.action = ACTION_AUTO_POST_HUBBLE

                // Correcta asignación de los flags
                // FLAG_ACTIVITY_NEW_TASK es necesario porque iniciamos desde un contexto
                // que no es una Activity (el Worker).
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                // Si quisieras añadir MÁS flags, manteniendo los existentes, harías:
                // this.flags = this.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // o si es la primera vez que asignas:
                // this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            // Podrías añadir extras si MainActivity necesita alguna info específica
            // intent.putExtra("source", "worker") // Esto se haría fuera del bloque apply si lo necesitas

            applicationContext.startActivity(intent)
            Log.i(
                TAG,
                "Intent a MainActivity enviado con acción ${intent.action}."
            ) // Usar intent.action para el log
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error al intentar lanzar MainActivity desde HubblePostWorker", e)
            return Result.failure()
        }
    }
}