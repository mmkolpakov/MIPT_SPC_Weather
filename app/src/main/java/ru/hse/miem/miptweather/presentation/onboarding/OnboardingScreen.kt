package ru.hse.miem.miptweather.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.hse.miem.miptweather.R

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                    slideInVertically(animationSpec = tween(durationMillis = 500)) { it / 3 },
            exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                    slideOutVertically(animationSpec = tween(durationMillis = 300)) { it / 3 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {

                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    modifier = Modifier
                        .size(140.dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge)
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.app_name_onboarding),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.onboarding_description),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_start_button),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}