import * as admin from "firebase-admin";
import { logger } from "firebase-functions";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";

admin.initializeApp();

const USERS = "users";
const SESSION_TOKEN = "sessionToken";

/**
 * When `users/{uid}.sessionToken` changes (new login / session takeover), revoke all
 * Firebase refresh tokens for that uid. Other devices can no longer refresh; your app
 * should still sign them out via the existing Firestore session listener.
 *
 * Note: revokeRefreshTokens invalidates every refresh token for this user, including
 * the device that just wrote the new sessionToken. ID tokens remain valid until expiry;
 * the client may need a follow-up sign-in if long-lived sessions rely on refresh.
 */
export const revokeRefreshTokensOnSessionChange = onDocumentUpdated(
  `${USERS}/{userId}`,
  async (event) => {
    const userId = event.params.userId as string;
    const beforeSnap = event.data?.before;
    const afterSnap = event.data?.after;
    if (!beforeSnap?.exists || !afterSnap?.exists) {
      return;
    }
    const beforeTok = beforeSnap.get(SESSION_TOKEN);
    const afterTok = afterSnap.get(SESSION_TOKEN);
    if (
      typeof afterTok !== "string" ||
      afterTok.length === 0 ||
      beforeTok === afterTok
    ) {
      return;
    }
    try {
      await admin.auth().revokeRefreshTokens(userId);
      logger.info("revokeRefreshTokens ok", { userId });
    } catch (e) {
      logger.error("revokeRefreshTokens failed", { userId, err: String(e) });
    }
  },
);
