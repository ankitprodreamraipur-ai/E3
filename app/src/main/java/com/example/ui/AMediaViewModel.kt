package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AMediaRepository
import com.example.data.AdminActivityLog
import com.example.data.AppNotification
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.data.FollowRequest
import com.example.data.ModerationReport
import com.example.data.Post
import com.example.data.Story
import com.example.data.UserProfile
import com.example.data.VerificationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class UserNavTab {
  HOME,
  SEARCH,
  ADD_POST,
  STORIES,
  CHAT,
  PROFILE,
}

enum class AdminSection {
  OVERVIEW,
  USERS,
  MODERATION,
  VERIFICATIONS,
  REPORTS,
  BROADCAST_LOGS,
}

data class AMediaUiState(
  val currentUser: UserProfile? = null,
  val allUsers: List<UserProfile> = emptyList(),
  val feedPosts: List<Post> = emptyList(),
  val activeStories: List<Story> = emptyList(),
  val followRequests: List<FollowRequest> = emptyList(),
  val chats: List<ChatThread> = emptyList(),
  val notifications: List<AppNotification> = emptyList(),
  val verificationRequests: List<VerificationRequest> = emptyList(),
  val reports: List<ModerationReport> = emptyList(),
  val activityLogs: List<AdminActivityLog> = emptyList(),
  val isLoading: Boolean = true,
  val statusBanner: String? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AMediaViewModel(
  private val repository: AMediaRepository,
  private val currentUserId: String,
  private val initialDisplayName: String,
  private val initialEmail: String,
) : ViewModel() {

  private val _selectedTab = MutableStateFlow(UserNavTab.HOME)
  val selectedTab: StateFlow<UserNavTab> = _selectedTab.asStateFlow()

  private val _isAdminPanelOpen = MutableStateFlow(false)
  val isAdminPanelOpen: StateFlow<Boolean> = _isAdminPanelOpen.asStateFlow()

  private val _selectedAdminSection = MutableStateFlow(AdminSection.OVERVIEW)
  val selectedAdminSection: StateFlow<AdminSection> = _selectedAdminSection.asStateFlow()

  private val _isDarkTheme = MutableStateFlow(true)
  val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

  private val _activeStoryViewer = MutableStateFlow<Story?>(null)
  val activeStoryViewer: StateFlow<Story?> = _activeStoryViewer.asStateFlow()

  private val _activeChatThread = MutableStateFlow<ChatThread?>(null)
  val activeChatThread: StateFlow<ChatThread?> = _activeChatThread.asStateFlow()

  private val _statusBanner = MutableStateFlow<String?>(null)
  val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

  val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatThread
    .flatMapLatest { thread ->
      if (thread == null) {
        flowOf(emptyList())
      } else {
        repository.observeMessages(thread.id, currentUserId)
          .catch { e ->
            Log.w(TAG, "Error observing chat messages", e)
            emit(emptyList())
          }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList(),
    )

  private val currentUserFlow = repository.observeCurrentUserProfile(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing current user profile", e)
      emit(null)
    }

  private val managedUsersFlow = repository.observeManagedUsers(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing managed users", e)
      emit(emptyList())
    }

  private val feedPostsFlow = repository.observeFeedPosts(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing feed posts", e)
      emit(emptyList())
    }

  private val storiesFlow = repository.observeStories(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing stories", e)
      emit(emptyList())
    }

  private val followRequestsFlow = repository.observeFollowRequests(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing follow requests", e)
      emit(emptyList())
    }

  private val chatsFlow = repository.observeChats(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing chats", e)
      emit(emptyList())
    }

  private val notificationsFlow = repository.observeNotifications(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing notifications", e)
      emit(emptyList())
    }

  private val verificationRequestsFlow = repository.observeVerificationRequests(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing verification requests", e)
      emit(emptyList())
    }

  private val reportsFlow = repository.observeReports(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing reports", e)
      emit(emptyList())
    }

  private val activityLogsFlow = repository.observeActivityLogs(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing activity logs", e)
      emit(emptyList())
    }

  val uiState: StateFlow<AMediaUiState> = combine(
    combine(currentUserFlow, managedUsersFlow, feedPostsFlow) { me, users, posts ->
      Triple(me, users, posts)
    },
    combine(storiesFlow, followRequestsFlow, chatsFlow) { stories, reqs, chats ->
      Triple(stories, reqs, chats)
    },
    combine(notificationsFlow, verificationRequestsFlow, reportsFlow, activityLogsFlow) { notifs, vreqs, reps, logs ->
       listOf(notifs, vreqs, reps, logs)
    },
    _statusBanner,
  ) { core, social, adminLists, banner ->
    val me = core.first
    val managedUsers = core.second
    val allUsersCombined = (listOfNotNull(me) + managedUsers).distinctBy { it.userId.ifBlank { it.id } }
    val nowMs = System.currentTimeMillis()
    val validStories = social.first.filter { it.isActive(nowMs) }

    @Suppress("UNCHECKED_CAST")
    AMediaUiState(
      currentUser = me,
      allUsers = allUsersCombined,
      feedPosts = core.third,
      activeStories = validStories,
      followRequests = social.second,
      chats = social.third,
      notifications = adminLists[0] as List<AppNotification>,
      verificationRequests = adminLists[1] as List<VerificationRequest>,
      reports = adminLists[2] as List<ModerationReport>,
      activityLogs = adminLists[3] as List<AdminActivityLog>,
      isLoading = me == null,
      statusBanner = banner,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = AMediaUiState(isLoading = true),
  )

  init {
    bootstrapUserAndEcosystem()
  }

  private fun bootstrapUserAndEcosystem() {
    viewModelScope.launch {
      val fallbackUsername = initialEmail.substringBefore("@")
        .ifBlank { initialDisplayName.lowercase().replace(" ", "_") }
        .ifBlank { "amedia_creator" }
      val result = repository.createOrEnsureUserProfile(
        username = fallbackUsername,
        displayName = initialDisplayName.ifBlank { "AMedia Creator" },
      )
      result.onSuccess { profile ->
        repository.seedInitialEcosystemIfNeeded(profile)
      }.onFailure { err ->
        showBanner("Profile sync error: ${err.localizedMessage ?: "Retry"}")
      }
    }
  }

  fun selectTab(tab: UserNavTab) {
    _selectedTab.value = tab
  }

  fun toggleAdminPanel(open: Boolean) {
    _isAdminPanelOpen.value = open
  }

  fun selectAdminSection(section: AdminSection) {
    _selectedAdminSection.value = section
  }

  fun toggleDarkTheme() {
    _isDarkTheme.value = !_isDarkTheme.value
  }

  fun openStoryViewer(story: Story?) {
    _activeStoryViewer.value = story
    if (story != null) {
      viewModelScope.launch {
        repository.markStoryViewed(story)
      }
    }
  }

  fun openChatThread(thread: ChatThread?) {
    _activeChatThread.value = thread
  }

  fun startChatWithFriend(peer: UserProfile) {
    viewModelScope.launch {
      repository.getOrCreateChatThread(peer).onSuccess { chatId ->
        _selectedTab.value = UserNavTab.CHAT
        val match = uiState.value.chats.find { it.id == chatId || it.peerId == peer.userId }
        if (match != null) {
          _activeChatThread.value = match
        }
        showBanner("Chat ready with @${peer.username}")
      }
    }
  }

  fun showBanner(message: String?) {
    _statusBanner.value = message
  }

  fun updateProfile(
    username: String,
    displayName: String,
    dob: String,
    bio: String,
    avatarUrl: String,
    isPrivate: Boolean,
  ) {
    viewModelScope.launch {
      repository.updateUserProfile(
        username = username,
        displayName = displayName,
        dob = dob,
        bio = bio,
        avatarUrl = avatarUrl,
        isPrivate = isPrivate,
      ).onSuccess {
        showBanner("Profile updated successfully ✨")
      }.onFailure {
        showBanner("Failed to update profile: ${it.localizedMessage}")
      }
    }
  }

  fun createPost(caption: String, imageUrl: String) {
    val me = uiState.value.currentUser ?: return
    viewModelScope.launch {
      repository.createPost(me, caption, imageUrl).onSuccess {
        _selectedTab.value = UserNavTab.HOME
        showBanner("Post published to your AMedia feed 📸")
      }.onFailure {
        showBanner("Could not publish post: ${it.localizedMessage}")
      }
    }
  }

  fun createStory(caption: String, imageUrl: String) {
    val me = uiState.value.currentUser ?: return
    viewModelScope.launch {
      repository.createStory(me, caption, imageUrl).onSuccess {
        _selectedTab.value = UserNavTab.HOME
        showBanner("24-hour story published ✨")
      }.onFailure {
        showBanner("Could not publish story: ${it.localizedMessage}")
      }
    }
  }

  fun toggleLike(post: Post) {
    val me = uiState.value.currentUser ?: return
    viewModelScope.launch {
      repository.toggleLikePost(post, me)
    }
  }

  fun addComment(post: Post, text: String) {
    val me = uiState.value.currentUser ?: return
    viewModelScope.launch {
      repository.addCommentToPost(post, me, text).onSuccess {
        showBanner("Comment added 💬")
      }
    }
  }

  fun sharePost(post: Post) {
    viewModelScope.launch {
      repository.incrementShareCount(post).onSuccess {
        showBanner("Shared @${post.authorUsername}'s post 🚀")
      }
    }
  }

  fun toggleSavePost(postId: String) {
    val me = uiState.value.currentUser ?: return
    viewModelScope.launch {
      repository.toggleSavePost(me, postId).onSuccess {
        val saved = postId !in me.savedPostIds
        showBanner(if (saved) "Saved to Bookmarks 🔖" else "Removed from Bookmarks")
      }
    }
  }

  fun followOrUnfollow(targetUser: UserProfile) {
    val me = uiState.value.currentUser ?: return
    val state = uiState.value
    viewModelScope.launch {
      repository.followOrUnfollowUser(
        currentUser = me,
        targetUser = targetUser,
        allPosts = state.feedPosts,
        allStories = state.activeStories,
      ).onSuccess { msg ->
        showBanner(msg)
      }
    }
  }

  fun respondFollowRequest(request: FollowRequest, approve: Boolean) {
    val me = uiState.value.currentUser ?: return
    viewModelScope.launch {
      repository.respondToFollowRequest(
        request = request,
        approve = approve,
        currentUser = me,
        allUsers = uiState.value.allUsers,
      ).onSuccess {
        showBanner(if (approve) "Approved @${request.fromUsername}" else "Declined follow request")
      }
    }
  }

  fun sendMessage(text: String) {
    val thread = _activeChatThread.value ?: return
    val me = uiState.value.currentUser ?: return
    viewModelScope.launch {
      repository.sendChatMessage(thread, me, text)
    }
  }

  fun setTyping(isTyping: Boolean) {
    val thread = _activeChatThread.value ?: return
    viewModelScope.launch {
      repository.setTypingIndicator(thread.id, isTyping)
    }
  }

  fun submitBlueTickRequest(category: String, reason: String) {
    val me = uiState.value.currentUser ?: return
    viewModelScope.launch {
      repository.submitVerificationRequest(me, category, reason).onSuccess {
        showBanner("Blue tick verification request submitted for Admin review ✓")
      }
    }
  }

  fun reportContent(targetId: String, targetType: String, summary: String, reason: String) {
    val me = uiState.value.currentUser ?: return
    viewModelScope.launch {
      repository.submitReport(me, targetId, targetType, summary, reason).onSuccess {
        showBanner("Report submitted to AMedia Moderation Team 🛡️")
      }
    }
  }

  fun markAllNotificationsRead() {
    viewModelScope.launch {
      repository.markNotificationsRead(uiState.value.notifications)
    }
  }

  // --- ADMIN PANEL ACTIONS ---

  fun adminReviewVerification(request: VerificationRequest, approve: Boolean) {
    val state = uiState.value
    val adminName = state.currentUser?.displayName ?: "Admin"
    viewModelScope.launch {
      repository.adminReviewVerificationRequest(
        request = request,
        approve = approve,
        adminName = adminName,
        allPosts = state.feedPosts,
        allStories = state.activeStories,
        allChats = state.chats,
      ).onSuccess {
        showBanner(
          if (approve) {
            "Approved & assigned Blue Tick Badge to @${request.username} ✓"
          } else {
            "Rejected verification request for @${request.username}"
          }
        )
      }
    }
  }

  fun adminToggleUserVerification(user: UserProfile) {
    val state = uiState.value
    val adminName = state.currentUser?.displayName ?: "Admin"
    val targetId = user.userId.ifBlank { user.id }
    val newVerified = !user.isVerified
    viewModelScope.launch {
      repository.adminSetUserVerified(
        targetUserId = targetId,
        targetUsername = user.username,
        verified = newVerified,
        adminName = adminName,
        allPosts = state.feedPosts,
        allStories = state.activeStories,
        allChats = state.chats,
      ).onSuccess {
        showBanner(
          if (newVerified) {
            "Assigned Blue Tick Badge to @${user.username} ✓"
          } else {
            "Removed Blue Tick Badge from @${user.username}"
          }
        )
      }
    }
  }

  fun adminToggleUserBan(user: UserProfile) {
    val adminName = uiState.value.currentUser?.displayName ?: "Admin"
    viewModelScope.launch {
      repository.adminToggleBanUser(user, adminName).onSuccess {
        showBanner(if (!user.isBanned) "Banned @${user.username}" else "Unbanned @${user.username}")
      }
    }
  }

  fun adminDeleteUser(user: UserProfile) {
    val adminName = uiState.value.currentUser?.displayName ?: "Admin"
    viewModelScope.launch {
      repository.adminDeleteUser(user, adminName).onSuccess {
        showBanner("Deleted user @${user.username}")
      }
    }
  }

  fun adminRemovePost(post: Post) {
    val adminName = uiState.value.currentUser?.displayName ?: "Admin"
    viewModelScope.launch {
      repository.deletePost(post, adminName, isAdminAction = true).onSuccess {
        showBanner("Removed post by @${post.authorUsername}")
      }
    }
  }

  fun adminRemoveStory(story: Story) {
    val adminName = uiState.value.currentUser?.displayName ?: "Admin"
    viewModelScope.launch {
      repository.deleteStory(story, adminName, isAdminAction = true).onSuccess {
        showBanner("Removed story by @${story.authorUsername}")
      }
    }
  }

  fun adminUpdateReport(report: ModerationReport, newStatus: String) {
    val adminName = uiState.value.currentUser?.displayName ?: "Admin"
    viewModelScope.launch {
      repository.adminUpdateReportStatus(report, newStatus, adminName).onSuccess {
        showBanner("Report status updated to '$newStatus'")
      }
    }
  }

  fun adminBroadcastAnnouncement(title: String, message: String) {
    val adminName = uiState.value.currentUser?.displayName ?: "Admin"
    viewModelScope.launch {
      repository.adminSendBroadcast(title, message, adminName).onSuccess {
        showBanner("Broadcast announcement sent to all users 📢")
      }
    }
  }

  fun deleteOwnAccount(onDeleted: () -> Unit) {
    val state = uiState.value
    val me = state.currentUser ?: return
    viewModelScope.launch {
      repository.deleteCurrentAccount(me, state.feedPosts, state.activeStories).onSuccess {
        onDeleted()
      }
    }
  }

  private companion object {
    const val TAG = "AMediaVM"
  }
}
