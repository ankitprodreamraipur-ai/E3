const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const EVE_UID = "eve_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated user cannot read or list any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("posts").get());
  await assertFails(unauthDb.collection("notifications").get());
});

test("2. Authenticated user can create own valid UserProfile, but cannot self-verify", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Valid creation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      managedByUid: ALICE_UID,
      username: "alice_media",
      displayName: "Alice Vance",
      dob: "1998-04-12",
      bio: "Digital visual storyteller",
      avatarUrl: "preset_1",
      isPrivate: false,
      isVerified: false,
      isBanned: false,
      accountStatus: "active",
      followersIds: [],
      followingIds: [],
      savedPostIds: [],
      postsCount: 0,
      followersCount: 0,
      followingCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Attempt self-verification on creation -> FAILS
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      managedByUid: BOB_UID,
      username: "bob_hacker",
      displayName: "Bob",
      dob: "1995-01-01",
      bio: "Hacker",
      avatarUrl: "preset_2",
      isPrivate: false,
      isVerified: true,
      isBanned: false,
      accountStatus: "active",
      followersIds: [],
      followingIds: [],
      savedPostIds: [],
      postsCount: 0,
      followersCount: 0,
      followingCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("3. Posts feed query alignment: only audience members can list posts", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      managedByUid: ALICE_UID,
      authorUsername: "alice_media",
      authorDisplayName: "Alice Vance",
      authorAvatar: "preset_1",
      authorIsVerified: true,
      caption: "Sunset vibes on AMedia",
      imageUrl: "preset_sunset",
      likedByIds: [],
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      audienceIds: [ALICE_UID, BOB_UID],
      commentsPreview: [],
      createdAt: now,
      updatedAt: now,
    });
  });

  // Bob is in audienceIds -> succeeds with aligned query
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("posts").where("audienceIds", "array-contains", BOB_UID).get()
  );

  // Blanket query without filter -> fails
  await assertFails(bobDb.collection("posts").get());

  // Eve is NOT in audienceIds -> fails to read post_1
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  await assertFails(eveDb.collection("posts").doc("post_1").get());
});

test("4. Cross-user notification isolation", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("notifications").doc("notif_1").set({
      recipientId: ALICE_UID,
      senderId: BOB_UID,
      senderUsername: "bob_photo",
      senderAvatar: "preset_2",
      type: "like",
      message: "liked your post",
      isRead: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("notifications").where("recipientId", "==", ALICE_UID).get()
  );

  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  await assertFails(eveDb.collection("notifications").doc("notif_1").get());
});

test("5. Email spoofing with unverified admin email is rejected", async () => {
  const spoofAdminDb = testEnv
    .authenticatedContext(EVE_UID, {
      email: "ankitprodream.raipur@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(spoofAdminDb.collection("admins").get());
});
