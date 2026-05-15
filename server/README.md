## SunoX OTP Backend (MSG91)

This backend is required for SMS OTP login. **Do not** put MSG91 keys in the Android app.

### Prereqs
- Node.js 18+

### Setup

```bash
cd server
npm install
cp .env.example .env
```

Fill `.env`:
- `MSG91_AUTH_KEY=...` (your MSG91 auth key)
- `MSG91_OTP_TEMPLATE_ID=...` (if required)
- `JWT_SECRET=...` (any random secret)

### Run

```bash
npm run dev
```

Server starts on `http://localhost:8080`.

### Android base URL
- Emulator: `http://10.0.2.2:8080`
- Physical device: `http://<your-lan-ip>:8080`

