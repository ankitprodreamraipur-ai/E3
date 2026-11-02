package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val managedByUid: String = "",
  val username: String = "",
  val displayName: String = "",
  val dob: String = "",
  val bio: String = "",
  val avatarUrl: String = "avatar_aurora",
  val isPrivate: Boolean = false,
  val isVerified: Boolean = false,
  val isBanned: Boolean = false,
  val accountStatus: String = "active",
  val followersIds: List<String> = emptyList(),
  val followingIds: List<String> = emptyList(),
  val savedPostIds: List<String> = emptyList(),
  val postsCount: Long = 0L,
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Post(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val managedByUid: String = "",
  val authorUsername: String = "",
  val authorDisplayName: String = "",
  val authorAvatar: String = "avatar_aurora",
  val authorIsVerified: Boolean = false,
  val caption: String = "",
  val imageUrl: String = "preset_cyber_city",
  val likedByIds: List<String> = emptyList(),
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val sharesCount: Long = 0L,
  val audienceIds: List<String> = emptyList(),
  val commentsPreview: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Story(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val managedByUid: String = "",
  val authorUsername: String = "",
  val authorDisplayName: String = "",
  val authorAvatar: String = "avatar_aurora",
  val authorIsVerified: Boolean = false,
  val caption: String = "",
  val imageUrl: String = "preset_golden_coast",
  val viewerIds: List<String> = emptyList(),
  val audienceIds: List<String> = emptyList(),
  val expiresAtMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun isActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
    return expiresAtMillis > nowMillis
  }
}

@IgnoreExtraProperties
data class FollowRequest(
  @DocumentId val id: String = "",
  val fromUserId: String = "",
  val fromUsername: String = "",
  val fromDisplayName: String = "",
  val fromAvatar: String = "avatar_aurora",
  val toUserId: String = "",
  val toUsername: String = "",
  val participantIds: List<String> = emptyList(),
  val status: String = "pending",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatThread(
  @DocumentId val id: String = "",
  val participantIds: List<String> = emptyList(),
  val peerId: String = "",
  val peerUsername: String = "",
  val peerDisplayName: String = "",
  val peerAvatar: String = "avatar_aurora",
  val peerIsVerified: Boolean = false,
  val lastMessage: String = "",
  val lastSenderId: String = "",
  val unreadCount: Long = 0L,
  val isPeerTyping: Boolean = false,
  val lastSeenStatus: String = "seen",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val text: String = "",
  val seen: Boolean = false,
  val participantIds: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AppNotification(
  @DocumentId val id: String = "",
  val recipientId: String = "",
  val senderId: String = "",
  val senderUsername: String = "",
  val senderAvatar: String = "avatar_aurora",
  val type: String = "like",
  val message: String = "",
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class VerificationRequest(
  @DocumentId val id: String = "",
  val userId: String = "",
  val managedByUid: String = "",
  val username: String = "",
  val displayName: String = "",
  val avatarUrl: String = "avatar_aurora",
  val category: String = "Creator",
  val reason: String = "",
  val status: String = "pending",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ModerationReport(
  @DocumentId val id: String = "",
  val reporterId: String = "",
  val reporterUsername: String = "",
  val targetId: String = "",
  val targetType: String = "post",
  val targetSummary: String = "",
  val reason: String = "",
  val status: String = "pending",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AdminActivityLog(
  @DocumentId val id: String = "",
  val actorId: String = "",
  val actorName: String = "",
  val actionType: String = "verify_user",
  val description: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
