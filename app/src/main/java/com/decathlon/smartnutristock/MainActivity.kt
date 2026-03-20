package com.decathlon.smartnutristock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.decathlon.smartnutristock.ui.theme.SmartNutriStockTheme
import dagger.hilt.android.AndroidEntryPoint

val DecathlonBlue = Color(0xFF3643BA)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartNutriStockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // Scaffold nos da la estructura clásica de app (barra superior, contenido, etc.)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Smart Nutri-Stock",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DecathlonBlue // Azul corporativo Decathlon nuevo
                )
            )
        }
    ) { paddingValues ->
        // Column centra el contenido en la pantalla
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "¡Sistema listo para funcionar!",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* TODO: Aquí conectaremos el ML Kit en el futuro */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DecathlonBlue
                ),
                modifier = Modifier.height(56.dp) // Botón grande para pulsar fácil en tienda
            ) {
                Text(
                    text = "Escanear Producto",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SmartNutriStockTheme {
        MainScreen()
    }
}