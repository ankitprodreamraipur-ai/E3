package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.data.Post
import com.example.data.Story
import com.example.ui.components.AMediaPostVisual
import com.example.ui.components.BlueVerifiedBadge
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.AMediaCoral
import com.example.ui.theme.AMediaCyan
import com.example.ui.theme.AMediaGold
import com.example.ui.theme.AMediaViolet
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPanelScreen(
  uiState: AMediaUiState,
  selectedTab: UserNavTab,
  isDarkTheme: Boolean,
  activeStoryViewer: Story?,
  activeChatThread: ChatThread?,
  activeChatMessages: List<ChatMessage>,
  onSelectTab: (UserNavTab) -> Unit,
  onOpenAdminPanel: () -> Unit,
  onToggleDarkTheme: () -> Unit,
  onOpenStoryViewer: (Story?) -> Unit,
  onOpenChatThread: (ChatThread?) -> Unit,
  onDismissBanner: () -> Unit,
  viewModel: AMediaViewModel,
  onLogout: () -> Unit,
) {
  var showNotificationsSheet by remember { mutableStateOf(false) }
  var commentingPost by remember { mutableStateOf<Post?>(null) }
  var reportingPost by remember { mutableStateOf<Post?>(null) }

  val unreadNotifCount = remember(uiState.notifications) {
    uiState.notifications.count { !it.isRead }
  }
  val pendingRequestsCount = remember(uiState.followRequests, uiState.currentUser) {
    uiState.followRequests.count {
      it.toUserId == uiState.currentUser?.userId && it.status == "pending"
    }
  }

  // Auto-dismiss banner after 3.5 seconds
  LaunchedEffect(uiState.statusBanner) {
    if (uiState.statusBanner != null) {
      delay(3500L)
      onDismissBanner()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      contentWindowInsets = WindowInsets.systemBars,
      topBar = {
        Surface(
          tonalElevation = 4.dp,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // AMedia Brand Badge
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  Brush.linearGradient(listOf(AMediaCoral, AMediaGold, AMediaViolet))
                ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "AMedia",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "AMedia",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
            )

            // Prominent Admin Panel Switcher Button
            FilledTonalButton(
              onClick = onOpenAdminPanel,
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("open_admin_panel_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.AdminPanelSettings,
                contentDescription = "Open Admin Panel",
                tint = AMediaGold,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Admin",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Theme Toggle Icon
            IconButton(
              onClick = onToggleDarkTheme,
              modifier = Modifier.testTag("header_theme_toggle"),
            ) {
              Icon(
                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "Toggle theme",
              )
            }

            // Notifications Button with Unread Badge
            IconButton(
              onClick = {
                showNotificationsSheet = true
                viewModel.markAllNotificationsRead()
              },
              modifier = Modifier.testTag("open_notifications_button"),
            ) {
              BadgedBox(
                badge = {
                  if (unreadNotifCount > 0) {
                    Badge(containerColor = AMediaCoral) {
                      Text(unreadNotifCount.toString())
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = Icons.Filled.Notifications,
                  contentDescription = "Notifications",
                )
              }
            }
          }
        }
      },
      bottomBar = {
        NavigationBar(
          tonalElevation = 8.dp,
          containerColor = MaterialTheme.colorScheme.surface,
        ) {
          NavigationBarItem(
            selected = selectedTab == UserNavTab.HOME,
            onClick = { onSelectTab(UserNavTab.HOME) },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            modifier = Modifier.testTag("nav_tab_home"),
          )
          NavigationBarItem(
            selected = selectedTab == UserNavTab.SEARCH,
            onClick = { onSelectTab(UserNavTab.SEARCH) },
            icon = {
              BadgedBox(
                badge = {
                  if (pendingRequestsCount > 0) {
                    Badge(containerColor = AMediaCoral) {
                      Text(pendingRequestsCount.toString())
                    }
                  }
                }
              ) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
              }
            },
            label = { Text("Search") },
            modifier = Modifier.testTag("nav_tab_search"),
          )
          NavigationBarItem(
            selected = selectedTab == UserNavTab.ADD_POST,
            onClick = { onSelectTab(UserNavTab.ADD_POST) },
            icon = { Icon(Icons.Filled.AddBox, contentDescription = "Add Post") },
            label = { Text("Add Post") },
            modifier = Modifier.testTag("nav_tab_add_post"),
          )
          NavigationBarItem(
            selected = selectedTab == UserNavTab.STORIES,
            onClick = { onSelectTab(UserNavTab.STORIES) },
            icon = { Icon(Icons.Filled.MotionPhotosOn, contentDescription = "Stories") },
            label = { Text("Stories") },
            modifier = Modifier.testTag("nav_tab_stories"),
          )
          NavigationBarItem(
            selected = selectedTab == UserNavTab.CHAT,
            onClick = { onSelectTab(UserNavTab.CHAT) },
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
            label = { Text("Chat") },
            modifier = Modifier.testTag("nav_tab_chat"),
          )
          NavigationBarItem(
            selected = selectedTab == UserNavTab.PROFILE,
            onClick = { onSelectTab(UserNavTab.PROFILE) },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            modifier = Modifier.testTag("nav_tab_profile"),
          )
        }
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        when (selectedTab) {
          UserNavTab.HOME -> {
            HomeFeedTab(
              uiState = uiState,
              onOpenStory = { onOpenStoryViewer(it) },
              onAddStoryClick = { onSelectTab(UserNavTab.ADD_POST) },
              onLikeToggle = { viewModel.toggleLike(it) },
              onCommentClick = { commentingPost = it },
              onShareClick = { viewModel.sharePost(it) },
              onSaveToggle = { viewModel.toggleSavePost(it.id) },
              onReportClick = { reportingPost = it },
              onExploreCreatorsClick = { onSelectTab(UserNavTab.SEARCH) },
            )
          }

          UserNavTab.SEARCH -> {
            SearchAndRequestsTab(
              uiState = uiState,
              onFollowToggle = { viewModel.followOrUnfollow(it) },
              onRespondRequest = { req, approve -> viewModel.respondFollowRequest(req, approve) },
              onStartChat = { viewModel.startChatWithFriend(it) },
            )
          }

          UserNavTab.ADD_POST -> {
            CreatePostOrStoryTab(
              onPublishPost = { caption, imageUrl -> viewModel.createPost(caption, imageUrl) },
              onPublishStory = { caption, imageUrl -> viewModel.createStory(caption, imageUrl) },
            )
          }

          UserNavTab.STORIES -> {
            StoriesGalleryTab(
              stories = uiState.activeStories,
              currentUserId = uiState.currentUser?.userId.orEmpty(),
              onOpenStory = { onOpenStoryViewer(it) },
              onCreateStoryClick = { onSelectTab(UserNavTab.ADD_POST) },
            )
          }

          UserNavTab.CHAT -> {
            FriendChatTab(
              uiState = uiState,
              activeThread = activeChatThread,
              messages = activeChatMessages,
              onOpenThread = onOpenChatThread,
              onStartChatWithFriend = { viewModel.startChatWithFriend(it) },
              onSendMessage = { viewModel.sendMessage(it) },
              onTypingChange = { viewModel.setTyping(it) },
            )
          }

          UserNavTab.PROFILE -> {
            ProfileAndSettingsTab(
              uiState = uiState,
              isDarkTheme = isDarkTheme,
              onToggleDarkTheme = onToggleDarkTheme,
              onUpdateProfile = { u, d, dob, bio, av, priv ->
                viewModel.updateProfile(u, d, dob, bio, av, priv)
              },
              onSubmitBlueTick = { cat, reason -> viewModel.submitBlueTickRequest(cat, reason) },
              onLogoutConfirmed = onLogout,
              onDeleteAccountConfirmed = { viewModel.deleteOwnAccount(onLogout) },
            )
          }
        }

        // Status Toast Banner
        AnimatedVisibility(
          visible = uiState.statusBanner != null,
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 12.dp, start = 16.dp, end = 16.dp),
        ) {
          Surface(
            color = AMediaViolet,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = uiState.statusBanner.orEmpty(),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f, fill = false),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = Color.White,
                modifier = Modifier
                  .size(16.dp)
                  .clickable { onDismissBanner() },
              )
            }
          }
        }
      }
    }

    // Full-Screen 24h Story Viewer Overlay
    if (activeStoryViewer != null) {
      FullScreenStoryViewerModal(
        story = activeStoryViewer,
        onDismiss = { onOpenStoryViewer(null) },
      )
    }
  }

  // Real-time Notifications Bottom Sheet
  if (showNotificationsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showNotificationsSheet = false },
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 10.dp),
      ) {
        Text(
          text = "Real-Time Notifications",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Live updates for likes, comments, follows, requests, verification & broadcasts",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 28.dp),
        ) {
          items(uiState.notifications, key = { it.id }) { notif ->
            Card(
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                UserAvatar(
                  avatarKey = notif.senderAvatar,
                  displayName = notif.senderUsername,
                  size = 42.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = notif.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                  )
                  Text(
                    text = "${notif.type.uppercase()} • ${formatRelativeTimestamp(notif.createdAt?.seconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Comments Bottom Sheet
  commentingPost?.let { post ->
    val livePost = uiState.feedPosts.find { it.id == post.id } ?: post
    var commentInput by remember { mutableStateOf("") }

    ModalBottomSheet(
      onDismissRequest = { commentingPost = null },
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 10.dp),
      ) {
        Text(
          text = "Comments (${livePost.commentsCount})",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (livePost.commentsPreview.isEmpty()) {
            item {
              Text(
                text = "No comments yet. Start the conversation!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            items(livePost.commentsPreview) { commentLine ->
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = commentLine,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = commentInput,
            onValueChange = { commentInput = it },
            placeholder = { Text("Add a comment for @${livePost.authorUsername}...") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("comment_input_field"),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (commentInput.isNotBlank()) {
                viewModel.addComment(livePost, commentInput.trim())
                commentInput = ""
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = AMediaCoral),
            modifier = Modifier.testTag("submit_comment_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment")
          }
        }
      }
    }
  }

  // Report Post Dialog
  reportingPost?.let { post ->
    var reason by remember { mutableStateOf("Spam or misleading content") }
    AlertDialog(
      onDismissRequest = { reportingPost = null },
      title = { Text("Report Post to Moderation") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Report @${post.authorUsername}'s post to the AMedia Admin Moderation Queue.",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Reason for report") },
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.reportContent(
              targetId = post.id,
              targetType = "post",
              summary = "Post by @${post.authorUsername}: ${post.caption.take(40)}",
              reason = reason,
            )
            reportingPost = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Submit Report")
        }
      },
      dismissButton = {
        TextButton(onClick = { reportingPost = null }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun HomeFeedTab(
  uiState: AMediaUiState,
  onOpenStory: (Story) -> Unit,
  onAddStoryClick: () -> Unit,
  onLikeToggle: (Post) -> Unit,
  onCommentClick: (Post) -> Unit,
  onShareClick: (Post) -> Unit,
  onSaveToggle: (Post) -> Unit,
  onReportClick: (Post) -> Unit,
  onExploreCreatorsClick: () -> Unit,
) {
  val currentUser = uiState.currentUser
  val followedSet = remember(currentUser) {
    (currentUser?.followingIds.orEmpty() + listOfNotNull(currentUser?.userId)).toSet()
  }

  // Home feed shows posts only from followed users (and own posts)
  val followedFeedPosts = remember(uiState.feedPosts, followedSet) {
    uiState.feedPosts.filter { it.authorId in followedSet }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    // 24-Hour Stories Row
    item {
      Column(modifier = Modifier.padding(top = 12.dp)) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // Current user's "Your Story" button
          item {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clickable { onAddStoryClick() }
                .testTag("add_your_story_item"),
            ) {
              Box(contentAlignment = Alignment.BottomEnd) {
                UserAvatar(
                  avatarKey = currentUser?.avatarUrl ?: "avatar_aurora",
                  displayName = currentUser?.displayName ?: "You",
                  size = 64.dp,
                  hasStoryRing = false,
                )
                Box(
                  modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(AMediaCoral),
                  contentAlignment = Alignment.Center,
                ) {
                  Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                }
              }
              Spacer(modifier = Modifier.height(5.dp))
              Text(
                text = "Your Story",
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }

          items(uiState.activeStories, key = { it.id }) { story ->
            val viewed = currentUser?.userId in story.viewerIds
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clickable { onOpenStory(story) }
                .testTag("feed_story_${story.authorUsername}"),
            ) {
              UserAvatar(
                avatarKey = story.authorAvatar,
                displayName = story.authorDisplayName,
                size = 64.dp,
                hasStoryRing = true,
                isStoryViewed = viewed,
              )
              Spacer(modifier = Modifier.height(5.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = story.authorUsername.take(10),
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                )
                if (story.authorIsVerified) {
                  Spacer(modifier = Modifier.width(2.dp))
                  BlueVerifiedBadge(size = 12.dp)
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      }
    }

    if (followedFeedPosts.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "Your Followed Feed is Empty",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Follow creators in the Search tab or publish a new post to populate your home feed.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = onExploreCreatorsClick,
              colors = ButtonDefaults.buttonColors(containerColor = AMediaCoral),
            ) {
              Text("Discover Creators")
            }
          }
        }
      }
    } else {
      items(followedFeedPosts, key = { it.id }) { post ->
        val isLiked = currentUser?.userId in post.likedByIds
        val isSaved = currentUser?.savedPostIds?.contains(post.id) == true

        Card(
          shape = RoundedCornerShape(26.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("feed_post_card_${post.id}"),
        ) {
          Column {
            // Post Header
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              UserAvatar(
                avatarKey = post.authorAvatar,
                displayName = post.authorDisplayName,
                size = 42.dp,
                hasStoryRing = true,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = post.authorDisplayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  if (post.authorIsVerified) {
                    Spacer(modifier = Modifier.width(5.dp))
                    BlueVerifiedBadge(size = 16.dp)
                  }
                }
                Text(
                  text = "@${post.authorUsername} • ${formatRelativeTimestamp(post.createdAt?.seconds)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(
                onClick = { onReportClick(post) },
                modifier = Modifier.testTag("report_post_${post.id}"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Flag,
                  contentDescription = "Report post",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
              }
            }

            // Post Visual Artwork / Photo
            AMediaPostVisual(
              imageUrl = post.imageUrl,
              caption = post.caption,
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
            )

            // Post Actions Bar (Like, Comment, Share, Save)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = { onLikeToggle(post) },
                modifier = Modifier.testTag("like_post_${post.id}"),
              ) {
                Icon(
                  imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                  contentDescription = if (isLiked) "Unlike" else "Like",
                  tint = if (isLiked) AMediaCoral else MaterialTheme.colorScheme.onSurface,
                )
              }
              Text(
                text = post.likedByIds.size.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )

              Spacer(modifier = Modifier.width(8.dp))

              IconButton(
                onClick = { onCommentClick(post) },
                modifier = Modifier.testTag("comment_post_${post.id}"),
              ) {
                Icon(
                  imageVector = Icons.Filled.ChatBubbleOutline,
                  contentDescription = "Comment",
                )
              }
              Text(
                text = post.commentsCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )

              Spacer(modifier = Modifier.width(8.dp))

              IconButton(
                onClick = { onShareClick(post) },
                modifier = Modifier.testTag("share_post_${post.id}"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Share,
                  contentDescription = "Share post",
                )
              }
              Text(
                text = post.sharesCount.toString(),
                style = MaterialTheme.typography.labelLarge,
              )

              Spacer(modifier = Modifier.weight(1f))

              IconButton(
                onClick = { onSaveToggle(post) },
                modifier = Modifier.testTag("save_post_${post.id}"),
              ) {
                Icon(
                  imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                  contentDescription = if (isSaved) "Remove bookmark" else "Save post",
                  tint = if (isSaved) AMediaCyan else MaterialTheme.colorScheme.onSurface,
                )
              }
            }

            // Caption & Comments preview
            Column(
              modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            ) {
              Text(
                text = "@${post.authorUsername}  ${post.caption}",
                style = MaterialTheme.typography.bodyMedium,
              )
              if (post.commentsPreview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                post.commentsPreview.takeLast(2).forEach { comment ->
                  Text(
                    text = comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
