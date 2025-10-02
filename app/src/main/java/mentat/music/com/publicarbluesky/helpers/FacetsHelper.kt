package mentat.music.com.publicarbluesky.helpers

import mentat.music.com.publicarbluesky.data.bluesky.model.Facet
import mentat.music.com.publicarbluesky.data.bluesky.model.FacetIndex
import mentat.music.com.publicarbluesky.data.bluesky.model.FacetLinkFeature
import mentat.music.com.publicarbluesky.data.bluesky.model.FacetTagFeature

// PASO 1: Importar los modelos CORRECTOS que TÚ definiste
fun createFacets(text: String): List<Facet> {
    val facets = mutableListOf<Facet>()
    val textBytes = text.toByteArray(Charsets.UTF_8)

    // Regex para encontrar links (http o https)
    val linkRegex = """https?://[^\s]+""".toRegex()
    linkRegex.findAll(text).forEach { matchResult ->
        val uri = matchResult.value
        val start = textBytes.slice(0 until matchResult.range.first).size
        val end = start + textBytes.slice(matchResult.range).size

        // PASO 2: Usar TUS clases: FacetIndex y FacetLinkFeature
        facets.add(
            Facet(
                index = FacetIndex(byteStart = start, byteEnd = end),
                features = listOf(FacetLinkFeature(uri = uri))
            )
        )
    }

    // Regex para encontrar hashtags (mejorado para soportar letras con acentos, etc.)
    val tagRegex = """#[\p{L}\p{N}_]+""".toRegex()
    tagRegex.findAll(text).forEach { matchResult ->
        val tag = matchResult.value.substring(1) // Quita el '#'
        val start = textBytes.slice(0 until matchResult.range.first).size
        val end = start + textBytes.slice(matchResult.range).size

        // PASO 3: Usar TUS clases: FacetIndex y FacetTagFeature
        facets.add(
            Facet(
                index = FacetIndex(byteStart = start, byteEnd = end),
                features = listOf(FacetTagFeature(tag = tag))
            )
        )
    }

    // Devuelve los facets ordenados por su posición inicial
    return facets.sortedBy { it.index.byteStart }
}