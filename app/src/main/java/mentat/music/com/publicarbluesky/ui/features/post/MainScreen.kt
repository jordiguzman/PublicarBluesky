package mentat.music.com.publicarbluesky.ui.features.post // O el paquete donde esté tu MainScreen.kt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview // Para el Preview

// Asumiendo que MainScreen está en este paquete o tienes las importaciones correctas

@OptIn(ExperimentalMaterial3Api::class) // Necesario para TextField en M3
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onPostTextClick: (text: String) -> Unit, // <--- AÑADIDO/MODIFICADO
    onFetchAndProcessHubbleImageClick: () -> Unit  // <--- AÑADIDO
) {
    var textToPost by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bluesky Publisher",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Campo para ingresar texto a postear
        OutlinedTextField(
            value = textToPost,
            onValueChange = { textToPost = it },
            label = { Text("¿Qué está pasando?") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            maxLines = 5
        )

        // Botón para postear solo texto
        Button(
            onClick = {
                if (textToPost.isNotBlank()) {
                    onPostTextClick(textToPost) // <--- USA EL NUEVO LAMBDA
                    // textToPost = "" // Opcional: limpiar el campo después de enviar
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Publicar Texto")
        }

        // Botón para obtener y procesar imagen del Hubble
        Button(
            onClick = {
                onFetchAndProcessHubbleImageClick() // <--- USA EL NUEVO LAMBDA
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Obtener y Publicar Imagen del Hubble")
        }

        // Aquí podrías añadir más UI en el futuro para mostrar el estado de la carga de la imagen,
        // la imagen descargada, etc.
    }
}

// Preview para MainScreen (opcional pero muy útil)
@Preview(showBackground = true)
@Composable
fun DefaultPreviewOfMainScreen() {
    // Para el preview, pasamos lambdas vacíos o con logs simples
    MainScreen(
        onPostTextClick = { text -> println("Preview: Postear texto: $text") },
        onFetchAndProcessHubbleImageClick = { println("Preview: Obtener imagen Hubble") }
    )
}
