// LINE 1: This is the package name. It tells the app that this file belongs in the 'pages' folder.
package com.gongchampou.gapps.pages

// LINE 4: We bring in 'clickable' so we can make parts of the screen respond when you touch them.
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
// LINE 8: This makes the corners of our boxes look soft and rounded instead of pointy.
import androidx.compose.foundation.shape.RoundedCornerShape
// LINE 10: This gives us access to all the small icons like the Gear, Checkmark, and Arrow.
import androidx.compose.material.icons.Icons
// LINE 12: We import all the 'filled' icons so we can use them anywhere in the file.
import androidx.compose.material.icons.filled.*
// LINE 14: Material3 is the design language that makes the buttons and text look modern and clean.
import androidx.compose.material3.*
// LINE 16: 'runtime' tools help the app "remember" settings even when the screen changes.
import androidx.compose.runtime.*
// LINE 18: 'Alignment' is used to center text and icons so they look perfectly straight.
import androidx.compose.ui.Alignment
// LINE 20: 'Modifier' is a tool that lets us change the size, padding, and color of anything.
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
// LINE 22: 'Color' allows us to use specific shades like Green, Gray, and Blue.
import androidx.compose.ui.graphics.Color
// LINE 24: 'LocalContext' helps the app find files that are saved on your phone.
import androidx.compose.ui.platform.LocalContext
// LINE 26: 'LocalUriHandler' lets the app open web links (like GitHub) in your browser.
import androidx.compose.ui.platform.LocalUriHandler
// LINE 28: 'FontWeight' is used to make text Bold or extra thick so it's easy to read.
import androidx.compose.ui.text.font.FontWeight
// LINE 30: 'dp' is a unit for measuring distance and size on the screen.
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// LINE 32: 'NavController' is the tool that lets the user jump from one screen to another.
import androidx.navigation.NavController
// LINE 33: 'Toast' is a small pop-up message that tells the user information.
import android.widget.Toast
// LINE 34: 'Screen' is a list of all the different pages in our app.
import com.gongchampou.gapps.Screen
// LINE 36: 'TaskViewModel' is the "Brain" that stores your settings and data.
import com.gongchampou.gapps.TaskViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
// LINE 50: 'java.io.File' is a tool that lets the app look at files in your phone's memory.
import java.io.File

/**
 * LINE 48: The SettingsScreen function builds the entire settings page.
 * It's where you change things like Dark Mode, Sound, and Font Size.
 */
