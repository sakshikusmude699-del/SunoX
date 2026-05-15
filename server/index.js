import express from "express";
import fetch from "node-fetch";
import jwt from "jsonwebtoken";
import crypto from "crypto";

const app = express();
app.use(express.json());

const PORT = process.env.PORT || 8080;
const MSG91_AUTH_KEY = process.env.MSG91_AUTH_KEY;
const MSG91_OTP_TEMPLATE_ID = process.env.MSG91_OTP_TEMPLATE_ID || "";
const JWT_SECRET = process.env.JWT_SECRET || "dev-secret";

if (!MSG91_AUTH_KEY) {
  console.warn("Missing MSG91_AUTH_KEY in environment.");
}

// In-memory sessions for demo. Replace with DB/Redis in production.
const sessions = new Map(); // sessionId -> { phone, createdAt, attempts }

function uuid() {
  return crypto.randomUUID();
}

// Basic rate limit (demo). Replace with real limiter (Redis) for production.
const lastSendByPhone = new Map(); // phone -> ms

app.post("/auth/otp/start", async (req, res) => {
  try {
    const phone = String(req.body?.phone || "").trim();
    if (!phone) return res.status(400).json({ error: "phone_required" });

    const now = Date.now();
    const last = lastSendByPhone.get(phone) || 0;
    if (now - last < 60_000) return res.status(429).json({ error: "cooldown" });
    lastSendByPhone.set(phone, now);

    const sessionId = uuid();
    sessions.set(sessionId, { phone, createdAt: now, attempts: 0 });

    // MSG91 OTP send (you must confirm the correct endpoint/settings for your MSG91 account).
    // Many MSG91 setups use an endpoint like:
    //   https://control.msg91.com/api/v5/otp?template_id=...&mobile=...&authkey=...
    // Some require country code separately, others expect E.164 without '+'.
    //
    // For now we call a placeholder "v5/otp" style endpoint and pass mobile=phone.
    const url = new URL("https://control.msg91.com/api/v5/otp");
    if (MSG91_OTP_TEMPLATE_ID) url.searchParams.set("template_id", MSG91_OTP_TEMPLATE_ID);
    url.searchParams.set("mobile", phone);
    url.searchParams.set("otp_length", "6");

    const r = await fetch(url.toString(), {
      method: "POST",
      headers: {
        authkey: MSG91_AUTH_KEY,
        "Content-Type": "application/json"
      }
    });

    if (!r.ok) {
      const text = await r.text();
      return res.status(502).json({ error: "msg91_send_failed", detail: text });
    }

    return res.json({ sessionId, expiresInSec: 300 });
  } catch (e) {
    return res.status(500).json({ error: "server_error", detail: String(e?.message || e) });
  }
});

app.post("/auth/otp/verify", async (req, res) => {
  try {
    const sessionId = String(req.body?.sessionId || "").trim();
    const otp = String(req.body?.otp || "").trim();
    if (!sessionId || !otp) return res.status(400).json({ error: "missing_fields" });

    const session = sessions.get(sessionId);
    if (!session) return res.status(400).json({ error: "invalid_session" });
    if (Date.now() - session.createdAt > 5 * 60_000) return res.status(400).json({ error: "expired" });
    if (session.attempts >= 5) return res.status(429).json({ error: "too_many_attempts" });

    session.attempts += 1;

    // MSG91 OTP verify (placeholder; confirm correct endpoint for your account).
    const url = new URL("https://control.msg91.com/api/v5/otp/verify");
    url.searchParams.set("mobile", session.phone);
    url.searchParams.set("otp", otp);

    const r = await fetch(url.toString(), {
      method: "POST",
      headers: {
        authkey: MSG91_AUTH_KEY,
        "Content-Type": "application/json"
      }
    });

    if (!r.ok) {
      const text = await r.text();
      return res.status(401).json({ error: "invalid_otp", detail: text });
    }

    // Mark session used
    sessions.delete(sessionId);

    const accessToken = jwt.sign({ phone: session.phone }, JWT_SECRET, { expiresIn: "30d" });
    return res.json({ accessToken, user: { phone: session.phone } });
  } catch (e) {
    return res.status(500).json({ error: "server_error", detail: String(e?.message || e) });
  }
});

app.listen(PORT, () => {
  console.log(`OTP backend listening on http://localhost:${PORT}`);
});

