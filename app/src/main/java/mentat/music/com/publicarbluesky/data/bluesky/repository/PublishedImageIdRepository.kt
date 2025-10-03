import android.content.Context
import android.content.SharedPreferences

/**
 * Gestiona el almacenamiento y la consulta de IDs de imágenes ya publicadas
 * utilizando SharedPreferences para evitar repeticiones.
 */
class PublishedImageIdRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "PublishedImagesPrefs"
        private const val KEY_PUBLISHED_IDS = "published_image_ids"
    }

    // Obtenemos una instancia de SharedPreferences.
    // El modo privado asegura que solo esta app pueda acceder.
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Añade un nuevo ID de imagen al conjunto de IDs ya publicados.
     * @param imageId El ID de la imagen que se acaba de publicar.
     */
    fun addPublishedId(imageId: String) {
        // 1. Obtenemos la lista actual de IDs. Es un Set para evitar duplicados.
        val currentIds =
            sharedPreferences.getStringSet(KEY_PUBLISHED_IDS, mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()

        // 2. Añadimos el nuevo ID.
        currentIds.add(imageId)

        // 3. Guardamos la lista actualizada.
        sharedPreferences.edit().putStringSet(KEY_PUBLISHED_IDS, currentIds).apply()
    }

    /**
     * Comprueba si un ID de imagen ya existe en el registro de imágenes publicadas.
     * @param imageId El ID de la imagen que se quiere comprobar.
     * @return `true` si la imagen ya ha sido publicada, `false` en caso contrario.
     */
    fun hasBeenPublished(imageId: String): Boolean {
        // Obtenemos la lista de IDs y comprobamos si el ID actual está contenido en ella.
        val publishedIds = sharedPreferences.getStringSet(KEY_PUBLISHED_IDS, emptySet())
        return publishedIds?.contains(imageId) ?: false
    }
}