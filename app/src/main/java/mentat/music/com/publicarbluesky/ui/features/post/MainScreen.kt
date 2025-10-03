package mentat.music.com.publicarbluesky.ui.features.post


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onPostTextClick: (text: String) -> Unit,
    onFetchAndProcessHubbleImageClick: () -> Unit
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
                    onPostTextClick(textToPost)
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
                onFetchAndProcessHubbleImageClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Obtener y Publicar Imagen del Hubble")
        }
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