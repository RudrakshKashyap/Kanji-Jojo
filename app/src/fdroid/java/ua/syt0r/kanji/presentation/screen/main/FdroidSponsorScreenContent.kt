package ua.syt0r.kanji.presentation.screen.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.presentation.screen.main.screen.sponsor.SponsorScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.sponsor.SponsorScreenUI

object FdroidSponsorScreenContent : SponsorScreenContract.Content {

    @Composable
    override fun invoke(state: MainNavigationState) {
        SponsorScreenUI(
            onUpClick = { state.navigateBack() }
        ) {
            val uriHandler = LocalUriHandler.current
            Button(
                onClick = { uriHandler.openUri("https://github.com/sponsors/RudrakshKashyap") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF24292F),
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Favorite, null)
                Spacer(Modifier.width(12.dp))
                Text("Sponsor on GitHub")
            }

        }
    }

}