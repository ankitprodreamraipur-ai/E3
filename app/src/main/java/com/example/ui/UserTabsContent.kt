package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.FollowRequest
import com.example.data.Post
import com.example.data.Story
import com.example.data.UserProfile
import com.example.ui.components.AMediaPostVisual
import com.example.ui.components.AVATAR_PRESETS
import com.example.ui.components.BlueVerifiedBadge
import com.example.ui.components.POST_VISUAL_PRESETS
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.AMediaBlueTick
import com.example.ui.theme.AMediaCoral
import com.example.ui.theme.AMediaCyan
import com.example.ui.theme.AMediaEmerald
import com.example.ui.theme.AMediaGold
import com.example.ui.theme.AMediaViolet
import kotlinx.coroutines.delay

@Composable
fun SearchAndRequestsTab(
  uiState: AMediaUiState,
  onFollowToggle: (UserProfile) -> Unit,
  onRespondRequest: (FollowRequest, Boolean) -> Unit,
  onStartChat: (UserProfile) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var filterMode by remember { mutableStateOf("all") } // all, verified, private, following
  val currentUser = uiState.currentUser

  val pendingIncoming = remember(uiState.followRequests, currentUser) {
    uiState.followRequests.filter {
      it.toUserId == currentUser?.userId && it.status == "pending"
    }
  }
  val pendingOutgoingIds = remember(uiState.followRequests, currentUser) {
    uiState.followRequests
      .filter { it.fromUserId == currentUser?.userId && it.status == "pending" }
      .map { it.toUserId }
      .toSet()
  }

  val otherUsers = remember(uiState.allUsers, currentUser, searchQuery, filterMode) {
    uiState.allUsers
      .filter { it.userId != currentUser?.userId }
      .filter { user ->
        val matchesQuery = searchQuery.isBlank() ||
          user.username.contains(searchQuery, ignoreCase = true) ||
          user.displayName.contains(searchQuery, ignoreCase = true) ||
          user.bio.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filterMode) {
          "verified" -> user.isVerified
          "private" -> user.isPrivate
          "following" -> currentUser?.followingIds?.contains(user.userId) == true
          else -> true
        }
        matchesQuery && matchesFilter
      }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search friends by @username, name, or bio...") },
        leadingIcon = {
          Icon(Icons.Filled.Search, contentDescription = "Search users")
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Filled.Close, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("search_friends_input"),
      )
    }

    // Live search suggestions / quick filter pills
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val filters = listOf(
          "all" to "All Creators (${uiState.allUsers.count { it.userId != currentUser?.userId }})",
          "verified" to "Verified ✓",
          "following" to "Following",
          "private" to "Private Accounts",
        )
        items(filters) { (key, label) ->
          FilterChip(
            selected = filterMode == key,
            onClick = { filterMode = key },
            label = { Text(label) },
          )
        }
      }
    }

    // Incoming Follow Requests section
    if (pendingIncoming.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
          ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = "Follow requests",
                tint = AMediaCoral,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Follow Requests (${pendingIncoming.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            pendingIncoming.forEach { req ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                UserAvatar(
                  avatarKey = req.fromAvatar,
                  displayName = req.fromDisplayName,
                  size = 44.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = req.fromDisplayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "@${req.fromUsername} wants to follow you",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Button(
                  onClick = { onRespondRequest(req, true) },
                  colors = ButtonDefaults.buttonColors(containerColor = AMediaEmerald),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("approve_request_${req.id}"),
                ) {
                  Text("Approve", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                  onClick = { onRespondRequest(req, false) },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Text("Reject", style = MaterialTheme.typography.labelMedium)
                }
              }
            }
          }
        }
      }
    }

    // Live search results
    items(otherUsers, key = { it.userId.ifBlank { it.id } }) { user ->
      val isFollowing = currentUser?.followingIds?.contains(user.userId) == true
      val isFollower = currentUser?.followersIds?.contains(user.userId) == true
      val isRequested = user.userId in pendingOutgoingIds
      val canChat = isFollowing || isFollower

      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
              avatarKey = user.avatarUrl,
              displayName = user.displayName,
              size = 52.dp,
              hasStoryRing = true,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = user.displayName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (user.isVerified) {
                  Spacer(modifier = Modifier.width(5.dp))
                  BlueVerifiedBadge(size = 17.dp)
                }
                if (user.isPrivate) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Private Account",
                    modifier = Modifier.size(14.dp),
                    tint = AMediaGold,
                  )
                }
              }
              Text(
                text = "@${user.username} • ${user.followersIds.size} followers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Button(
              onClick = { onFollowToggle(user) },
              enabled = !isRequested,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = when {
                  isFollowing -> MaterialTheme.colorScheme.surface
                  isRequested -> AMediaGold.copy(alpha = 0.35f)
                  else -> AMediaCoral
                },
                contentColor = when {
                  isFollowing -> MaterialTheme.colorScheme.onSurface
                  else -> Color.White
                },
              ),
              modifier = Modifier.testTag("follow_btn_${user.username}"),
            ) {
              Text(
                text = when {
                  isFollowing -> "Following"
                  isRequested -> "Requested"
                  user.isPrivate -> "Request"
                  else -> "Follow"
                },
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }

          if (user.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = user.bio,
              style = MaterialTheme.typography.bodyMedium,
            )
          }

          if (canChat) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
            ) {
              TextButton(
                onClick = { onStartChat(user) },
                modifier = Modifier.testTag("message_friend_${user.username}"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Message friend",
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Message @${user.username}")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun CreatePostOrStoryTab(
  onPublishPost: (caption: String, imageUrl: String) -> Unit,
  onPublishStory: (caption: String, imageUrl: String) -> Unit,
) {
  var isStoryMode by remember { mutableStateOf(false) }
  var selectedVisual by remember { mutableStateOf(POST_VISUAL_PRESETS.first().key) }
  var caption by remember { mutableStateOf("") }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedVisual = uri.toString()
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = if (isStoryMode) "Create 24-Hour Disappearing Story" else "Create New Feed Post",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
    )

    // Mode Switcher
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(4.dp),
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(if (!isStoryMode) AMediaCoral else Color.Transparent)
          .clickable { isStoryMode = false }
          .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Feed Post",
          color = if (!isStoryMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Bold,
        )
      }
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(if (isStoryMode) AMediaViolet else Color.Transparent)
          .clickable { isStoryMode = true }
          .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "24h Story",
          color = if (isStoryMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    // Live Visual Preview
    Card(
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(if (isStoryMode) 9f / 12f else 4f / 3f),
    ) {
      AMediaPostVisual(
        imageUrl = selectedVisual,
        caption = caption,
        modifier = Modifier.fillMaxSize(),
      )
    }

    // Device Gallery Picker + Curated Presets
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Choose Visual Artwork or Device Photo",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      OutlinedButton(
        onClick = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("pick_device_photo_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.AddPhotoAlternate,
          contentDescription = "Pick photo from gallery",
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Gallery Photo")
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      items(POST_VISUAL_PRESETS) { preset ->
        val isSelected = selectedVisual == preset.key
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isSelected) {
            AMediaCoral.copy(alpha = 0.18f)
          } else {
            MaterialTheme.colorScheme.surfaceVariant
          },
          modifier = Modifier
            .border(
              width = if (isSelected) 2.dp else 1.dp,
              color = if (isSelected) AMediaCoral else Color.Transparent,
              shape = RoundedCornerShape(16.dp),
            )
            .clickable { selectedVisual = preset.key },
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
              text = preset.label,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = preset.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    OutlinedTextField(
      value = caption,
      onValueChange = { caption = it },
      label = {
        Text(if (isStoryMode) "Story overlay caption..." else "Write a caption for your post...")
      },
      minLines = 3,
      shape = RoundedCornerShape(18.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("post_caption_input"),
    )

    Button(
      onClick = {
        val finalCaption = caption.ifBlank {
          if (isStoryMode) "24h moment on AMedia ✨" else "Captured on AMedia ✨"
        }
        if (isStoryMode) {
          onPublishStory(finalCaption, selectedVisual)
        } else {
          onPublishPost(finalCaption, selectedVisual)
        }
        caption = ""
      },
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = if (isStoryMode) AMediaViolet else AMediaCoral,
        contentColor = Color.White,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("publish_content_button"),
    ) {
      Icon(Icons.Filled.AutoAwesome, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (isStoryMode) "Share 24-Hour Story" else "Publish Post to Feed",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun StoriesGalleryTab(
  stories: List<Story>,
  currentUserId: String,
  onOpenStory: (Story) -> Unit,
  onCreateStoryClick: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = "24-Hour Live Stories",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Stories automatically disappear 24 hours after posting",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Button(
        onClick = onCreateStoryClick,
        colors = ButtonDefaults.buttonColors(containerColor = AMediaCoral),
        shape = RoundedCornerShape(14.dp),
      ) {
        Text("+ Add Story")
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (stories.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "No active stories in the last 24 hours. Share the first one!",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      val nowMs = System.currentTimeMillis()
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 155.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(stories, key = { it.id }) { story ->
          val hoursLeft = ((story.expiresAtMillis - nowMs).coerceAtLeast(0L) / 3600_000L) + 1L
          val viewed = currentUserId in story.viewerIds
          Card(
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(9f / 14f)
              .clickable { onOpenStory(story) }
              .testTag("story_card_${story.id}"),
          ) {
            Box(modifier = Modifier.fillMaxSize()) {
              AMediaPostVisual(
                imageUrl = story.imageUrl,
                caption = story.caption,
                modifier = Modifier.fillMaxSize(),
              )
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      listOf(
                        Color.Black.copy(alpha = 0.35f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.8f),
                      )
                    )
                  )
              )
              Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(10.dp),
              ) {
                Text(
                  text = "${hoursLeft}h left",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }

              Column(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .padding(12.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  UserAvatar(
                    avatarKey = story.authorAvatar,
                    displayName = story.authorDisplayName,
                    size = 34.dp,
                    hasStoryRing = true,
                    isStoryViewed = viewed,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "@${story.authorUsername}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (story.authorIsVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    BlueVerifiedBadge(size = 15.dp)
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = story.caption,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.9f),
                  maxLines = 2,
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

@Composable
fun FullScreenStoryViewerModal(
  story: Story,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  var progress by remember(story.id) { mutableFloatStateOf(0f) }

  LaunchedEffect(story.id) {
    progress = 0f
    val steps = 60
    for (i in 1..steps) {
      delay(80L)
      progress = i / steps.toFloat()
    }
    onDismiss()
  }

  val hoursLeft = ((story.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L) / 3600_000L) + 1L

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black),
  ) {
    AMediaPostVisual(
      imageUrl = story.imageUrl,
      caption = story.caption,
      modifier = Modifier.fillMaxSize(),
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color.Black.copy(alpha = 0.65f),
              Color.Transparent,
              Color.Black.copy(alpha = 0.82f),
            )
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 42.dp, start = 16.dp, end = 16.dp),
    ) {
      LinearProgressIndicator(
        progress = { progress },
        color = AMediaCoral,
        trackColor = Color.White.copy(alpha = 0.3f),
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(50)),
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        UserAvatar(
          avatarKey = story.authorAvatar,
          displayName = story.authorDisplayName,
          size = 42.dp,
          hasStoryRing = true,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = story.authorDisplayName,
              color = Color.White,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            if (story.authorIsVerified) {
              Spacer(modifier = Modifier.width(5.dp))
              BlueVerifiedBadge(size = 16.dp)
            }
          }
          Text(
            text = "@${story.authorUsername} • Expires in ${hoursLeft}h",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodySmall,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_story_viewer_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close story",
            tint = Color.White,
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth()
        .padding(24.dp),
    ) {
      Surface(
        color = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = story.caption,
          color = Color.White,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(16.dp),
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.Visibility,
          contentDescription = "Viewers",
          tint = Color.White.copy(alpha = 0.8f),
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${story.viewerIds.size} viewers • 24h auto-disappearing story",
          color = Color.White.copy(alpha = 0.8f),
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }
  }
}

@Composable
fun FriendChatTab(
  uiState: AMediaUiState,
  activeThread: ChatThread?,
  messages: List<ChatMessage>,
  onOpenThread: (ChatThread?) -> Unit,
  onStartChatWithFriend: (UserProfile) -> Unit,
  onSendMessage: (String) -> Unit,
  onTypingChange: (Boolean) -> Unit,
) {
  val currentUser = uiState.currentUser

  if (activeThread != null) {
    BackHandler {
      onTypingChange(false)
      onOpenThread(null)
    }

    var messageText by remember { mutableStateOf("") }
    var simulateFriendTyping by remember { mutableStateOf(false) }
    val emojis = listOf("❤️", "🔥", "😂", "✨", "👏", "🙌", "🎉", "💯")

    Column(modifier = Modifier.fillMaxSize()) {
      // Conversation Top Bar
      Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = {
            onTypingChange(false)
            onOpenThread(null)
          }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to chats")
          }
          UserAvatar(
            avatarKey = activeThread.peerAvatar,
            displayName = activeThread.peerDisplayName,
            size = 40.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = activeThread.peerDisplayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              if (activeThread.peerIsVerified) {
                Spacer(modifier = Modifier.width(4.dp))
                BlueVerifiedBadge(size = 16.dp)
              }
            }
            val peerTyping = simulateFriendTyping || activeThread.isPeerTyping
            Text(
              text = if (peerTyping) "typing..." else "@${activeThread.peerUsername} • Active now",
              style = MaterialTheme.typography.bodySmall,
              color = if (peerTyping) AMediaCyan else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = if (peerTyping) FontWeight.Bold else FontWeight.Normal,
            )
          }

          FilterChip(
            selected = simulateFriendTyping,
            onClick = { simulateFriendTyping = !simulateFriendTyping },
            label = { Text("Typing Demo") },
          )
        }
      }

      // Messages list
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(messages, key = { it.id }) { msg ->
          val isMe = msg.senderId == currentUser?.userId
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
          ) {
            Surface(
              shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp,
              ),
              color = if (isMe) AMediaCoral else MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                  text = msg.text,
                  color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.End,
                ) {
                  Text(
                    text = formatRelativeTimestamp(msg.createdAt?.seconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) {
                      Color.White.copy(alpha = 0.78f)
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    },
                  )
                  if (isMe) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                      imageVector = Icons.Filled.DoneAll,
                      contentDescription = "Seen status",
                      tint = Color.White,
                      modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                      text = "Seen",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.9f),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Emoji Quick Bar
      LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(emojis) { emoji ->
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable {
              messageText += emoji
              onTypingChange(true)
            },
          ) {
            Text(
              text = emoji,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }
      }

      // Message Composer
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = messageText,
          onValueChange = {
            messageText = it
            onTypingChange(it.isNotBlank())
          },
          placeholder = { Text("Message @${activeThread.peerUsername}...") },
          shape = RoundedCornerShape(24.dp),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("chat_message_input"),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = {
            if (messageText.isNotBlank()) {
              onSendMessage(messageText.trim())
              messageText = ""
              onTypingChange(false)
            }
          },
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = AMediaCoral),
          contentPadding = PaddingValues(14.dp),
          modifier = Modifier.testTag("send_chat_message_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send message",
            tint = Color.White,
          )
        }
      }
    }
    return
  }

  // Thread list + Mutual/Followed Friends selector
  val mutualOrFollowedFriends = remember(uiState.allUsers, currentUser) {
    uiState.allUsers.filter { user ->
      user.userId != currentUser?.userId &&
        (currentUser?.followingIds?.contains(user.userId) == true ||
          currentUser?.followersIds?.contains(user.userId) == true)
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text(
        text = "Real-Time Friend Messages",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "Chat exclusively with friends you follow or who follow you back",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (mutualOrFollowedFriends.isNotEmpty()) {
      item {
        Text(
          text = "Connected Friends",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(mutualOrFollowedFriends, key = { it.userId.ifBlank { it.id } }) { friend ->
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clickable { onStartChatWithFriend(friend) }
                .padding(4.dp),
            ) {
              UserAvatar(
                avatarKey = friend.avatarUrl,
                displayName = friend.displayName,
                size = 54.dp,
                hasStoryRing = true,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = friend.displayName.substringBefore(" "),
                  style = MaterialTheme.typography.labelMedium,
                )
                if (friend.isVerified) {
                  Spacer(modifier = Modifier.width(3.dp))
                  BlueVerifiedBadge(size = 13.dp)
                }
              }
            }
          }
        }
      }
    }

    items(uiState.chats, key = { it.id }) { thread ->
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenThread(thread) }
          .testTag("chat_thread_${thread.peerUsername}"),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          UserAvatar(
            avatarKey = thread.peerAvatar,
            displayName = thread.peerDisplayName,
            size = 52.dp,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = thread.peerDisplayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              if (thread.peerIsVerified) {
                Spacer(modifier = Modifier.width(5.dp))
                BlueVerifiedBadge(size = 16.dp)
              }
            }
            Text(
              text = thread.lastMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = formatRelativeTimestamp(thread.updatedAt?.seconds),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
              imageVector = Icons.Filled.DoneAll,
              contentDescription = "Seen",
              tint = AMediaBlueTick,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
fun ProfileAndSettingsTab(
  uiState: AMediaUiState,
  isDarkTheme: Boolean,
  onToggleDarkTheme: () -> Unit,
  onUpdateProfile: (
    username: String,
    displayName: String,
    dob: String,
    bio: String,
    avatarUrl: String,
    isPrivate: Boolean,
  ) -> Unit,
  onSubmitBlueTick: (category: String, reason: String) -> Unit,
  onLogoutConfirmed: () -> Unit,
  onDeleteAccountConfirmed: () -> Unit,
) {
  val me = uiState.currentUser ?: return
  var showEditDialog by remember { mutableStateOf(false) }
  var showVerifyDialog by remember { mutableStateOf(false) }
  var showLogoutConfirm by remember { mutableStateOf(false) }
  var showDeleteConfirm by remember { mutableStateOf(false) }
  var selectedSubTab by remember { mutableStateOf("my_posts") } // my_posts, saved

  val myPosts = remember(uiState.feedPosts, me.userId) {
    uiState.feedPosts.filter { it.authorId == me.userId }
  }
  val savedPosts = remember(uiState.feedPosts, me.savedPostIds) {
    uiState.feedPosts.filter { it.id in me.savedPostIds }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Profile Card Header
    item {
      Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
              avatarKey = me.avatarUrl,
              displayName = me.displayName,
              size = 76.dp,
              hasStoryRing = true,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = me.displayName,
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                )
                if (me.isVerified) {
                  Spacer(modifier = Modifier.width(6.dp))
                  BlueVerifiedBadge(size = 22.dp)
                }
              }
              Text(
                text = "@${me.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = if (me.isPrivate) AMediaGold.copy(alpha = 0.2f) else AMediaEmerald.copy(alpha = 0.2f),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = if (me.isPrivate) Icons.Filled.Lock else Icons.Filled.Public,
                      contentDescription = null,
                      tint = if (me.isPrivate) AMediaGold else AMediaEmerald,
                      modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (me.isPrivate) "Private Account" else "Public Account",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (me.isPrivate) AMediaGold else AMediaEmerald,
                    )
                  }
                }
                if (me.dob.isNotBlank()) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Filled.Cake,
                      contentDescription = "Date of Birth",
                      modifier = Modifier.size(14.dp),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = me.dob,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = me.bio,
            style = MaterialTheme.typography.bodyMedium,
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Stats Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
              .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            ProfileStatItem(count = myPosts.size.toString(), label = "Posts")
            ProfileStatItem(count = me.followersIds.size.toString(), label = "Followers")
            ProfileStatItem(count = me.followingIds.size.toString(), label = "Following")
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { showEditDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = AMediaCoral),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("edit_profile_button"),
            ) {
              Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Edit Profile")
            }

            OutlinedButton(
              onClick = { showVerifyDialog = true },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("request_blue_tick_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = AMediaBlueTick,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (me.isVerified) "Verified ✓" else "Blue Tick")
            }
          }
        }
      }
    }

    // Account Preferences & Security Controls
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = "Theme toggle",
                tint = AMediaViolet,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Appearance Theme",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = if (isDarkTheme) "Obsidian Dark Mode active" else "Daylight Mode active",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Switch(
              checked = isDarkTheme,
              onCheckedChange = { onToggleDarkTheme() },
              modifier = Modifier.testTag("profile_theme_switch"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = { showLogoutConfirm = true },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("logout_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Logout")
            }

            OutlinedButton(
              onClick = { showDeleteConfirm = true },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("delete_account_button"),
            ) {
              Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Delete Account")
            }
          }
        }
      }
    }

    // My Posts / Saved Bookmarks Switcher
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FilterChip(
          selected = selectedSubTab == "my_posts",
          onClick = { selectedSubTab = "my_posts" },
          leadingIcon = { Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
          label = { Text("My Posts (${myPosts.size})") },
        )
        FilterChip(
          selected = selectedSubTab == "saved",
          onClick = { selectedSubTab = "saved" },
          leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp)) },
          label = { Text("Saved Bookmarks (${savedPosts.size})") },
        )
      }
    }

    val displayedPosts = if (selectedSubTab == "my_posts") myPosts else savedPosts
    items(displayedPosts, key = { it.id }) { post ->
      Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          AMediaPostVisual(
            imageUrl = post.imageUrl,
            caption = post.caption,
            modifier = Modifier
              .fillMaxWidth()
              .height(190.dp),
          )
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = post.caption,
              style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${post.likedByIds.size} likes • ${post.commentsCount} comments • ${formatRelativeTimestamp(post.createdAt?.seconds)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }

  // Edit Profile Dialog
  if (showEditDialog) {
    var editDisplayName by remember { mutableStateOf(me.displayName) }
    var editUsername by remember { mutableStateOf(me.username) }
    var editDob by remember { mutableStateOf(me.dob) }
    var editBio by remember { mutableStateOf(me.bio) }
    var editAvatar by remember { mutableStateOf(me.avatarUrl) }
    var editIsPrivate by remember { mutableStateOf(me.isPrivate) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        editAvatar = uri.toString()
      }
    }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      title = { Text("Edit AMedia Profile") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(avatarKey = editAvatar, displayName = editDisplayName, size = 56.dp)
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
              onClick = {
                avatarPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              }
            ) {
              Text("Pick Photo")
            }
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(AVATAR_PRESETS) { (key, label) ->
              FilterChip(
                selected = editAvatar == key,
                onClick = { editAvatar = key },
                label = { Text(label) },
              )
            }
          }
          OutlinedTextField(
            value = editDisplayName,
            onValueChange = { editDisplayName = it },
            label = { Text("Full Name") },
            singleLine = true,
          )
          OutlinedTextField(
            value = editUsername,
            onValueChange = { editUsername = it },
            label = { Text("Username") },
            singleLine = true,
          )
          OutlinedTextField(
            value = editDob,
            onValueChange = { editDob = it },
            label = { Text("Date of Birth (YYYY-MM-DD)") },
            singleLine = true,
          )
          OutlinedTextField(
            value = editBio,
            onValueChange = { editBio = it },
            label = { Text("Bio") },
            minLines = 2,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Private Account", fontWeight = FontWeight.Bold)
              Text(
                "Require approval before followers see your content",
                style = MaterialTheme.typography.bodySmall,
              )
            }
            Switch(
              checked = editIsPrivate,
              onCheckedChange = { editIsPrivate = it },
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateProfile(
              editUsername.trim(),
              editDisplayName.trim(),
              editDob.trim(),
              editBio.trim(),
              editAvatar,
              editIsPrivate,
            )
            showEditDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = AMediaCoral),
        ) {
          Text("Save Changes")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }

  // Blue Tick Verification Request Dialog
  if (showVerifyDialog) {
    var category by remember { mutableStateOf("Digital Creator & Artist") }
    var reason by remember { mutableStateOf("Official AMedia creator account.") }

    AlertDialog(
      onDismissRequest = { showVerifyDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          BlueVerifiedBadge(size = 22.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Blue Tick Verification")
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Submit a verification request to the AMedia Admin Panel. Once approved, a Blue Tick badge is automatically assigned across your profile, posts, stories, and chats.",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category (e.g. Creator, Brand, Public Figure)") },
            singleLine = true,
          )
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Verification Statement / Portfolio Link") },
            minLines = 2,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSubmitBlueTick(category, reason)
            showVerifyDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = AMediaBlueTick),
        ) {
          Text("Submit Request")
        }
      },
      dismissButton = {
        TextButton(onClick = { showVerifyDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }

  // Logout Confirmation Dialog
  if (showLogoutConfirm) {
    AlertDialog(
      onDismissRequest = { showLogoutConfirm = false },
      title = { Text("Log Out of AMedia?") },
      text = { Text("Are you sure you want to log out of your AMedia session?") },
      confirmButton = {
        Button(
          onClick = {
            showLogoutConfirm = false
            onLogoutConfirmed()
          },
          colors = ButtonDefaults.buttonColors(containerColor = AMediaCoral),
        ) {
          Text("Log Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirm = false }) {
          Text("Cancel")
        }
      },
    )
  }

  // Delete Account Confirmation Dialog
  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete Account Permanently?") },
      text = {
        Text("This will permanently remove your AMedia profile, authored posts, and 24-hour stories. This action cannot be undone.")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDeleteAccountConfirmed()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Delete Forever")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun ProfileStatItem(count: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = count,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
