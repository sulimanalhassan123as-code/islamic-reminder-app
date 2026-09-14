package com.neverhide.islamicreminder

import java.util.Calendar

object Content {

    // ── 4 daily reminder slots: morning dhikr, midday Qur'an, afternoon hadith, evening dhikr ──

    val MORNING_DHIKR = arrayOf(
        "Say: SubhanAllahi wa bihamdihi 100 times, and your sins fall away like leaves. (Muslim)",
        "O Allah, by Your leave we have reached the morning... keep us safe this day. Morning du'a",
        "Recite Ayat al-Kursi after Fajr — you are under Allah's protection until evening.",
        "The morning adhkar: a shield for your day. Take 5 minutes before the phone takes you.",
        "Whoever says 'As-salamu alaykum' — peace spreads. Be the first to greet today.",
        "Duha prayer (mid-morning) — 2 short rak'ahs carry immense reward. Don't skip it."
    )

    val QURAN_REMINDERS = arrayOf(
        "So remember Me; I will remember you. — Qur'an 2:152",
        "Indeed, with hardship comes ease. — Qur'an 94:6",
        "And your Lord says: Call upon Me, I will answer you. — Qur'an 40:60",
        "Verily in the remembrance of Allah do hearts find rest. — Qur'an 13:28",
        "Allah does not burden a soul beyond what it can bear. — Qur'an 2:286",
        "And whoever fears Allah — He will make a way out for him. — Qur'an 65:2",
        "Do not despair of the mercy of Allah. He forgives all sins. — Qur'an 39:53",
        "The believers are only those whose hearts tremble when Allah is mentioned. — Qur'an 8:2",
        "And He is with you wherever you are. — Qur'an 57:4",
        "Whoever relies upon Allah — He is sufficient for him. — Qur'an 65:3",
        "And be grateful to Me and to My parents. — Qur'an 31:14",
        "Indeed, prayer prohibits immorality and wrongdoing. — Qur'an 29:45",
        "Race toward forgiveness from your Lord and a Garden as wide as the heavens. — Qur'an 3:133",
        "Every soul shall taste death. So do the best today. — Qur'an 3:185",
        "Hold firmly to the rope of Allah, all together. — Qur'an 3:103"
    )

    val HADITHS = arrayOf(
        "Actions are judged by intentions. — Bukhari & Muslim",
        "Allah is gentle and loves gentleness in all matters. — Muslim",
        "The best among you are those who learn the Qur'an and teach it. — Bukhari",
        "Whoever believes in Allah and the Last Day should speak good or remain silent. — Bukhari",
        "A smile in the face of your brother is charity. — Tirmidhi",
        "The strong believer is better than the weak believer, but in both is good. — Muslim",
        "None of you truly believes until he loves for his brother what he loves for himself. — Bukhari",
        "Cleanliness is half of faith. — Muslim",
        "The best of people are those most beneficial to people. — Tabarani",
        "Whoever prays Fajr is under Allah's protection. — Muslim",
        "Paradise lies at the feet of mothers. — Ahmad",
        "Seeking knowledge is a duty upon every Muslim. — Ibn Majah",
        "Be in this world as a traveler. — Bukhari",
        "Allah loves when one of you does a job, he does it with excellence. — Bayhaqi"
    )

    val EVENING_DHIKR = arrayOf(
        "Recite Ayat al-Kursi before sleep — an angel guards you all night. (Bukhari)",
        "Say 'Bismillah' 3x, then the 3 Quls 3x, blow into your palms, wipe your body — the Sunnah of sleeping.",
        "Sleep with wudu if you can — the soul rises in purity.",
        "Say: SubhanAllah 33x, Alhamdulillah 33x, Allahu Akbar 34x — better than a servant. (Muslim)",
        "The last thing tonight: forgive whoever wronged you. Free your heart before it sleeps.",
        "Tahajjud is the honor of the believer — wake before Fajr even for 2 rak'ahs sometimes."
    )

    const val FRIDAY_TITLE = "🕌 Jumu'ah Mubarak — Special Friday Reminder"

    fun fridayMessage(): String =
        "It's Friday — the best day the sun rises upon. (Ahmad)\n\n" +
        "• Take ghusl, wear your best clothes, use perfume\n" +
        "• Read Surah Al-Kahf — light from one Friday to the next\n" +
        "• Go early to the mosque — every step is reward\n" +
        "• Make abundant du'a before Maghrib — the hour of answered prayers\n\n" +
        "May Allah accept your Jumu'ah. Ameen."

    /** Returns (title, body) for slot 0..3 */
    fun forDailySlot(ctx: android.content.Context, slot: Int): Pair<String, String> {
        val doy = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return when (slot) {
            0 -> "🌅 Morning Reminder" to MORNING_DHIKR[doy % MORNING_DHIKR.size]
            1 -> "📖 Qur'an Reminder" to QURAN_REMINDERS[doy % QURAN_REMINDERS.size]
            2 -> "🌙 Hadith Reminder" to HADITHS[doy % HADITHS.size]
            else -> "🌙 Evening Reminder" to EVENING_DHIKR[doy % EVENING_DHIKR.size]
        }
    }
}
