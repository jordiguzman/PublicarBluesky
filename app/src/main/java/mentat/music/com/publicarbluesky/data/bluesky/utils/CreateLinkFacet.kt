package mentat.music.com.publicarbluesky.data.bluesky.utils

import android.util.Log
import mentat.music.com.publicarbluesky.data.bluesky.model.Facet
import mentat.music.com.publicarbluesky.data.bluesky.model.FacetIndex
import mentat.music.com.publicarbluesky.data.bluesky.model.FacetLinkFeature
import mentat.music.com.publicarbluesky.data.bluesky.model.FacetTagFeature
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * Crea un Facet para una URL dentro de un texto, usando las clases de BlueskyRichtextFacets.
 *
 * @param textContent El texto completo del post.
 * @param linkText El texto exacto del enlace tal como aparece en textContent.
 * @param linkUrl La URL real a la que debe apuntar el enlace.
 * @return Un Facet si el linkText se encuentra en textContent, sino null.
 */
fun createLinkFacet(textContent: String, linkText: String, linkUrl: String): Facet? {
    val textBytes = textContent.toByteArray(StandardCharsets.UTF_8)
    val linkTextBytes = linkText.toByteArray(StandardCharsets.UTF_8)

    var startIndex = -1
    var windowStart = 0

    // Encontrar la subcadena de bytes
    while (windowStart + linkTextBytes.size <= textBytes.size) {
        var match = true
        for (i in linkTextBytes.indices) {
            if (textBytes[windowStart + i] != linkTextBytes[i]) {
                match = false
                break
            }
        }
        if (match) {
            startIndex = windowStart
            break
        }
        windowStart++
    }

    if (startIndex != -1) {
        val byteStart = startIndex
        val byteEnd = startIndex + linkTextBytes.size

        val extractedLinkText =
            String(textBytes.sliceArray(byteStart until byteEnd), StandardCharsets.UTF_8)
        if (extractedLinkText != linkText) {
            Log.w(
                "FacetUtil",
                "Error de coincidencia de bytes para facet. Texto original: '$linkText', Extraído de bytes: '$extractedLinkText'. Facet para URL '$linkUrl' podría ser incorrecto."
            )
        }

        // Usamos tus clases: Facet, FacetIndex, FacetLinkFeature
        return Facet(
            index = FacetIndex(byteStart = byteStart, byteEnd = byteEnd),
            features = listOf(FacetLinkFeature(uri = linkUrl)) // No necesitas pasar 'type' explícitamente si tiene valor por defecto
        )
    }
    Log.w(
        "FacetUtil",
        "No se encontró el texto del enlace '$linkText' en el contenido (para URL '$linkUrl') para crear el facet."
    )
    return null

}
/**
 * Crea una lista de Facets para todos los hashtags encontrados en un texto.
 *
 * @param textContent El texto completo del post.
 * @return Una lista de Facets para los hashtags. La lista estará vacía si no se encuentran hashtags.
 */
fun createTagFacets(textContent: String): List<Facet> {
    val facets = mutableListOf<Facet>()
    // Expresión regular para encontrar hashtags: # seguido de caracteres alfanuméricos
    // (ATProtocol/Lexicon define los caracteres válidos para un tag, esta es una simplificación)
    // Lexicon para tags: https://github.com/bluesky-social/atproto/blob/main/lexicons/app/bsky/richtext/facet.json
    // Permite letras, números, guiones bajos. No permite espacios ni otros símbolos dentro del tag.
    val hashtagPattern = Pattern.compile("#([a-zA-Z0-9_]+)")
    val matcher = hashtagPattern.matcher(textContent)

    val textBytes = textContent.toByteArray(StandardCharsets.UTF_8)

    while (matcher.find()) {
        val hashtagWithSymbol = matcher.group(0) // Ej: "#Bluesky"
        val hashtagWithoutSymbol = matcher.group(1) // Ej: "Bluesky"

        if (hashtagWithSymbol != null && hashtagWithoutSymbol != null) {
            // Necesitamos la posición en bytes del hashtag CON el símbolo '#'
            // String.indexOf da la posición en caracteres. Convertimos a bytes.
            // Para ser precisos con los bytes, idealmente buscaríamos el substring de bytes.
            // Esta es una aproximación:
            try {
                // Buscamos el inicio del hashtag completo (#palabra) en el texto original (en caracteres)
                val charStart = matcher.start()

                // Obtenemos la representación en bytes de:
                // 1. El texto ANTES del hashtag
                // 2. El hashtag MISMO
                val textBeforeHashtag = textContent.substring(0, charStart)
                val byteStart = textBeforeHashtag.toByteArray(StandardCharsets.UTF_8).size

                val hashtagBytes = hashtagWithSymbol.toByteArray(StandardCharsets.UTF_8)
                val byteEnd = byteStart + hashtagBytes.size

                // Verificación (opcional pero recomendada): que el slice de bytes coincida
                val extractedHashtag = String(textBytes.sliceArray(byteStart until byteEnd), StandardCharsets.UTF_8)
                if (extractedHashtag != hashtagWithSymbol) {
                    Log.w("FacetUtil", "Tag: Byte mismatch for '$hashtagWithSymbol'. Extracted: '$extractedHashtag'. Skipping this tag.")
                    continue // Saltar este tag si hay discrepancia
                }

                facets.add(
                    Facet(
                        index = FacetIndex(byteStart = byteStart, byteEnd = byteEnd),
                        features = listOf(FacetTagFeature(tag = hashtagWithoutSymbol))
                    )
                )
                Log.d("FacetUtil", "Tag Facet CREATED for: $hashtagWithSymbol, tag value: $hashtagWithoutSymbol, byteStart: $byteStart, byteEnd: $byteEnd")
            } catch (e: Exception) {
                Log.e("FacetUtil", "Error processing hashtag '$hashtagWithSymbol': ${e.message}", e)
            }
        }
    }
    return facets
}