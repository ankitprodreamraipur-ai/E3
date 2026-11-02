package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class AMediaRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createProfileAndPost_authenticatedOwner_succeedsAndEmitsInFeed() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val aliceUid = signInTestUser("alice_$suffix@test.com")
    val repo = AMediaRepository(firestore, auth)

    val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createOrEnsureUserProfile(
        username = "alice_$suffix",
        displayName = "Alice Wonderland",
      )
    }
    assertTrue("Profile creation should succeed: ${profileResult.exceptionOrNull()}", profileResult.isSuccess)
    val aliceProfile = profileResult.getOrThrow()
    assertEquals(aliceUid, aliceProfile.userId)

    val postResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createPost(
        authorProfile = aliceProfile,
        caption = "Testing AMedia feed post!",
        imageUrl = "preset_cyber_city",
      )
    }
    assertTrue("Post creation should succeed: ${postResult.exceptionOrNull()}", postResult.isSuccess)
    val postId = postResult.getOrThrow()

    val emittedPosts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeFeedPosts(aliceUid).first { list -> list.any { it.id == postId } }
    }
    assertTrue(emittedPosts.any { it.id == postId })
  }

  @Test
  fun observeNotifications_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val aliceUid = signInTestUser("alice_notif_$suffix@test.com")
    val aliceRepo = AMediaRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createNotification(
        recipientId = aliceUid,
        senderUsername = "alice",
        senderAvatar = "avatar_aurora",
        type = "like",
        message = "Private notification",
      )
    }

    // Sign in as Bob and try to read Alice's notifications stream
    signInTestUser("bob_notif_$suffix@test.com")
    val bobRepo = AMediaRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeNotifications(aliceUid).first()
      }
      fail("Expected PERMISSION_DENIED when Bob observes Alice's notifications")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected wrapped FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  @Test
  fun observeFeedPosts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = AMediaRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeFeedPosts("any_uid").first()
      }
      fail("Expected PERMISSION_DENIED for unauthenticated caller")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected wrapped FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3500L
  }
}
