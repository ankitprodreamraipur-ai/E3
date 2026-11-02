# AMedia Security Specification (Payload-First TDD)

## 1. Data Invariants
1. **Authentication Gate**: Unauthenticated callers (`request.auth == null`) cannot read or write any document across any collection.
2. **PII Isolation**: User profile documents (`users/{userId}`) contain zero PII (no email, phone, or physical address).
3. **Identity & Ownership Integrity**: Every created document must bind its creator/owner field (`userId`, `managedByUid`, `fromUserId`, `senderId`, `reporterId`, `actorId`) to `request.auth.uid`.
4. **Temporal Integrity**: All `createdAt` and `updatedAt` fields must be valid Firestore `timestamp` objects with `<= request.time`. On updates, `createdAt` and primary ownership fields are strictly immutable.
5. **Query Enforcer (`allow list`)**: No collection allows blanket `isSignedIn()` list queries. Every `allow list` rule evaluates `resource.data` against `request.auth.uid` (e.g., `request.auth.uid in resource.data.audienceIds`, `resource.data.recipientId == request.auth.uid`, `resource.data.accountStatus == 'active'`, or `isAdmin()`).
6. **Privilege Escalation Prevention**: Regular users cannot self-assign `isVerified: true` on user creation or profile edits unless via the authorized Admin / workspace manager action.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Scrape**: `GET /users/alice` with `auth = null` -> REJECT.
2. **Blanket List Scrape**: `LIST /posts` without `audienceIds` or `authorId` filter by non-admin -> REJECT.
3. **Identity Spoofing on Post Creation**: `CREATE /posts/post_1` where `managedByUid != request.auth.uid` -> REJECT.
4. **Self-Verification Shadow Update**: User updates own profile with `isVerified: true` and `bio: "hacked"` in the same payload without admin authorization -> REJECT.
5. **Ghost Field Injection**: `CREATE /users/alice` with undeclared key `isAdminRole: true` -> REJECT via `hasOnly()`.
6. **Future Timestamp Injection**: `CREATE /posts/post_1` with `createdAt = request.time + 10h` -> REJECT via `<= request.time`.
7. **Immutable Timestamp Tampering**: `UPDATE /posts/post_1` mutating `createdAt` -> REJECT.
8. **Cross-User Notification Snooping**: `GET /notifications/notif_bob` by Alice where `recipientId == 'bob'` and `senderId == 'charlie'` -> REJECT.
9. **Unauthorized Chat Eavesdropping**: `LIST /messages` by Eve where `Eve` is not in `participantIds` -> REJECT.
10. **String Overflow (Denial of Wallet)**: `CREATE /posts/post_1` with a 10,000-character `caption` -> REJECT via `.size() <= 500`.
11. **Admin Email Spoofing (Unverified Email)**: Token has `email == 'ankitprodream.raipur@gmail.com'` but `email_verified == false` -> REJECT `isAdmin()`.
12. **Path Variable Poisoning**: Document ID exceeding 128 chars or containing invalid characters -> REJECT via `isValidId()`.
