package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AMediaRepository
import com.example.ui.AMediaViewModel
import com.example.ui.AdminPanelScreen
import com.example.ui.AuthScreen
import com.example.ui.UserPanelScreen
import com.example.ui.authStateFlow
import com.example.ui.signOutOfAMedia
import com.example.ui.theme.AMediaTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AMediaAppRoot()
    }
  }
}

@Composable
fun AMediaAppRoot() {
  val currentUser by FirebaseAuth.getInstance()
    .authStateFlow()
    .collectAsStateWithLifecycle(initialValue = FirebaseAuth.getInstance().currentUser)

  if (currentUser == null) {
    AMediaTheme(darkTheme = true) {
      AuthScreen()
    }
  } else {
    AMediaAuthenticatedApp(user = currentUser!!)
  }
}

@Composable
private fun AMediaAuthenticatedApp(user: FirebaseUser) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val viewModel: AMediaViewModel = viewModel(
    key = user.uid,
    factory = viewModelFactory {
      initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
        AMediaViewModel(
          repository = AMediaRepository(db),
          currentUserId = user.uid,
          initialDisplayName = user.displayName.orEmpty(),
          initialEmail = user.email.orEmpty(),
        )
      }
    },
  )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
  val isAdminPanelOpen by viewModel.isAdminPanelOpen.collectAsStateWithLifecycle()
  val selectedAdminSection by viewModel.selectedAdminSection.collectAsStateWithLifecycle()
  val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
  val activeStoryViewer by viewModel.activeStoryViewer.collectAsStateWithLifecycle()
  val activeChatThread by viewModel.activeChatThread.collectAsStateWithLifecycle()
  val activeChatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()

  AMediaTheme(darkTheme = isDarkTheme) {
    if (isAdminPanelOpen) {
      AdminPanelScreen(
        uiState = uiState,
        selectedSection = selectedAdminSection,
        isDarkTheme = isDarkTheme,
        onSelectSection = { viewModel.selectAdminSection(it) },
        onToggleDarkTheme = { viewModel.toggleDarkTheme() },
        onExitAdminPanel = { viewModel.toggleAdminPanel(false) },
        onDismissBanner = { viewModel.showBanner(null) },
        viewModel = viewModel,
      )
    } else {
      UserPanelScreen(
        uiState = uiState,
        selectedTab = selectedTab,
        isDarkTheme = isDarkTheme,
        activeStoryViewer = activeStoryViewer,
        activeChatThread = activeChatThread,
        activeChatMessages = activeChatMessages,
        onSelectTab = { viewModel.selectTab(it) },
        onOpenAdminPanel = { viewModel.toggleAdminPanel(true) },
        onToggleDarkTheme = { viewModel.toggleDarkTheme() },
        onOpenStoryViewer = { viewModel.openStoryViewer(it) },
        onOpenChatThread = { viewModel.openChatThread(it) },
        onDismissBanner = { viewModel.showBanner(null) },
        viewModel = viewModel,
        onLogout = {
          scope.launch {
            signOutOfAMedia(context)
          }
        },
      )
    }
  }
}
