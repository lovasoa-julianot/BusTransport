package com.orion.madatransit.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.orion.madatransit.components.FeatureItem
import com.orion.madatransit.R

@Composable
fun FeaturesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

//        AsyncImage(
//            model = R.drawable.logo,
//            modifier = Modifier.size(250.dp),
//            contentDescription = null
//        )

        Column {
            Text(
                text = "Trouvez votre arrêt\net le bon bus facilement",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            FeatureItem(
                title = "Arrêts proches",
                description = "Voir les arrêts autour de votre position et la distance à pied.",
                model = R.drawable.placeholder
            )

            Spacer(modifier = Modifier.height(20.dp))

            FeatureItem(
                title = "Choix de destination",
                description = "Entrez votre destination pour obtenir les lignes recommandées.",
                model = R.drawable.bus
            )

            Spacer(modifier = Modifier.height(20.dp))

            FeatureItem(
                title = "Suggestions de bus",
                description = "Nous vous suggérons les meilleures lignes pour rejoindre votre destination.",
                model = R.drawable.motion
            )
        }

    }
}