package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AdminActivityLog
import com.example.data.ModerationReport
import com.example.data.Post
import com.example.data.Story
import com.example.data.UserProfile
import com.example.data.VerificationRequest
import com.example.ui.components.AMediaPostVisual
import com.example.ui.components.BlueVerifiedBadge
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.AMediaBlueTick
import com.example.ui.theme.AMediaCoral
import com.example.ui.theme.AMediaCyan
import com.example.ui.theme.AMediaEmerald
import com.example.ui.theme.AMediaGold
import com.example.ui.theme.AMediaViolet

@Composable
fun AdminPanelScreen(
  uiState: AMediaUiState,
  selectedSection: AdminSection,
  isDarkTheme: Boolean,
  onSelectSection: (AdminSection) -> Unit,
  onToggleDarkTheme: () -> Unit,
  onExitAdminPanel: () -> Unit,
  onDismissBanner: () -> Unit,
  viewModel: AMediaViewModel,
) {
  BackHandler(onBack = onExitAdminPanel)

  var isSidebarExpanded by remember { mutableStateOf(false) }
  var inspectedUser by remember { mutableStateOf<UserProfile?>(null) }
  var userPendingDelete by remember { mutableStateOf<UserProfile?>(null) }
  var postPendingDelete by remember { mutableStateOf<Post?>(null) }
  var storyPendingDelete by remember { mutableStateOf<Story?>(null) }

  val sections = listOf(
    Triple(AdminSection.OVERVIEW, "Overview", Icons.Filled.Analytics),
    Triple(AdminSection.USERS, "Users (${uiState.allUsers.size})", Icons.Filled.Group),
    Triple(AdminSection.MODERATION, "Moderation", Icons.Filled.Policy),
    Triple(
      AdminSection.VERIFICATIONS,
      "Blue Tick (${uiState.verificationRequests.count { it.status == "pending" }})",
      Icons.Filled.Verified
    ),
    Triple(
      AdminSection.REPORTS,
      "Reports (${uiState.reports.count { it.status == "pending" }})",
      Icons.Filled.Flag
    ),
    Triple(AdminSection.BROADCAST_LOGS, "Broadcast & Logs", Icons.Filled.Campaign),
  )

  Scaffold(
    contentWindowInsets = WindowInsets.systemBars,
    topBar = {
      Surface(
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = { isSidebarExpanded = !isSidebarExpanded },
              modifier = Modifier.testTag("toggle_admin_sidebar_button"),
            ) {
              Icon(
                imageVector = if (isSidebarExpanded) Icons.Filled.MenuOpen else Icons.Filled.Menu,
                contentDescription = "Toggle Admin Sidebar",
                tint = AMediaGold,
              )
            }

            Icon(
              imageVector = Icons.Filled.AdminPanelSettings,
              contentDescription = null,
              tint = AMediaGold,
              modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "AMedia Control Panel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Real-Time Ecosystem & Moderation Dashboard",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            IconButton(onClick = onToggleDarkTheme) {
              Icon(
                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "Toggle theme",
              )
            }

            OutlinedButton(
              onClick = onExitAdminPanel,
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("exit_admin_panel_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to User App",
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("User App", style = MaterialTheme.typography.labelMedium)
            }
          }

          // Quick horizontal section switcher for mobile ergonomics
          LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(sections) { (section, label, icon) ->
              val selected = selectedSection == section
              FilterChip(
                selected = selected,
                onClick = { onSelectSection(section) },
                leadingIcon = {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag("admin_tab_${section.name.lowercase()}"),
              )
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
      }
    },
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      // Collapsible Control Panel Sidebar
      AnimatedVisibility(visible = isSidebarExpanded) {
        Surface(
          tonalElevation = 4.dp,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
          modifier = Modifier
            .width(220.dp)
            .fillMaxHeight(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "ADMIN NAVIGATION",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
            sections.forEach { (section, label, icon) ->
              val isSelected = selectedSection == section
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) AMediaCoral else Color.Transparent,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onSelectSection(section)
                    isSidebarExpanded = false
                  },
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  )
                }
              }
            }
          }
        }
      }

      // Main Dashboard Content Area
      Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
        when (selectedSection) {
          AdminSection.OVERVIEW -> {
            AdminOverviewSection(
              uiState = uiState,
              onNavigateToSection = onSelectSection,
            )
          }

          AdminSection.USERS -> {
            AdminUsersManagementSection(
              users = uiState.allUsers,
              onInspectUser = { inspectedUser = it },
              onToggleVerify = { viewModel.adminToggleUserVerification(it) },
              onToggleBan = { viewModel.adminToggleUserBan(it) },
              onRequestDelete = { userPendingDelete = it },
            )
          }

          AdminSection.MODERATION -> {
            AdminContentModerationSection(
              posts = uiState.feedPosts,
              stories = uiState.activeStories,
              onRequestDeletePost = { postPendingDelete = it },
              onRequestDeleteStory = { storyPendingDelete = it },
            )
          }

          AdminSection.VERIFICATIONS -> {
            AdminVerificationRequestsSection(
              requests = uiState.verificationRequests,
              onReviewRequest = { req, approve ->
                viewModel.adminReviewVerification(req, approve)
              },
            )
          }

          AdminSection.REPORTS -> {
            AdminReportsSection(
              reports = uiState.reports,
              onUpdateStatus = { report, status ->
                viewModel.adminUpdateReport(report, status)
              },
            )
          }

          AdminSection.BROADCAST_LOGS -> {
            AdminBroadcastAndLogsSection(
              logs = uiState.activityLogs,
              onSendBroadcast = { title, msg ->
                viewModel.adminBroadcastAnnouncement(title, msg)
              },
            )
          }
        }

        // Status banner inside Admin Panel
        androidx.compose.animation.AnimatedVisibility(
          visible = uiState.statusBanner != null,
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 10.dp, start = 16.dp, end = 16.dp),
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
  }

  // User Profile Inspection Modal
  inspectedUser?.let { user ->
    AlertDialog(
      onDismissRequest = { inspectedUser = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          UserAvatar(avatarKey = user.avatarUrl, displayName = user.displayName, size = 44.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(user.displayName, fontWeight = FontWeight.Bold)
              if (user.isVerified) {
                Spacer(modifier = Modifier.width(5.dp))
                BlueVerifiedBadge(size = 18.dp)
              }
            }
            Text("@${user.username}", style = MaterialTheme.typography.bodySmall)
          }
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("User ID: ${user.userId.ifBlank { user.id }}", style = MaterialTheme.typography.labelSmall)
          Text("Bio: ${user.bio}")
          Text("Date of Birth: ${user.dob.ifBlank { "Not specified" }}")
          Text("Account Privacy: ${if (user.isPrivate) "Private 🔒" else "Public 🌐"}")
          Text("Verification Status: ${if (user.isVerified) "Blue Tick Verified ✓" else "Standard"}")
          Text("Account Status: ${if (user.isBanned) "BANNED 🚫" else "Active ✅"}")
          Text("Followers: ${user.followersIds.size} • Following: ${user.followingIds.size}")
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.adminToggleUserVerification(user)
            inspectedUser = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = AMediaBlueTick),
        ) {
          Text(if (user.isVerified) "Remove Blue Tick" else "Assign Blue Tick ✓")
        }
      },
      dismissButton = {
        TextButton(onClick = { inspectedUser = null }) {
          Text("Close")
        }
      },
    )
  }

  // Confirm User Deletion Dialog
  userPendingDelete?.let { user ->
    AlertDialog(
      onDismissRequest = { userPendingDelete = null },
      title = { Text("Delete User @${user.username}?") },
      text = {
        Text("Are you sure you want to permanently remove @${user.username} from the AMedia ecosystem?")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.adminDeleteUser(user)
            userPendingDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Confirm Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { userPendingDelete = null }) {
          Text("Cancel")
        }
      },
    )
  }

  // Confirm Post Moderation Removal Dialog
  postPendingDelete?.let { post ->
    AlertDialog(
      onDismissRequest = { postPendingDelete = null },
      title = { Text("Remove Post by @${post.authorUsername}?") },
      text = { Text("This post will be immediately removed from all user feeds and logged in Admin Activity.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.adminRemovePost(post)
            postPendingDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Remove Post")
        }
      },
      dismissButton = {
        TextButton(onClick = { postPendingDelete = null }) {
          Text("Cancel")
        }
      },
    )
  }

  // Confirm Story Moderation Removal Dialog
  storyPendingDelete?.let { story ->
    AlertDialog(
      onDismissRequest = { storyPendingDelete = null },
      title = { Text("Remove 24h Story by @${story.authorUsername}?") },
      text = { Text("This 24-hour story will be immediately removed and logged in Admin Activity.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.adminRemoveStory(story)
            storyPendingDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Remove Story")
        }
      },
      dismissButton = {
        TextButton(onClick = { storyPendingDelete = null }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun AdminOverviewSection(
  uiState: AMediaUiState,
  onNavigateToSection: (AdminSection) -> Unit,
) {
  val totalUsers = uiState.allUsers.size
  val activeUsers = uiState.allUsers.count { !it.isBanned }
  val verifiedUsers = uiState.allUsers.count { it.isVerified }
  val postsCount = uiState.feedPosts.size
  val storiesCount = uiState.activeStories.size
  val reportsCount = uiState.reports.size
  val chatsCount = uiState.chats.size
  val activityCount = uiState.activityLogs.size

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text(
        text = "Platform Real-Time Statistics",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "Live Firestore metrics across users, verifications, posts, stories, chats, and reports",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // 8 Real-Time KPI Stat Cards in a 2-column grid layout
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          KpiStatCard(
            title = "Total Users",
            value = totalUsers.toString(),
            subtitle = "$activeUsers active now",
            accent = AMediaCoral,
            modifier = Modifier.weight(1f).clickable { onNavigateToSection(AdminSection.USERS) },
          )
          KpiStatCard(
            title = "Verified Users",
            value = verifiedUsers.toString(),
            subtitle = "Blue Tick Badges ✓",
            accent = AMediaBlueTick,
            modifier = Modifier.weight(1f).clickable { onNavigateToSection(AdminSection.VERIFICATIONS) },
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          KpiStatCard(
            title = "Active Users",
            value = activeUsers.toString(),
            subtitle = "${uiState.allUsers.count { it.isBanned }} banned",
            accent = AMediaEmerald,
            modifier = Modifier.weight(1f).clickable { onNavigateToSection(AdminSection.USERS) },
          )
          KpiStatCard(
            title = "Feed Posts",
            value = postsCount.toString(),
            subtitle = "${uiState.feedPosts.sumOf { it.likedByIds.size }} total likes",
            accent = AMediaViolet,
            modifier = Modifier.weight(1f).clickable { onNavigateToSection(AdminSection.MODERATION) },
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          KpiStatCard(
            title = "24h Stories",
            value = storiesCount.toString(),
            subtitle = "Auto-expiring live",
            accent = AMediaGold,
            modifier = Modifier.weight(1f).clickable { onNavigateToSection(AdminSection.MODERATION) },
          )
          KpiStatCard(
            title = "Reports Queue",
            value = reportsCount.toString(),
            subtitle = "${uiState.reports.count { it.status == "pending" }} pending review",
            accent = Color(0xFFEF4444),
            modifier = Modifier.weight(1f).clickable { onNavigateToSection(AdminSection.REPORTS) },
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          KpiStatCard(
            title = "Friend Chats",
            value = chatsCount.toString(),
            subtitle = "Real-time threads",
            accent = AMediaCyan,
            modifier = Modifier.weight(1f),
          )
          KpiStatCard(
            title = "Platform Activity",
            value = activityCount.toString(),
            subtitle = "Logged events",
            accent = AMediaCoral,
            modifier = Modifier.weight(1f).clickable { onNavigateToSection(AdminSection.BROADCAST_LOGS) },
          )
        }
      }
    }

    // Analytics Graph 1: Growth & Engagement Trend Line Chart
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "Growth, Engagement & Usage Trends",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "7-Day Active Users (Coral) vs Content Interactions (Cyan)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(14.dp))
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(170.dp),
          ) {
            val w = size.width
            val h = size.height
            val growthPoints = listOf(0.25f, 0.38f, 0.42f, 0.55f, 0.68f, 0.78f, 0.92f)
            val engagementPoints = listOf(0.18f, 0.30f, 0.48f, 0.44f, 0.62f, 0.74f, 0.88f)

            // Horizontal grid lines
            for (i in 1..4) {
              val y = h * (i / 5f)
              drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
              )
            }

            fun buildLinePath(points: List<Float>): Path {
              return Path().apply {
                points.forEachIndexed { idx, ratio ->
                  val x = idx * (w / (points.size - 1))
                  val y = h - (ratio * h * 0.85f) - h * 0.08f
                  if (idx == 0) moveTo(x, y) else lineTo(x, y)
                }
              }
            }

            val growthPath = buildLinePath(growthPoints)
            val engagePath = buildLinePath(engagementPoints)

            drawPath(
              path = growthPath,
              color = AMediaCoral,
              style = Stroke(width = 3.5.dp.toPx()),
            )
            drawPath(
              path = engagePath,
              color = AMediaCyan,
              style = Stroke(width = 3.5.dp.toPx()),
            )

            growthPoints.forEachIndexed { idx, ratio ->
              val x = idx * (w / (growthPoints.size - 1))
              val y = h - (ratio * h * 0.85f) - h * 0.08f
              drawCircle(color = AMediaCoral, radius = 5.dp.toPx(), center = Offset(x, y))
            }
          }
        }
      }
    }

    // Analytics Graph 2: Ecosystem Distribution Bar Chart
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "Content & Moderation Distribution",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(12.dp))
          val bars = listOf(
            Triple("Users", totalUsers, AMediaCoral),
            Triple("Verified", verifiedUsers, AMediaBlueTick),
            Triple("Posts", postsCount, AMediaViolet),
            Triple("Stories", storiesCount, AMediaGold),
            Triple("Chats", chatsCount, AMediaCyan),
            Triple("Reports", reportsCount, AMediaEmerald),
          )
          val maxVal = (bars.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp),
          ) {
            val w = size.width
            val h = size.height
            val slotW = w / bars.size
            bars.forEachIndexed { idx, (_, count, color) ->
              val ratio = (count.toFloat() / maxVal.toFloat()).coerceIn(0.12f, 1f)
              val barH = h * 0.82f * ratio
              val left = idx * slotW + slotW * 0.2f
              drawRoundRect(
                color = color,
                topLeft = Offset(left, h - barH),
                size = Size(slotW * 0.6f, barH),
                cornerRadius = CornerRadius(12f, 12f),
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            bars.forEach { (label, count, _) ->
              Text(
                text = "$label ($count)",
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

@Composable
private fun KpiStatCard(
  title: String,
  value: String,
  subtitle: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = accent,
      )
    }
  }
}

@Composable
private fun AdminUsersManagementSection(
  users: List<UserProfile>,
  onInspectUser: (UserProfile) -> Unit,
  onToggleVerify: (UserProfile) -> Unit,
  onToggleBan: (UserProfile) -> Unit,
  onRequestDelete: (UserProfile) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var filter by remember { mutableStateOf("all") } // all, verified, private, banned

  val filteredUsers = remember(users, searchQuery, filter) {
    users.filter { u ->
      val qMatch = searchQuery.isBlank() ||
        u.username.contains(searchQuery, ignoreCase = true) ||
        u.displayName.contains(searchQuery, ignoreCase = true)
      val fMatch = when (filter) {
        "verified" -> u.isVerified
        "private" -> u.isPrivate
        "banned" -> u.isBanned
        else -> true
      }
      qMatch && fMatch
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
        text = "User Management Directory",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search users by @username or display name...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_user_search_input"),
      )
      Spacer(modifier = Modifier.height(8.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val chips = listOf(
          "all" to "All (${users.size})",
          "verified" to "Blue Tick (${users.count { it.isVerified }})",
          "private" to "Private (${users.count { it.isPrivate }})",
          "banned" to "Banned (${users.count { it.isBanned }})",
        )
        items(chips) { (key, label) ->
          FilterChip(
            selected = filter == key,
            onClick = { filter = key },
            label = { Text(label) },
          )
        }
      }
    }

    items(filteredUsers, key = { it.userId.ifBlank { it.id } }) { user ->
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
              avatarKey = user.avatarUrl,
              displayName = user.displayName,
              size = 46.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = user.displayName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                if (user.isVerified) {
                  Spacer(modifier = Modifier.width(5.dp))
                  BlueVerifiedBadge(size = 17.dp)
                }
              }
              Text(
                text = "@${user.username} • ${if (user.isPrivate) "Private" else "Public"} • ${if (user.isBanned) "BANNED" else "Active"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (user.isBanned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(onClick = { onInspectUser(user) }) {
              Icon(Icons.Filled.Visibility, contentDescription = "View profile details")
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { onToggleVerify(user) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (user.isVerified) MaterialTheme.colorScheme.surface else AMediaBlueTick,
                contentColor = if (user.isVerified) MaterialTheme.colorScheme.onSurface else Color.White,
              ),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("admin_verify_user_${user.username}"),
            ) {
              Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (user.isVerified) "Unverify" else "Verify ✓",
                style = MaterialTheme.typography.labelMedium,
              )
            }

            OutlinedButton(
              onClick = { onToggleBan(user) },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("admin_ban_user_${user.username}"),
            ) {
              Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (user.isBanned) "Unban" else "Ban",
                style = MaterialTheme.typography.labelMedium,
              )
            }

            OutlinedButton(
              onClick = { onRequestDelete(user) },
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
              ),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.testTag("admin_delete_user_${user.username}"),
            ) {
              Icon(Icons.Filled.Delete, contentDescription = "Delete user", modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminContentModerationSection(
  posts: List<Post>,
  stories: List<Story>,
  onRequestDeletePost: (Post) -> Unit,
  onRequestDeleteStory: (Story) -> Unit,
) {
  var subTab by remember { mutableStateOf("posts") } // posts, stories

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text(
        text = "Content Moderation Queue",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "Review and remove posts or 24-hour stories that violate platform rules",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
          selected = subTab == "posts",
          onClick = { subTab = "posts" },
          label = { Text("Feed Posts (${posts.size})") },
        )
        FilterChip(
          selected = subTab == "stories",
          onClick = { subTab = "stories" },
          label = { Text("24h Stories (${stories.size})") },
        )
      }
    }

    if (subTab == "posts") {
      items(posts, key = { it.id }) { post ->
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
                .height(160.dp),
            )
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "@${post.authorUsername}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                if (post.authorIsVerified) {
                  Spacer(modifier = Modifier.width(4.dp))
                  BlueVerifiedBadge(size = 15.dp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                  onClick = { onRequestDeletePost(post) },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("admin_remove_post_${post.id}"),
                ) {
                  Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Remove Post", style = MaterialTheme.typography.labelMedium)
                }
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(post.caption, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    } else {
      items(stories, key = { it.id }) { story ->
        Card(
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
            ) {
              AMediaPostVisual(
                imageUrl = story.imageUrl,
                caption = story.caption,
                modifier = Modifier.fillMaxSize(),
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "@${story.authorUsername}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                if (story.authorIsVerified) {
                  Spacer(modifier = Modifier.width(4.dp))
                  BlueVerifiedBadge(size = 15.dp)
                }
              }
              Text(
                text = story.caption,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Button(
              onClick = { onRequestDeleteStory(story) },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("admin_remove_story_${story.id}"),
            ) {
              Text("Remove", style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminVerificationRequestsSection(
  requests: List<VerificationRequest>,
  onReviewRequest: (VerificationRequest, Boolean) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        BlueVerifiedBadge(size = 26.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Blue Tick Verification Requests",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
      }
      Text(
        text = "Approving a request automatically assigns the Blue Verification Badge to that user across AMedia",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    items(requests, key = { it.id }) { req ->
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
              avatarKey = req.avatarUrl,
              displayName = req.displayName,
              size = 48.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = req.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "@${req.username} • ${req.category}",
                style = MaterialTheme.typography.bodySmall,
                color = AMediaBlueTick,
              )
            }
            Surface(
              shape = RoundedCornerShape(50),
              color = when (req.status) {
                "approved" -> AMediaEmerald.copy(alpha = 0.2f)
                "rejected" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                else -> AMediaGold.copy(alpha = 0.2f)
              },
            ) {
              Text(
                text = req.status.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = when (req.status) {
                  "approved" -> AMediaEmerald
                  "rejected" -> MaterialTheme.colorScheme.error
                  else -> AMediaGold
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = req.reason,
            style = MaterialTheme.typography.bodyMedium,
          )

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { onReviewRequest(req, true) },
              enabled = req.status != "approved",
              colors = ButtonDefaults.buttonColors(containerColor = AMediaBlueTick),
              modifier = Modifier
                .weight(1f)
                .testTag("approve_verification_${req.username}"),
            ) {
              Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Approve & Assign Blue Tick")
            }

            OutlinedButton(
              onClick = { onReviewRequest(req, false) },
              enabled = req.status != "rejected",
              modifier = Modifier
                .weight(1f)
                .testTag("reject_verification_${req.username}"),
            ) {
              Text("Reject")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminReportsSection(
  reports: List<ModerationReport>,
  onUpdateStatus: (ModerationReport, String) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text(
        text = "Reports & Complaint Management",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "Track and resolve community reports with real-time status progression",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    items(reports, key = { it.id }) { report ->
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Reported by @${report.reporterUsername}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
            Surface(
              shape = RoundedCornerShape(50),
              color = AMediaGold.copy(alpha = 0.2f),
            ) {
              Text(
                text = report.status.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AMediaGold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = report.targetSummary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "Reason: ${report.reason}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
              "under_review" to "Under Review",
              "resolved" to "Resolve ✓",
              "dismissed" to "Dismiss",
            ).forEach { (statusKey, label) ->
              FilterChip(
                selected = report.status == statusKey,
                onClick = { onUpdateStatus(report, statusKey) },
                label = { Text(label) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminBroadcastAndLogsSection(
  logs: List<AdminActivityLog>,
  onSendBroadcast: (String, String) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var message by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Campaign, contentDescription = null, tint = AMediaCoral)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Broadcast Announcement to All Users",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Announcement Title") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_broadcast_title_input"),
          )
          OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Broadcast Message Content") },
            minLines = 2,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_broadcast_message_input"),
          )
          Button(
            onClick = {
              if (title.isNotBlank() && message.isNotBlank()) {
                onSendBroadcast(title.trim(), message.trim())
                title = ""
                message = ""
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = AMediaCoral),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("send_broadcast_button"),
          ) {
            Text("Send Broadcast Notification 📢", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.History, contentDescription = null, tint = AMediaCyan)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Real-Time Admin Activity Logs (${logs.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    items(logs, key = { it.id }) { log ->
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(AMediaCyan)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = log.actionType,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = log.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = formatRelativeTimestamp(log.createdAt?.seconds),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
