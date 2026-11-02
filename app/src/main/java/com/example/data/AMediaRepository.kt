package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AMediaRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  // --- REAL-TIME STREAMS (ALIGNED WITH FIRESTORE.RULES) ---

  fun observeCurrentUserProfile(userId: String): Flow<UserProfile?> {
    val path = "users/$userId"
    return db.collection("users").document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  fun observeActiveUsers(): Flow<List<UserProfile>> {
    val path = "users"
    return db.collection("users")
      .whereEqualTo("accountStatus", "active")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeManagedUsers(userId: String): Flow<List<UserProfile>> {
    val path = "users"
    return db.collection("users")
      .whereEqualTo("managedByUid", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeFeedPosts(userId: String): Flow<List<Post>> {
    val path = "posts"
    return db.collection("posts")
      .whereArrayContains("audienceIds", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeStories(userId: String): Flow<List<Story>> {
    val path = "stories"
    return db.collection("stories")
      .whereArrayContains("audienceIds", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeFollowRequests(userId: String): Flow<List<FollowRequest>> {
    val path = "follow_requests"
    return db.collection("follow_requests")
      .whereArrayContains("participantIds", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(FollowRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeChats(userId: String): Flow<List<ChatThread>> {
    val path = "chats"
    return db.collection("chats")
      .whereArrayContains("participantIds", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeMessages(chatId: String, userId: String): Flow<List<ChatMessage>> {
    val path = "messages"
    return db.collection("messages")
      .whereEqualTo("chatId", chatId)
      .whereArrayContains("participantIds", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeNotifications(userId: String): Flow<List<AppNotification>> {
    val path = "notifications"
    return db.collection("notifications")
      .whereEqualTo("recipientId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeVerificationRequests(userId: String): Flow<List<VerificationRequest>> {
    val path = "verification_requests"
    return db.collection("verification_requests")
      .whereEqualTo("managedByUid", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(VerificationRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeReports(userId: String): Flow<List<ModerationReport>> {
    val path = "reports"
    return db.collection("reports")
      .whereEqualTo("reporterId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeActivityLogs(userId: String): Flow<List<AdminActivityLog>> {
    val path = "activity_logs"
    return db.collection("activity_logs")
      .whereEqualTo("actorId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(AdminActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  // --- CRUD & ECOSYSTEM OPERATIONS ---

  suspend fun getProfileById(targetUserId: String): Result<UserProfile?> = runCatching {
    val docRef = db.collection("users").document(targetUserId)
    try {
      val snap = docRef.get().await()
      if (snap.exists()) {
        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun createOrEnsureUserProfile(
    username: String,
    displayName: String,
    dob: String = "1999-08-15",
    bio: String = "Creating visual stories & moments on AMedia ✨",
    avatarUrl: String = "avatar_aurora",
    isPrivate: Boolean = false,
  ): Result<UserProfile> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val existingSnap = docRef.get().await()
      if (existingSnap.exists()) {
        return@runCatching existingSnap.toObject(
          UserProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )!!
      }
      val sanitizedUsername = username.lowercase()
        .replace(Regex("[^a-z0-9_.]"), "")
        .take(28)
        .padEnd(3, 'x')
      val payload = mapOf(
        "userId" to uid,
        "managedByUid" to uid,
        "username" to sanitizedUsername,
        "displayName" to displayName.take(50).ifBlank { "AMedia Creator" },
        "dob" to dob.take(20),
        "bio" to bio.take(250),
        "avatarUrl" to avatarUrl.take(300),
        "isPrivate" to isPrivate,
        "isVerified" to false,
        "isBanned" to false,
        "accountStatus" to "active",
        "followersIds" to emptyList<String>(),
        "followingIds" to emptyList<String>(),
        "savedPostIds" to emptyList<String>(),
        "postsCount" to 0L,
        "followersCount" to 0L,
        "followingCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      val created = docRef.get().await()
      created.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)!!
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateUserProfile(
    username: String,
    displayName: String,
    dob: String,
    bio: String,
    avatarUrl: String,
    isPrivate: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val sanitizedUsername = username.lowercase()
      .replace(Regex("[^a-z0-9_.]"), "")
      .take(28)
      .padEnd(3, 'x')
    val updates = mapOf(
      "username" to sanitizedUsername,
      "displayName" to displayName.trim().take(50).ifBlank { "AMedia User" },
      "dob" to dob.trim().take(20),
      "bio" to bio.trim().take(250),
      "avatarUrl" to avatarUrl.take(300),
      "isPrivate" to isPrivate,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun createPost(
    authorProfile: UserProfile,
    caption: String,
    imageUrl: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val postRef = db.collection("posts").document()
    val audience = (authorProfile.followersIds + listOf(uid, authorProfile.userId))
      .distinct()
      .take(100)
    val payload = mapOf(
      "authorId" to authorProfile.userId.ifBlank { uid },
      "managedByUid" to uid,
      "authorUsername" to authorProfile.username.ifBlank { "amedia_user" }.take(40),
      "authorDisplayName" to authorProfile.displayName.ifBlank { "AMedia User" }.take(50),
      "authorAvatar" to authorProfile.avatarUrl.take(300),
      "authorIsVerified" to authorProfile.isVerified,
      "caption" to caption.trim().take(500),
      "imageUrl" to imageUrl.ifBlank { "preset_cyber_city" }.take(500),
      "likedByIds" to emptyList<String>(),
      "likesCount" to 0L,
      "commentsCount" to 0L,
      "sharesCount" to 0L,
      "audienceIds" to audience,
      "commentsPreview" to emptyList<String>(),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      postRef.set(payload).await()
      val userRef = db.collection("users").document(authorProfile.userId.ifBlank { uid })
      userRef.update(
        mapOf(
          "postsCount" to (authorProfile.postsCount + 1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      postRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, postRef.path)
      throw e
    }
  }

  suspend fun toggleLikePost(post: Post, currentUser: UserProfile): Result<Unit> = runCatching {
    val uid = requireUserId()
    val postRef = db.collection("posts").document(post.id)
    val alreadyLiked = uid in post.likedByIds
    val updatedLikes = if (alreadyLiked) {
      post.likedByIds - uid
    } else {
      (post.likedByIds + uid).distinct().take(100)
    }
    try {
      postRef.update(
        mapOf(
          "likedByIds" to updatedLikes,
          "likesCount" to updatedLikes.size.toLong(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      if (!alreadyLiked) {
        createNotification(
          recipientId = uid,
          senderUsername = currentUser.username.ifBlank { "you" },
          senderAvatar = currentUser.avatarUrl,
          type = "like",
          message = "You liked @${post.authorUsername}'s post: \"${post.caption.take(40)}\"",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, postRef.path)
      throw e
    }
  }

  suspend fun addCommentToPost(
    post: Post,
    currentUser: UserProfile,
    commentText: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanComment = commentText.trim().take(180)
    if (cleanComment.isBlank()) return@runCatching
    val postRef = db.collection("posts").document(post.id)
    val formatted = "@${currentUser.username.ifBlank { "user" }}: $cleanComment"
    val updatedComments = (post.commentsPreview + formatted).takeLast(50)
    try {
      postRef.update(
        mapOf(
          "commentsPreview" to updatedComments,
          "commentsCount" to (post.commentsCount + 1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      createNotification(
        recipientId = uid,
        senderUsername = currentUser.username.ifBlank { "you" },
        senderAvatar = currentUser.avatarUrl,
        type = "comment",
        message = "Commented on @${post.authorUsername}'s post: \"$cleanComment\"",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, postRef.path)
      throw e
    }
  }

  suspend fun incrementShareCount(post: Post): Result<Unit> = runCatching {
    requireUserId()
    val postRef = db.collection("posts").document(post.id)
    try {
      postRef.update(
        mapOf(
          "sharesCount" to (post.sharesCount + 1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, postRef.path)
      throw e
    }
  }

  suspend fun toggleSavePost(currentUser: UserProfile, postId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val updatedSaved = if (postId in currentUser.savedPostIds) {
      currentUser.savedPostIds - postId
    } else {
      (currentUser.savedPostIds + postId).distinct().take(100)
    }
    try {
      userRef.update(
        mapOf(
          "savedPostIds" to updatedSaved,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun createStory(
    authorProfile: UserProfile,
    caption: String,
    imageUrl: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val storyRef = db.collection("stories").document()
    val audience = (authorProfile.followersIds + listOf(uid, authorProfile.userId))
      .distinct()
      .take(100)
    val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
    val payload = mapOf(
      "authorId" to authorProfile.userId.ifBlank { uid },
      "managedByUid" to uid,
      "authorUsername" to authorProfile.username.ifBlank { "amedia_user" }.take(40),
      "authorDisplayName" to authorProfile.displayName.ifBlank { "AMedia User" }.take(50),
      "authorAvatar" to authorProfile.avatarUrl.take(300),
      "authorIsVerified" to authorProfile.isVerified,
      "caption" to caption.trim().take(250),
      "imageUrl" to imageUrl.ifBlank { "preset_golden_coast" }.take(500),
      "viewerIds" to listOf(uid),
      "audienceIds" to audience,
      "expiresAtMillis" to expiresAt,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      storyRef.set(payload).await()
      storyRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, storyRef.path)
      throw e
    }
  }

  suspend fun markStoryViewed(story: Story): Result<Unit> = runCatching {
    val uid = requireUserId()
    if (uid in story.viewerIds) return@runCatching
    val storyRef = db.collection("stories").document(story.id)
    val updatedViewers = (story.viewerIds + uid).distinct().take(100)
    try {
      storyRef.update(
        mapOf(
          "viewerIds" to updatedViewers,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, storyRef.path)
      throw e
    }
  }

  suspend fun followOrUnfollowUser(
    currentUser: UserProfile,
    targetUser: UserProfile,
    allPosts: List<Post>,
    allStories: List<Story>,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val targetId = targetUser.userId.ifBlank { targetUser.id }
    val isCurrentlyFollowing = targetId in currentUser.followingIds

    if (isCurrentlyFollowing) {
      // Unfollow
      val newFollowing = currentUser.followingIds - targetId
      val newFollowers = targetUser.followersIds - uid
      db.collection("users").document(uid).update(
        mapOf(
          "followingIds" to newFollowing,
          "followingCount" to newFollowing.size.toLong(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      db.collection("users").document(targetId).update(
        mapOf(
          "followersIds" to newFollowers,
          "followersCount" to newFollowers.size.toLong(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // Remove uid from target's posts audienceIds unless target is managed by uid
      allPosts.filter { it.authorId == targetId }.forEach { post ->
        val updatedAudience = (post.audienceIds - uid).ifEmpty { listOf(post.authorId) }
        db.collection("posts").document(post.id).update(
          mapOf(
            "audienceIds" to updatedAudience,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      "Unfollowed @${targetUser.username}"
    } else if (targetUser.isPrivate) {
      // Send Follow Request to Private Account
      val reqRef = db.collection("follow_requests").document("${uid}_${targetId}")
      val payload = mapOf(
        "fromUserId" to uid,
        "fromUsername" to currentUser.username.ifBlank { "amedia_user" }.take(40),
        "fromDisplayName" to currentUser.displayName.ifBlank { "AMedia User" }.take(50),
        "fromAvatar" to currentUser.avatarUrl.take(300),
        "toUserId" to targetId,
        "toUsername" to targetUser.username.take(40),
        "participantIds" to listOf(uid, targetId).distinct(),
        "status" to "pending",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      reqRef.set(payload).await()
      createNotification(
        recipientId = uid,
        senderUsername = targetUser.username,
        senderAvatar = targetUser.avatarUrl,
        type = "follow_request",
        message = "Follow request sent to private account @${targetUser.username}",
      )
      "Follow request sent to @${targetUser.username}"
    } else {
      // Direct Follow on Public Account
      val newFollowing = (currentUser.followingIds + targetId).distinct().take(100)
      val newFollowers = (targetUser.followersIds + uid).distinct().take(100)
      db.collection("users").document(uid).update(
        mapOf(
          "followingIds" to newFollowing,
          "followingCount" to newFollowing.size.toLong(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      db.collection("users").document(targetId).update(
        mapOf(
          "followersIds" to newFollowers,
          "followersCount" to newFollowers.size.toLong(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      allPosts.filter { it.authorId == targetId }.forEach { post ->
        val updatedAudience = (post.audienceIds + uid).distinct().take(100)
        db.collection("posts").document(post.id).update(
          mapOf(
            "audienceIds" to updatedAudience,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      allStories.filter { it.authorId == targetId }.forEach { story ->
        val updatedAudience = (story.audienceIds + uid).distinct().take(100)
        db.collection("stories").document(story.id).update(
          mapOf(
            "audienceIds" to updatedAudience,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }

      createNotification(
        recipientId = uid,
        senderUsername = targetUser.username,
        senderAvatar = targetUser.avatarUrl,
        type = "follow",
        message = "You started following @${targetUser.username}",
      )
      "Following @${targetUser.username}"
    }
  }

  suspend fun respondToFollowRequest(
    request: FollowRequest,
    approve: Boolean,
    currentUser: UserProfile,
    allUsers: List<UserProfile>,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val reqRef = db.collection("follow_requests").document(request.id)
    val newStatus = if (approve) "approved" else "rejected"
    try {
      reqRef.update(
        mapOf(
          "status" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      if (approve) {
        val fromUser = allUsers.find { it.userId == request.fromUserId }
        val toUser = allUsers.find { it.userId == request.toUserId } ?: currentUser
        if (fromUser != null) {
          val updatedFollowing = (fromUser.followingIds + toUser.userId).distinct().take(100)
          db.collection("users").document(fromUser.userId).update(
            mapOf(
              "followingIds" to updatedFollowing,
              "followingCount" to updatedFollowing.size.toLong(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
        val updatedFollowers = (toUser.followersIds + request.fromUserId).distinct().take(100)
        db.collection("users").document(toUser.userId).update(
          mapOf(
            "followersIds" to updatedFollowers,
            "followersCount" to updatedFollowers.size.toLong(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()

        createNotification(
          recipientId = uid,
          senderUsername = request.fromUsername,
          senderAvatar = request.fromAvatar,
          type = "follow",
          message = "Approved follow request between @${request.fromUsername} and @${request.toUsername}",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
      throw e
    }
  }

  suspend fun getOrCreateChatThread(peerUser: UserProfile): Result<String> = runCatching {
    val uid = requireUserId()
    val peerId = peerUser.userId.ifBlank { peerUser.id }
    val chatId = "chat_${uid}_${peerId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val chatRef = db.collection("chats").document(chatId)
    try {
      val snap = chatRef.get().await()
      if (!snap.exists()) {
        val payload = mapOf(
          "participantIds" to listOf(uid, peerId).distinct(),
          "peerId" to peerId,
          "peerUsername" to peerUser.username.take(40),
          "peerDisplayName" to peerUser.displayName.take(50),
          "peerAvatar" to peerUser.avatarUrl.take(300),
          "peerIsVerified" to peerUser.isVerified,
          "lastMessage" to "Say hi to @${peerUser.username} 👋",
          "lastSenderId" to uid,
          "unreadCount" to 0L,
          "isPeerTyping" to false,
          "lastSeenStatus" to "seen",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        chatRef.set(payload).await()
      }
      chatId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, chatRef.path)
      throw e
    }
  }

  suspend fun sendChatMessage(
    chat: ChatThread,
    senderProfile: UserProfile,
    text: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanText = text.trim().take(500)
    if (cleanText.isBlank()) return@runCatching
    val msgRef = db.collection("messages").document()
    val chatRef = db.collection("chats").document(chat.id)
    val participants = (chat.participantIds + uid).distinct().take(10)
    try {
      val msgPayload = mapOf(
        "chatId" to chat.id,
        "senderId" to uid,
        "senderName" to senderProfile.displayName.ifBlank { "You" }.take(50),
        "text" to cleanText,
        "seen" to true,
        "participantIds" to participants,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      msgRef.set(msgPayload).await()

      chatRef.update(
        mapOf(
          "lastMessage" to cleanText,
          "lastSenderId" to uid,
          "unreadCount" to 0L,
          "isPeerTyping" to false,
          "lastSeenStatus" to "seen",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      throw e
    }
  }

  suspend fun setTypingIndicator(chatId: String, isTyping: Boolean): Result<Unit> = runCatching {
    requireUserId()
    val chatRef = db.collection("chats").document(chatId)
    try {
      chatRef.update(
        mapOf(
          "isPeerTyping" to isTyping,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, chatRef.path)
      throw e
    }
  }

  suspend fun submitVerificationRequest(
    userProfile: UserProfile,
    category: String,
    reason: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val reqRef = db.collection("verification_requests").document("vreq_${userProfile.userId.ifBlank { uid }}")
    val payload = mapOf(
      "userId" to userProfile.userId.ifBlank { uid },
      "managedByUid" to uid,
      "username" to userProfile.username.ifBlank { "amedia_user" }.take(40),
      "displayName" to userProfile.displayName.ifBlank { "AMedia User" }.take(50),
      "avatarUrl" to userProfile.avatarUrl.take(300),
      "category" to category.trim().ifBlank { "Creator" }.take(60),
      "reason" to reason.trim().ifBlank { "Official creator account on AMedia" }.take(400),
      "status" to "pending",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      reqRef.set(payload).await()
      createNotification(
        recipientId = uid,
        senderUsername = userProfile.username,
        senderAvatar = userProfile.avatarUrl,
        type = "verification",
        message = "Blue tick verification request submitted for @${userProfile.username}",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, reqRef.path)
      throw e
    }
  }

  suspend fun submitReport(
    reporter: UserProfile,
    targetId: String,
    targetType: String,
    targetSummary: String,
    reason: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val reportRef = db.collection("reports").document()
    val payload = mapOf(
      "reporterId" to uid,
      "reporterUsername" to reporter.username.ifBlank { "amedia_user" }.take(40),
      "targetId" to targetId.take(128),
      "targetType" to if (targetType in listOf("post", "story", "user")) targetType else "post",
      "targetSummary" to targetSummary.take(300).ifBlank { "Reported content" },
      "reason" to reason.trim().take(300).ifBlank { "Community guidelines violation" },
      "status" to "pending",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      reportRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, reportRef.path)
      throw e
    }
  }

  suspend fun markNotificationsRead(notifications: List<AppNotification>): Result<Unit> = runCatching {
    requireUserId()
    notifications.filter { !it.isRead }.forEach { notif ->
      val ref = db.collection("notifications").document(notif.id)
      try {
        ref.update(
          mapOf(
            "isRead" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, ref.path)
      }
    }
  }

  suspend fun createNotification(
    recipientId: String,
    senderUsername: String,
    senderAvatar: String,
    type: String,
    message: String,
  ) {
    val uid = requireUserId()
    val notifRef = db.collection("notifications").document()
    val payload = mapOf(
      "recipientId" to recipientId.take(128),
      "senderId" to uid,
      "senderUsername" to senderUsername.ifBlank { "amedia" }.take(40),
      "senderAvatar" to senderAvatar.take(300),
      "type" to if (type in listOf("like", "comment", "follow", "follow_request", "verification", "broadcast")) type else "like",
      "message" to message.take(300).ifBlank { "New activity on AMedia" },
      "isRead" to false,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      notifRef.set(payload).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, notifRef.path)
    }
  }

  // --- ADMIN PANEL CONTROLS ---

  suspend fun adminReviewVerificationRequest(
    request: VerificationRequest,
    approve: Boolean,
    adminName: String,
    allPosts: List<Post>,
    allStories: List<Story>,
    allChats: List<ChatThread>,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val reqRef = db.collection("verification_requests").document(request.id)
    val newStatus = if (approve) "approved" else "rejected"
    try {
      reqRef.update(
        mapOf(
          "status" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      if (approve) {
        adminSetUserVerified(
          targetUserId = request.userId,
          targetUsername = request.username,
          verified = true,
          adminName = adminName,
          allPosts = allPosts,
          allStories = allStories,
          allChats = allChats,
        )
      } else {
        logAdminActivity(
          actorName = adminName,
          actionType = "verify_user",
          description = "Rejected blue tick verification request for @${request.username}",
        )
        createNotification(
          recipientId = uid,
          senderUsername = "AMedia Admin",
          senderAvatar = "avatar_neon",
          type = "verification",
          message = "Verification request for @${request.username} was declined by Admin.",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
      throw e
    }
  }

  suspend fun adminSetUserVerified(
    targetUserId: String,
    targetUsername: String,
    verified: Boolean,
    adminName: String,
    allPosts: List<Post>,
    allStories: List<Story>,
    allChats: List<ChatThread>,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(targetUserId)
    val snap = userRef.get().await()
    val currentBanned = snap.getBoolean("isBanned") ?: false
    val currentStatus = snap.getString("accountStatus") ?: "active"
    try {
      userRef.update(
        mapOf(
          "isVerified" to verified,
          "isBanned" to currentBanned,
          "accountStatus" to currentStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // Propagate Blue Verification Badge across user's posts, stories, and chat threads
      allPosts.filter { it.authorId == targetUserId }.forEach { post ->
        db.collection("posts").document(post.id).update(
          mapOf(
            "authorIsVerified" to verified,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      allStories.filter { it.authorId == targetUserId }.forEach { story ->
        db.collection("stories").document(story.id).update(
          mapOf(
            "authorIsVerified" to verified,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      allChats.filter { it.peerId == targetUserId }.forEach { chat ->
        db.collection("chats").document(chat.id).update(
          mapOf(
            "peerIsVerified" to verified,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }

      logAdminActivity(
        actorName = adminName,
        actionType = "verify_user",
        description = if (verified) {
          "Assigned Blue Verification Badge to @$targetUsername"
        } else {
          "Revoked Blue Verification Badge from @$targetUsername"
        },
      )

      createNotification(
        recipientId = uid,
        senderUsername = "AMedia Verified",
        senderAvatar = "avatar_neon",
        type = "verification",
        message = if (verified) {
          "Blue Verification Badge assigned to @$targetUsername ✓"
        } else {
          "Blue Verification Badge removed from @$targetUsername"
        },
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun adminToggleBanUser(
    targetUser: UserProfile,
    adminName: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val targetId = targetUser.userId.ifBlank { targetUser.id }
    val newBanned = !targetUser.isBanned
    val newStatus = if (newBanned) "banned" else "active"
    val userRef = db.collection("users").document(targetId)
    try {
      userRef.update(
        mapOf(
          "isVerified" to targetUser.isVerified,
          "isBanned" to newBanned,
          "accountStatus" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      logAdminActivity(
        actorName = adminName,
        actionType = "ban_user",
        description = if (newBanned) {
          "Suspended / banned user @${targetUser.username}"
        } else {
          "Restored active status for @${targetUser.username}"
        },
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun adminDeleteUser(
    targetUser: UserProfile,
    adminName: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val targetId = targetUser.userId.ifBlank { targetUser.id }
    val userRef = db.collection("users").document(targetId)
    try {
      userRef.delete().await()
      logAdminActivity(
        actorName = adminName,
        actionType = "delete_user",
        description = "Permanently deleted user account @${targetUser.username}",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, userRef.path)
      throw e
    }
  }

  suspend fun deletePost(post: Post, actorName: String, isAdminAction: Boolean = false): Result<Unit> = runCatching {
    requireUserId()
    val postRef = db.collection("posts").document(post.id)
    try {
      postRef.delete().await()
      if (isAdminAction) {
        logAdminActivity(
          actorName = actorName,
          actionType = "remove_post",
          description = "Removed post by @${post.authorUsername}: \"${post.caption.take(40)}\"",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, postRef.path)
      throw e
    }
  }

  suspend fun deleteStory(story: Story, actorName: String, isAdminAction: Boolean = false): Result<Unit> = runCatching {
    requireUserId()
    val storyRef = db.collection("stories").document(story.id)
    try {
      storyRef.delete().await()
      if (isAdminAction) {
        logAdminActivity(
          actorName = actorName,
          actionType = "remove_story",
          description = "Removed 24h story by @${story.authorUsername}: \"${story.caption.take(40)}\"",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, storyRef.path)
      throw e
    }
  }

  suspend fun adminUpdateReportStatus(
    report: ModerationReport,
    newStatus: String,
    adminName: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val reportRef = db.collection("reports").document(report.id)
    try {
      reportRef.update(
        mapOf(
          "status" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      logAdminActivity(
        actorName = adminName,
        actionType = "resolve_report",
        description = "Updated report on ${report.targetType} (${report.targetSummary.take(30)}) to '$newStatus'",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, reportRef.path)
      throw e
    }
  }

  suspend fun adminSendBroadcast(
    title: String,
    message: String,
    adminName: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val formatted = "📢 [${title.trim().ifBlank { "AMedia Announcement" }}] ${message.trim()}".take(300)
    createNotification(
      recipientId = uid,
      senderUsername = "AMedia Broadcast",
      senderAvatar = "avatar_neon",
      type = "broadcast",
      message = formatted,
    )
    logAdminActivity(
      actorName = adminName,
      actionType = "broadcast",
      description = "Broadcast sent to all users: $formatted",
    )
  }

  suspend fun logAdminActivity(
    actorName: String,
    actionType: String,
    description: String,
  ) {
    val uid = requireUserId()
    val logRef = db.collection("activity_logs").document()
    val payload = mapOf(
      "actorId" to uid,
      "actorName" to actorName.ifBlank { "Admin" }.take(50),
      "actionType" to actionType,
      "description" to description.take(400),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      logRef.set(payload).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, logRef.path)
    }
  }

  suspend fun deleteCurrentAccount(
    currentUser: UserProfile,
    posts: List<Post>,
    stories: List<Story>,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    posts.filter { it.authorId == uid }.forEach { post ->
      try {
        db.collection("posts").document(post.id).delete().await()
      } catch (_: Exception) {}
    }
    stories.filter { it.authorId == uid }.forEach { story ->
      try {
        db.collection("stories").document(story.id).delete().await()
      } catch (_: Exception) {}
    }
    val userRef = db.collection("users").document(uid)
    try {
      userRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, userRef.path)
      throw e
    }
  }

  // --- INITIAL ECOSYSTEM SEEDING (SO FEED, STORIES, CHATS & ADMIN PANEL ARE POPULATED) ---

  suspend fun seedInitialEcosystemIfNeeded(currentUser: UserProfile) {
    val uid = requireUserId()
    val seedPostId = "seed_post_1_$uid".take(100)
    val existingPosts = try {
      db.collection("posts")
        .whereArrayContains("audienceIds", uid)
        .limit(1)
        .get()
        .await()
    } catch (e: Exception) {
      return
    }
    if (!existingPosts.isEmpty) return

    val creator1Id = "creator_elena_$uid".take(90)
    val creator2Id = "creator_marcus_$uid".take(90)
    val creator3Id = "creator_sora_$uid".take(90)
    val creator4Id = "creator_liam_$uid".take(90)

    val creators = listOf(
      mapOf(
        "userId" to creator1Id,
        "managedByUid" to uid,
        "username" to "elena.visuals",
        "displayName" to "Elena Rostova",
        "dob" to "1997-03-21",
        "bio" to "Architectural & night street photographer 📸 Tokyo • Milan",
        "avatarUrl" to "avatar_sunset",
        "isPrivate" to false,
        "isVerified" to true,
        "isBanned" to false,
        "accountStatus" to "active",
        "followersIds" to listOf(uid),
        "followingIds" to listOf(uid, creator2Id),
        "savedPostIds" to emptyList<String>(),
        "postsCount" to 2L,
        "followersCount" to 1480L,
        "followingCount" to 312L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
      mapOf(
        "userId" to creator2Id,
        "managedByUid" to uid,
        "username" to "marcus.studio",
        "displayName" to "Marcus Vance",
        "dob" to "1995-11-04",
        "bio" to "Creative Director @ AMedia Labs • Minimalist 3D & Motion 🎨",
        "avatarUrl" to "avatar_neon",
        "isPrivate" to false,
        "isVerified" to true,
        "isBanned" to false,
        "accountStatus" to "active",
        "followersIds" to listOf(uid),
        "followingIds" to listOf(uid, creator1Id),
        "savedPostIds" to emptyList<String>(),
        "postsCount" to 1L,
        "followersCount" to 2940L,
        "followingCount" to 185L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
      mapOf(
        "userId" to creator3Id,
        "managedByUid" to uid,
        "username" to "sora.nomad",
        "displayName" to "Sora Takahashi",
        "dob" to "2000-06-18",
        "bio" to "Private journal • Coastal cafes, film rolls & slow mornings ☕🌊",
        "avatarUrl" to "avatar_ocean",
        "isPrivate" to true,
        "isVerified" to false,
        "isBanned" to false,
        "accountStatus" to "active",
        "followersIds" to emptyList<String>(),
        "followingIds" to listOf(uid),
        "savedPostIds" to emptyList<String>(),
        "postsCount" to 1L,
        "followersCount" to 640L,
        "followingCount" to 210L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
      mapOf(
        "userId" to creator4Id,
        "managedByUid" to uid,
        "username" to "liam.synth",
        "displayName" to "Liam O'Connor",
        "dob" to "1998-01-29",
        "bio" to "Analog modular synths & late-night sound design 🎹",
        "avatarUrl" to "avatar_emerald",
        "isPrivate" to false,
        "isVerified" to false,
        "isBanned" to false,
        "accountStatus" to "active",
        "followersIds" to listOf(uid),
        "followingIds" to listOf(uid),
        "savedPostIds" to emptyList<String>(),
        "postsCount" to 1L,
        "followersCount" to 890L,
        "followingCount" to 420L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    try {
      for (creator in creators) {
        val cId = creator["userId"] as String
        db.collection("users").document(cId).set(creator).await()
      }

      // Update current user to follow Elena, Marcus, and Liam by default so Home Feed is rich
      db.collection("users").document(uid).update(
        mapOf(
          "followingIds" to listOf(creator1Id, creator2Id, creator4Id),
          "followingCount" to 3L,
          "followersIds" to listOf(creator1Id, creator2Id, creator3Id, creator4Id),
          "followersCount" to 4L,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // Seed 3 Posts
      val posts = listOf(
        seedPostId to mapOf(
          "authorId" to creator1Id,
          "managedByUid" to uid,
          "authorUsername" to "elena.visuals",
          "authorDisplayName" to "Elena Rostova",
          "authorAvatar" to "avatar_sunset",
          "authorIsVerified" to true,
          "caption" to "Rain-slicked neon reflections in Shinjuku at blue hour. Shot handheld at 35mm f/1.4 — love how the magenta glass highlights pop! ✨🌆 #AMedia #NightPhotography",
          "imageUrl" to "preset_cyber_city",
          "likedByIds" to listOf(creator2Id),
          "likesCount" to 1L,
          "commentsCount" to 2L,
          "sharesCount" to 14L,
          "audienceIds" to listOf(uid, creator1Id),
          "commentsPreview" to listOf(
            "@marcus.studio: The color grading on this frame is unreal 🔥",
            "@liam.synth: Looks like a synthwave album cover!"
          ),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        "seed_post_2_$uid".take(100) to mapOf(
          "authorId" to creator2Id,
          "managedByUid" to uid,
          "authorUsername" to "marcus.studio",
          "authorDisplayName" to "Marcus Vance",
          "authorAvatar" to "avatar_neon",
          "authorIsVerified" to true,
          "caption" to "Golden hour espresso overlooking the Amalfi cliffs. Nothing beats natural warm travertine textures and coastal light ☀️☕ #DesignLife #CoastalVibes",
          "imageUrl" to "preset_golden_coast",
          "likedByIds" to listOf(creator1Id, creator4Id),
          "likesCount" to 2L,
          "commentsCount" to 1L,
          "sharesCount" to 9L,
          "audienceIds" to listOf(uid, creator2Id),
          "commentsPreview" to listOf(
            "@elena.visuals: Take me back to the coast immediately!"
          ),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        "seed_post_3_$uid".take(100) to mapOf(
          "authorId" to creator4Id,
          "managedByUid" to uid,
          "authorUsername" to "liam.synth",
          "authorDisplayName" to "Liam O'Connor",
          "authorAvatar" to "avatar_emerald",
          "authorIsVerified" to false,
          "caption" to "Late night studio session patching ambient textures for the upcoming visual exhibition on AMedia 🎛️🎶",
          "imageUrl" to "preset_aurora_synth",
          "likedByIds" to emptyList<String>(),
          "likesCount" to 0L,
          "commentsCount" to 1L,
          "sharesCount" to 4L,
          "audienceIds" to listOf(uid, creator4Id),
          "commentsPreview" to listOf(
            "@marcus.studio: Can't wait to hear the full mix!"
          ),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      for ((pId, pData) in posts) {
        db.collection("posts").document(pId).set(pData).await()
      }

      // Seed 24-Hour Active Stories
      val expires24h = System.currentTimeMillis() + 22L * 60L * 60L * 1000L
      val stories = listOf(
        "seed_story_1_$uid".take(100) to mapOf(
          "authorId" to creator1Id,
          "managedByUid" to uid,
          "authorUsername" to "elena.visuals",
          "authorDisplayName" to "Elena Rostova",
          "authorAvatar" to "avatar_sunset",
          "authorIsVerified" to true,
          "caption" to "Rooftop light test before tonight's shoot 📸✨",
          "imageUrl" to "preset_cyber_city",
          "viewerIds" to emptyList<String>(),
          "audienceIds" to listOf(uid, creator1Id),
          "expiresAtMillis" to expires24h,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        "seed_story_2_$uid".take(100) to mapOf(
          "authorId" to creator2Id,
          "managedByUid" to uid,
          "authorUsername" to "marcus.studio",
          "authorDisplayName" to "Marcus Vance",
          "authorAvatar" to "avatar_neon",
          "authorIsVerified" to true,
          "caption" to "Morning Mediterranean swim & sketchbook session 🌊",
          "imageUrl" to "preset_golden_coast",
          "viewerIds" to emptyList<String>(),
          "audienceIds" to listOf(uid, creator2Id),
          "expiresAtMillis" to expires24h,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        "seed_story_3_$uid".take(100) to mapOf(
          "authorId" to creator4Id,
          "managedByUid" to uid,
          "authorUsername" to "liam.synth",
          "authorDisplayName" to "Liam O'Connor",
          "authorAvatar" to "avatar_emerald",
          "authorIsVerified" to false,
          "caption" to "New modular rack wired up! Sound on 🔊",
          "imageUrl" to "preset_aurora_synth",
          "viewerIds" to emptyList<String>(),
          "audienceIds" to listOf(uid, creator4Id),
          "expiresAtMillis" to expires24h,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      for ((sId, sData) in stories) {
        db.collection("stories").document(sId).set(sData).await()
      }

      // Seed Incoming Follow Request from Sora (Private Creator)
      val followReqId = "seed_freq_$uid".take(100)
      db.collection("follow_requests").document(followReqId).set(
        mapOf(
          "fromUserId" to creator3Id,
          "fromUsername" to "sora.nomad",
          "fromDisplayName" to "Sora Takahashi",
          "fromAvatar" to "avatar_ocean",
          "toUserId" to uid,
          "toUsername" to currentUser.username.ifBlank { "you" },
          "participantIds" to listOf(uid, creator3Id),
          "status" to "pending",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // Seed Real-Time Chat Threads & Messages
      val chat1Id = "chat_${uid}_$creator1Id".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
      db.collection("chats").document(chat1Id).set(
        mapOf(
          "participantIds" to listOf(uid, creator1Id),
          "peerId" to creator1Id,
          "peerUsername" to "elena.visuals",
          "peerDisplayName" to "Elena Rostova",
          "peerAvatar" to "avatar_sunset",
          "peerIsVerified" to true,
          "lastMessage" to "Hey! Are you joining the AMedia gallery walk this evening? 📸",
          "lastSenderId" to creator1Id,
          "unreadCount" to 1L,
          "isPeerTyping" to false,
          "lastSeenStatus" to "delivered",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      db.collection("messages").document("seed_msg_1_$uid".take(100)).set(
        mapOf(
          "chatId" to chat1Id,
          "senderId" to creator1Id,
          "senderName" to "Elena Rostova",
          "text" to "Hey! Welcome to AMedia ✨ Love your profile aesthetic!",
          "seen" to true,
          "participantIds" to listOf(uid, creator1Id),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      db.collection("messages").document("seed_msg_2_$uid".take(100)).set(
        mapOf(
          "chatId" to chat1Id,
          "senderId" to creator1Id,
          "senderName" to "Elena Rostova",
          "text" to "Hey! Are you joining the AMedia gallery walk this evening? 📸",
          "seen" to false,
          "participantIds" to listOf(uid, creator1Id),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // Seed Pending Blue Tick Verification Requests for Admin Panel
      db.collection("verification_requests").document("seed_vreq_1_$uid".take(100)).set(
        mapOf(
          "userId" to creator4Id,
          "managedByUid" to uid,
          "username" to "liam.synth",
          "displayName" to "Liam O'Connor",
          "avatarUrl" to "avatar_emerald",
          "category" to "Music & Audio Artist",
          "reason" to "Verified touring electronic musician with 50K+ monthly listeners and official AMedia sound packs.",
          "status" to "pending",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      db.collection("verification_requests").document("seed_vreq_2_$uid".take(100)).set(
        mapOf(
          "userId" to creator3Id,
          "managedByUid" to uid,
          "username" to "sora.nomad",
          "displayName" to "Sora Takahashi",
          "avatarUrl" to "avatar_ocean",
          "category" to "Travel & Lifestyle Creator",
          "reason" to "Published editorial travel columnist and visual storyteller.",
          "status" to "pending",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // Seed Moderation Reports for Admin Panel
      db.collection("reports").document("seed_rep_1_$uid".take(100)).set(
        mapOf(
          "reporterId" to uid,
          "reporterUsername" to currentUser.username.ifBlank { "amedia_member" }.take(40),
          "targetId" to "seed_post_3_$uid".take(100),
          "targetType" to "post",
          "targetSummary" to "Post by @liam.synth: Late night studio session",
          "reason" to "Unlicensed audio sample inquiry — needs moderator copyright review",
          "status" to "pending",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // Seed Initial Notifications & Activity Log
      createNotification(
        recipientId = uid,
        senderUsername = "elena.visuals",
        senderAvatar = "avatar_sunset",
        type = "follow",
        message = "@elena.visuals started following you on AMedia",
      )
      createNotification(
        recipientId = uid,
        senderUsername = "sora.nomad",
        senderAvatar = "avatar_ocean",
        type = "follow_request",
        message = "@sora.nomad requested to follow you",
      )
      logAdminActivity(
        actorName = "AMedia System",
        actionType = "verify_user",
        description = "Initialized AMedia workspace with verified creators @elena.visuals and @marcus.studio",
      )
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "seed_ecosystem")
    }
  }
}