@Composable
fun SettingsScreen(viewModel: TaskViewModel, navController: NavController) {
    // LINE 53: We "watch" the Dark Mode setting. If it's on, the screen turns dark.
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkModePref by viewModel.isDarkMode.collectAsState()
    val isDarkMode = isDarkModePref ?: isSystemDark
    // LINE 55: We "watch" the Notifications setting to see if you want alerts.
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    // LINE 57: This checks if the phone should vibrate when the timer stops.
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
    // LINE 59: This checks if the app should play a sound when the timer stops.
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    // LINE 61: This checks if the screen should stay awake when the timer is running.
    val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()
    
    // LINE 64: This tool handles opening websites for us.
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    // LINE 66: This tool lets the app run tasks in the background without freezing the screen.
    val scope = rememberCoroutineScope()

    /**
     * LINE 103: This function checks GitHub to see if there is a new version of the app.
     * It looks for a "tag" like v1.0.1 and compares it to your current version (1.0.0).
     */
    fun checkForUpdates() {
        scope.launch(Dispatchers.IO) { // Run this on a background thread so the app doesn't lag.
            try {
                // The URL for the latest release information on GitHub.
                val url = URL("https://api.github.com/repos/Gongchampou/an-focus/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == 200) {
                    // Read the text from GitHub.
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    // Turn the text into a format the app understands.
                    val latestRelease = Gson().fromJson(response, Map::class.java)
                    val tagName = latestRelease["tag_name"] as String
                    // Remove the 'v' (e.g., 'v1.0.1' becomes '1.0.1').
                    val latestVersion = tagName.removePrefix("v")

                    withContext(Dispatchers.Main) { // Switch back to the UI thread to show messages.
                        if (latestVersion > "1.0.0") {
                            Toast.makeText(context, "New Update Found: $tagName", Toast.LENGTH_LONG).show()
                            uriHandler.openUri("https://github.com/Gongchampou/an-focus/releases/latest")
                        } else {
                            Toast.makeText(context, "Your app is already up to date!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error checking updates. Please try again later.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No Internet connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // LINE 144: Column puts all the settings in a vertical list from top to bottom.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        // LINE 106: The main title at the top of the settings page.
        Text("General Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))

        SettingsToggle("Dark Mode", isDarkMode) { viewModel.setDarkMode(it) }
        SettingsToggle("Notifications", isNotificationsEnabled) { viewModel.setNotifications(it) }
        SettingsToggle("Vibration", isVibrationEnabled) { viewModel.setVibration(it) }
        SettingsToggle("Sound", isSoundEnabled) { viewModel.setSound(it) }
        SettingsToggle("Screen Awake", keepScreenAwake) { viewModel.setKeepScreenAwake(it) }

        val isSwipeMainNavEnabled by viewModel.isSwipeMainNavEnabled.collectAsState()
        val isSwipeTrackingNavEnabled by viewModel.isSwipeTrackingNavEnabled.collectAsState()
        SettingsToggle("Swipe: Main Navigation", isSwipeMainNavEnabled) { viewModel.setSwipeMainNav(it) }
        SettingsToggle("Swipe: Tracking Sheet", isSwipeTrackingNavEnabled) { viewModel.setSwipeTrackingNav(it) }

        // LINE 116: This setting controls how you see your money tracking (Circle vs Card).
        val showCircularProgress by viewModel.showCircularProgress.collectAsState()
        SettingsToggle(
            label = if (showCircularProgress) "Money: Circle View" else "Money: Card View",
            checked = showCircularProgress
        ) { viewModel.setShowCircularProgress(it) }

        // LINE 120: CURRENCY SELECTOR
        // This is a button that opens a menu to pick your money symbol ($, €, etc).
        val currentCurrency by viewModel.currency.collectAsState()
        var showCurrencyMenu by remember { mutableStateOf(false) }
        val currencies = listOf("$", "₹", "€", "£", "¥", "₩")

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Currency", style = MaterialTheme.typography.bodyLarge)
            Box {
                OutlinedButton(
                    onClick = { showCurrencyMenu = true },
                    modifier = Modifier.height(32.dp), // Reduced height
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(currentCurrency, fontSize = 14.sp)
                }
                DropdownMenu(
                    expanded = showCurrencyMenu,
                    onDismissRequest = { showCurrencyMenu = false }
                ) {
                    currencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency) },
                            onClick = {
                                viewModel.setCurrency(currency)
                                showCurrencyMenu = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // LINE 158: TIMER TONE SELECTOR
        val currentTone by viewModel.timerTone.collectAsState()
        var showToneMenu by remember { mutableStateOf(false) }
        val tones = listOf("Default", "berivan.opus", "doorbell.opus", "field-ring.opus", "fieldtone.opus", "jingle-bells.opus", "liquid-glass.opus", "normal.opus", "phone-call.opus", "ringtone.opus", "ringtone-car.opus", "spring-drip.mp3", "univ.opus", "univers.opus", "universfield.opus")

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Timer Tone", style = MaterialTheme.typography.bodyLarge)
            Box {
                OutlinedButton(
                    onClick = { showToneMenu = true },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(currentTone, fontSize = 14.sp)
                }
                DropdownMenu(
                    expanded = showToneMenu,
                    onDismissRequest = { showToneMenu = false }
                ) {
                    tones.forEach { tone ->
                        DropdownMenuItem(
                            text = { Text(tone) },
                            onClick = {
                                viewModel.setTimerTone(tone)
                                showToneMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LINE 158: E-BOOK FONT SIZE SLIDER - COMPACTED
        val ebookFontSize by viewModel.ebookFontSize.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("E-book Font", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = ebookFontSize,
                onValueChange = { viewModel.setEbookFontSize(it) },
                valueRange = 12f..32f,
                steps = 10,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp).height(18.dp)
            )
            Text("${ebookFontSize.toInt()}sp", fontSize = 12.sp, modifier = Modifier.width(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LINE 200: INFO SECTION
        // This shows the app version and developer name.

        Spacer(modifier = Modifier.height(12.dp))
        Text("About", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("Developer: Gongchampou Kamei.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(10.dp))
        Text("G Apps Version: 2.0.0", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        // LINE 212: THE GRID BOX FOR BUTTONS
        // We use a Row to put two buttons side-by-side.
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp) // This adds a small gap between the two boxes.
        ) {
            // BOX 1: THE GITHUB BUTTON
            Button(
                onClick = {
                    uriHandler.openUri("https://github.com/Gongchampou/an-focus.git")
                },
                modifier = Modifier.weight(1f).height(44.dp), // .weight(1f) makes it take half the space.
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View GitHub", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // BOX 2: THE UPDATE CHECK BUTTON
            Button(
                onClick = {
                    checkForUpdates()
                },
                modifier = Modifier.weight(1f).height(44.dp), // .weight(1f) makes it take the other half.
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Check Update", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * SettingsToggle is a special helper row for settings like 'Dark Mode'.
 */
@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)

        // I put the switch back but scaled it down to 0.7x size to make it look smaller.
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.7f)
        )
    }
}
