package com.orion.bustransport.components

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.bustransport.R

@Composable
fun ErrorSplashScreen(
    errorTitle: String = "Erreur de connexion",
    errorMessage: String = "Impossible de contacter le serveur. Veuillez vérifier votre connexion internet.",
    onExit: () -> Unit
) {
    val darkBlue = Color(0xFF19193E)
    val accentTeal = Color(0xFF00B29A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icône d'erreur ou Logo
        Box(
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.error),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
//                colorFilter = ColorFilter.tint(accentTeal)
            )
            
//            Icon(
//                imageVector = Icons.Default.Warning,
//                contentDescription = null,
//                tint = Color.Red,
//                modifier = Modifier
//                    .size(32.dp)
//                    .align(Alignment.BottomEnd)
//            )
        }

        Text(
            text = errorTitle,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = darkBlue,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = errorMessage,
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentTeal)
        ) {
            Text(
                text = "QUITTER L'APPLICATION",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Color.White
            )
        }
    }
}
