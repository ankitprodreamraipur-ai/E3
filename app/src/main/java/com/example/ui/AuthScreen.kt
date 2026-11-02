package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.theme.AMediaBlueTick
import com.example.ui.theme.AMediaCoral
import com.example.ui.theme.AMediaCyan
import com.example.ui.theme.AMediaGold
import com.example.ui.theme.AMediaObsidian
import com.example.ui.theme.AMediaViolet
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private tailrec fun Context.findActivity(): Activity =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Context is not an Activity")
  }

fun FirebaseAuth.authStateFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

private suspend fun handleSignInResult(result: GetCredentialResponse) {
  val credential = result.credential
  if (
    credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
  ) {
    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
    val firebaseCredential =
      GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
    FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
  } else {
    throw IllegalStateException("Unexpected credential type: ${credential.type}")
  }
}

suspend fun signInSilently(activity: Activity, webClientId: String) {
  val credentialManager = CredentialManager.create(activity)
  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(webClientId)
      .setAutoSelectEnabled(true)
      .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  val result = credentialManager.getCredential(activity, request)
  handleSignInResult(result)
}

suspend fun signInWithGoogleButton(activity: Activity, webClientId: String) {
  val credentialManager = CredentialManager.create(activity)
  val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  val result = credentialManager.getCredential(activity, request)
  handleSignInResult(result)
}

suspend fun signOutOfAMedia(context: Context) {
  FirebaseAuth.getInstance().signOut()
  CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
}

@Composable
fun AuthScreen() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val webClientId = remember { context.getString(R.string.default_web_client_id) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    if (FirebaseAuth.getInstance().currentUser == null) {
      try {
        signInSilently(context.findActivity(), webClientId)
      } catch (_: NoCredentialException) {
        // Silent sign-in failed; user must click the button
      } catch (e: GetCredentialException) {
        Log.d("Auth", "Silent sign-in unavailable: ${e.message}")
      } catch (e: Exception) {
        Log.d("Auth", "Silent sign-in skipped: ${e.message}")
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(AMediaObsidian)
      .systemBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    // Ambient glowing background orbs
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(AMediaCoral.copy(alpha = 0.28f), Color.Transparent),
          center = Offset(w * 0.2f, h * 0.18f),
          radius = w * 0.65f,
        ),
        radius = w * 0.65f,
        center = Offset(w * 0.2f, h * 0.18f),
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(AMediaViolet.copy(alpha = 0.32f), Color.Transparent),
          center = Offset(w * 0.85f, h * 0.78f),
          radius = w * 0.7f,
        ),
        radius = w * 0.7f,
        center = Offset(w * 0.85f, h * 0.78f),
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(AMediaCyan.copy(alpha = 0.18f), Color.Transparent),
          center = Offset(w * 0.78f, h * 0.22f),
          radius = w * 0.45f,
        ),
        radius = w * 0.45f,
        center = Offset(w * 0.78f, h * 0.22f),
      )
    }

    Surface(
      modifier = Modifier
        .padding(horizontal = 24.dp)
        .widthIn(max = 440.dp)
        .fillMaxWidth()
        .border(
          width = 1.dp,
          brush = Brush.linearGradient(
            listOf(
              Color.White.copy(alpha = 0.22f),
              AMediaCoral.copy(alpha = 0.35f),
              AMediaViolet.copy(alpha = 0.35f),
            )
          ),
          shape = RoundedCornerShape(32.dp),
        ),
      color = Color(0xFF121424).copy(alpha = 0.88f),
      shape = RoundedCornerShape(32.dp),
      tonalElevation = 8.dp,
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Brand emblem
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
              Brush.linearGradient(
                listOf(AMediaCoral, AMediaGold, AMediaViolet, AMediaCyan)
              )
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = "AMedia Logo",
            tint = Color.White,
            modifier = Modifier.size(38.dp),
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "AMedia",
          style = MaterialTheme.typography.displayMedium,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Capture, connect, and curate your visual universe — complete with real-time stories, friend chat, and unified Admin Command.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFA0A5C0),
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feature highlights
        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          FeatureRow(
            icon = Icons.Filled.AutoAwesome,
            tint = AMediaCoral,
            title = "24-Hour Stories & Curated Feed",
            subtitle = "Follow creators, approve private requests & share visual stories",
          )
          FeatureRow(
            icon = Icons.Filled.ChatBubble,
            tint = AMediaCyan,
            title = "Real-Time Friend Chat",
            subtitle = "Live typing indicators, seen status & instant emoji reactions",
          )
          FeatureRow(
            icon = Icons.Filled.Verified,
            tint = AMediaBlueTick,
            title = "Blue Tick Verification & Privacy",
            subtitle = "Request official verification and switch public/private anytime",
          )
          FeatureRow(
            icon = Icons.Filled.AdminPanelSettings,
            tint = AMediaGold,
            title = "Unified Admin Control Dashboard",
            subtitle = "Real-time analytics, moderation, user management & broadcasts",
          )
        }

        Spacer(modifier = Modifier.height(28.dp))

        errorMessage?.let { msg ->
          Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp),
          ) {
            Text(
              text = msg,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(12.dp),
              textAlign = TextAlign.Center,
            )
          }
        }

        Button(
          onClick = {
            scope.launch {
              isLoading = true
              errorMessage = null
              try {
                signInWithGoogleButton(context.findActivity(), webClientId)
              } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Sign-in cancelled by user", e)
              } catch (e: Exception) {
                Log.e("Auth", "Sign-in failed", e)
                errorMessage = e.localizedMessage ?: "Sign-in failed"
              } finally {
                isLoading = false
              }
            }
          },
          enabled = !isLoading,
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = AMediaCoral,
            contentColor = Color.White,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = Color.White,
              strokeWidth = 2.5.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Connecting to AMedia...",
              style = MaterialTheme.typography.titleMedium,
            )
          } else {
            Text(
              text = "Sign in with Google",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureRow(
  icon: ImageVector,
  tint: Color,
  title: String,
  subtitle: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White.copy(alpha = 0.05f))
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .clip(CircleShape)
        .background(tint.copy(alpha = 0.16f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = tint,
        modifier = Modifier.size(20.dp),
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFA0A5C0),
      )
    }
  }
}
