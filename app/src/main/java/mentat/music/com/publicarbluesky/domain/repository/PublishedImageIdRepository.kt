package mentat.music.com.publicarbluesky.domain.repository


import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class PublishedImageIdRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "published_images_prefs"
        private const val KEY_PUBLISHED_IDS = "published_image_ids"
        private const val TAG = "PublishedImageRepo"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Añade un ID de imagen a la lista de IDs ya publicados.
     * @param imageId El ID de la imagen que ha sido publicada.
     */
    fun addPublishedId(imageId: String) {
        val currentIds = getAllPublishedIds().toMutableSet()
        if (currentIds.add(imageId)) { // .add() devuelve true si el set fue modificado (el ID no estaba)
            sharedPreferences.edit { putStringSet(KEY_PUBLISHED_IDS, currentIds) }
            Log.d(TAG, "ID de imagen añadido al repositorio: $imageId")
        } else {
            Log.d(TAG, "ID de imagen '$imageId' ya existía en el repositorio.")
        }
    }

    /**
     * Comprueba si un ID de imagen ya ha sido publicado.
     * @param imageId El ID de la imagen a comprobar.
     * @return True si el ID ya ha sido publicado, false en caso contrario.
     */
    fun hasBeenPublished(imageId: String): Boolean {
        val publishedIds = getAllPublishedIds()
        val found = imageId in publishedIds
        if (found) {
            Log.d(TAG, "Comprobación: ID '$imageId' YA HA SIDO PUBLICADO.")
        } else {
            Log.d(TAG, "Comprobación: ID '$imageId' NO ha sido publicado.")
        }
        return found
    }

    /**
     * Obtiene todos los IDs de imágenes que han sido publicados.
     * @return Un Set de Strings con todos los IDs publicados. Puede estar vacío.
     */
    fun getAllPublishedIds(): Set<String> {
        // Retorna un nuevo Set para evitar modificaciones externas directas al Set de SharedPreferences
        return sharedPreferences.getStringSet(KEY_PUBLISHED_IDS, emptySet())?.toSet() ?: emptySet()
    }

    /**
     * (Opcional) Limpia todos los IDs de imágenes publicadas. Útil para testing o reseteo.
     */
    fun clearAllPublishedIds() {
        sharedPreferences.edit { remove(KEY_PUBLISHED_IDS) }
        Log.d(TAG, "Todos los IDs de imágenes publicadas han sido eliminados.")
    }
}
