package com.orion.bustransport.screen

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.orion.bustransport.R
import com.orion.bustransport.network.User
import java.util.Locale

@Composable
fun ScanScreen(
    user: User,
    driverName: String,
    imageBaseUrl: String,
    scanResult: Pair<Boolean, String>?,
    scanResultId: Long,
    onTokenScanned: (String) -> Unit,
    onClearScanResult: () -> Unit
) {

    val context = LocalContext.current

    val scanner = remember {
        GmsBarcodeScanning.getClient(context)
    }

    var tts by remember {
        mutableStateOf<TextToSpeech?>(null)
    }

    var ttsReady by remember {
        mutableStateOf(false)
    }

    var autoScanEnabled by remember {
        mutableStateOf(false)
    }

    var isScannerRunning by remember {
        mutableStateOf(false)
    }

    fun startQrScanner() {
        if (isScannerRunning) {
            return
        }
        // Supprimer le résultat précédent
        onClearScanResult()
        isScannerRunning = true
        println("📷 Ouverture du scanner...")
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                isScannerRunning = false
                val rawValue = barcode.rawValue
                if (!rawValue.isNullOrBlank()) {
                    println("📷 QR Code : $rawValue")
                    // On garde le mode automatique actif
                    autoScanEnabled = true
                    onTokenScanned(rawValue)
                } else {
                    println("⚠️ QR Code vide")
                }
            }
            .addOnFailureListener { e ->
                isScannerRunning = false
                println("❌ Erreur scanner : ${e.message}")
                autoScanEnabled = false
                onClearScanResult()
            }
    }

    LaunchedEffect(Unit) {
        lateinit var engine: TextToSpeech

        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val languageResult =
                    engine.setLanguage(Locale.FRENCH)
                when (languageResult) {
                    TextToSpeech.LANG_MISSING_DATA -> {
                        println(
                            "Données vocales françaises manquantes"
                        )
                        ttsReady = false
                    }
                    TextToSpeech.LANG_NOT_SUPPORTED -> {
                        println(
                            "Langue française non supportée"
                        )
                        ttsReady = false
                    }
                    else -> {
                        engine.setSpeechRate(0.95f)
                        ttsReady = true
                        println(
                            "TTS prêt avec succès"
                        )
                    }
                }
            } else {
                println(
                    "Échec initialisation TTS : $status"
                )
                ttsReady = false
            }
        }
        tts = engine
    }

    DisposableEffect(Unit) {
        onDispose {
            println("Arrêt du TTS")
            tts?.stop()
            tts?.shutdown()
            tts = null
            ttsReady = false
        }
    }

    // LECTURE DU RÉSULTAT
    LaunchedEffect(scanResultId, ttsReady) {
        if (scanResultId == 0L) {
            return@LaunchedEffect
        }
        val result = scanResult
        if (result == null) {
            println(
                "Résultat du scan #$scanResultId encore indisponible"
            )
            return@LaunchedEffect
        }
        if (!ttsReady) {
            println(
                "TTS pas encore prêt"
            )
            return@LaunchedEffect
        }
        val message = result.second
        if (message.isBlank()) {
            println(
                "Message TTS vide"
            )
            return@LaunchedEffect
        }
        println(
            "Lecture TTS du résultat #$scanResultId : $message"
        )
        tts?.stop()
        val utteranceId =
            "SCAN_RESULT_${scanResultId}_${System.currentTimeMillis()}"

        val speakResult = tts?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )
        println(
            "Résultat speak() : $speakResult"
        )
    }

    LaunchedEffect(scanResultId) {
        if (scanResultId == 0L) {
            return@LaunchedEffect
        }
        if (!autoScanEnabled) {
            return@LaunchedEffect
        }
        println(
            "🔄 Résultat reçu pour le scan #$scanResultId"
        )
        kotlinx.coroutines.delay(700)
        // Vérification supplémentaire
        if (autoScanEnabled && !isScannerRunning) {
            println(
                "📷 Retour automatique vers le scanner..."
            )
            startQrScanner()
        }
    }

    val tealBackground = Color(0xFFF3F7F9)
    val darkBlue = Color(0xFF19193E)
    val accentTeal = Color(0xFF00B29A)
    val textMuted = Color(0xFF757575)

    val successBg = Color(0xFFE8F5E9)
    val successContent = Color(0xFF2E7D32)

    val errorBg = Color(0xFFFFEBEE)
    val errorContent = Color(0xFFC62828)

    // INTERFACE
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tealBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // SESSION CHAUFFEUR
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            darkBlue.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = user.photo?.let {
                        "$imageBaseUrl/uploads/$it"
                    }

                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Photo profil",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                Column {
                    Text(
                        text = "SESSION ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = driverName,
                        color = darkBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // ZONE CENTRALE
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // RESULTAT DU SCAN

            AnimatedVisibility(
                visible = scanResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                scanResult?.let { result ->
                    val (isSuccess, message) = result
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (isSuccess) {
                                    successBg
                                } else {
                                    errorBg
                                }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector =
                                    if (isSuccess) {
                                        Icons.Default.Check
                                    } else {
                                        Icons.Default.Close
                                    },
                                contentDescription = null,
                                tint =
                                    if (isSuccess) {
                                        successContent
                                    } else {
                                        errorContent
                                    },
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(12.dp)
                            )

                            Text(
                                text = message,
                                color =
                                    if (isSuccess) {
                                        successContent
                                    } else {
                                        errorContent
                                    },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // BOUTON SCANNER
            Button(
                onClick = {
                    autoScanEnabled = true
                    startQrScanner()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentTeal
                ),
                modifier = Modifier.size(160.dp),
                shape = CircleShape,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = R.drawable.code
                        ),
                        contentDescription = "Scanner QR Code",
                        modifier = Modifier.size(42.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = "SCANNER\nQR CODE",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}