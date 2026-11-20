package com.orion.madatransit.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.madatransit.model.BusStop
import com.orion.madatransit.model.BusStopInfo
import com.orion.madatransit.R

@Composable
fun BusStopRowItem(stop: BusStop, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FE))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
//                    .background(Color(0xFF5E17EB)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.bus_station
                    ),
                    contentDescription = "Bus stop",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(Color.Black)
                )
//                Icon(Icons.Default.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(stop.availableLines, fontSize = 12.sp, color = Color.Gray)
            }
            Text(stop.distance, color = stop.distanceColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Gray)
        }
    }
}
