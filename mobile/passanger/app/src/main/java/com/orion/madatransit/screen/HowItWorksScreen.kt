package com.orion.madatransit.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.orion.madatransit.components.LegendItem
import com.orion.madatransit.components.StepItem
import com.orion.madatransit.R

@Composable
fun HowItWorksScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Comment ça marche ?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            StepItem(
                step = "1",
                text = "Autorisez la localisation pour voir les arrêts proches."
            )

            Spacer(modifier = Modifier.height(18.dp))


            StepItem(
                step = "2",
                text = "Choisissez votre destination ou saisissez-la."
            )

            Spacer(modifier = Modifier.height(18.dp))

            StepItem(
                step = "3",
                text = "Consultez les suggestions de bus et prenez le meilleur trajet."
            )
        }

        Card(
//            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
//            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
                .background(color = Color.White)
        ) {
            AsyncImage(
                model = R.drawable.splash,
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White),
                contentDescription = null
            )
        }
    }
}