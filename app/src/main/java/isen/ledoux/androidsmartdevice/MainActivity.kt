package isen.ledoux.androidsmartdevice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import isen.ledoux.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidSmartDeviceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainScreen(onStartScan = {
                        val intent = Intent(this, ScanActivity::class.java)
                        startActivity(intent)
                    })
                }
            }
        }
    }
}

@Composable
fun MainScreen(onStartScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo principal",
                modifier = Modifier
                    .height(120.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Bienvenue dans l'application\nSmartDevice BLE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onStartScan) {
                Text("Lancer le scan BLE")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Image(
                painter = painterResource(id = R.drawable.isen_logo),
                contentDescription = "Logo ISEN",
                modifier = Modifier.height(50.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.stm),
                contentDescription = "Logo STM",
                modifier = Modifier.height(50.dp)
            )
        }
    }
}
