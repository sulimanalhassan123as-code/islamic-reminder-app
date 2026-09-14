# Islamic Reminder — Azan & Daily Reminders (Android APK)

Private Android app for Never Hide Tech Empire.

## Features
- 🕌 **Azan at all 5 prayer times** — local offline prayer calculation (PrayTimes algorithm), full-screen alert over the lock screen + azan audio out loud
- 📖 **4 daily reminders** — morning dhikr, midday Qur'an, afternoon hadith, evening dhikr
- 🌟 **Separate Friday reminder** — Jumu'ah special message (Surah Al-Kahf, ghusl, early to mosque)
- 🔒 **Stubborn-proof notifications** — exact alarms + full-screen intent + battery-optimization exemption + boot reschedule
- 📢 **Admin broadcasts** — web panel sends notifications to every installed device

## Admin panel
Deployed at: https://islamic-reminder-admin.vercel.app (`admin/` folder, Vercel project `islamic-reminder-admin`)

## Build
GitHub Actions builds the signed APK on every push. Download from Actions artifacts (`IslamicReminder-APK`).

## Backend
Supabase tables `islamic_app_users` + `islamic_broadcasts` (RLS: anon can register devices and read broadcasts; only service_role can send broadcasts).

See `supabase/schema.sql`.
