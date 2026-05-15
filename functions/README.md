# Cloud Functions (single-device sessions)

The Firestore trigger **`revokeRefreshTokensOnSessionChange`** runs when a document at **`users/{uid}`** is updated and the **`sessionToken`** field changes to a new non-empty string. It calls **`admin.auth().revokeRefreshTokens(uid)`**, which invalidates **all** Firebase refresh tokens for that user. Combined with your Android Firestore session listener, other devices stop being able to refresh their credentials.

## Prerequisites

- Firebase **Blaze** plan (Cloud Functions requirement).
- Firebase CLI: `npm install -g firebase-tools`
- Node **18**+ locally for build/deploy.

## Setup

From the **repository root** (where `firebase.json` lives):

```bash
cd functions && npm install && npm run build
```

Link the Firebase project (once):

```bash
firebase use --add
```

## Deploy

```bash
firebase deploy --only functions
```

## Behaviour note

`revokeRefreshTokens` affects **every** refresh token for that UID, **including** the device that just wrote the new `sessionToken`. Short-lived ID tokens may still work until they expire; if you see odd behaviour on the signing-in device after login, you may need to adjust client timing (for example ensure sign-in fully completes before depending on refresh) or consult Firebase docs on token lifecycle.

## Local build only

```bash
cd functions && npm run build
```
