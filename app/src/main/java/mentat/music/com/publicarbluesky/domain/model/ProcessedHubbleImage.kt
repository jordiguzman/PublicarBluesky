package mentat.music.com.publicarbluesky.domain.model


import mentat.music.com.publicarbluesky.data.hubble.model.HubbleSiteImage
import java.io.File

data class ProcessedHubbleImage(
    val imageFile: File,
    val imageInfo: HubbleSiteImage // La información obtenida del scraper
)