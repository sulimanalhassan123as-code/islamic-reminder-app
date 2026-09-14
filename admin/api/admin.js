const SUPABASE_URL = process.env.SUPABASE_URL;
const SERVICE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD;

export default async function handler(req, res) {
  res.setHeader('Content-Type', 'application/json');

  if (req.method !== 'POST') {
    return res.status(405).json({ ok: false, error: 'POST only' });
  }

  const body = req.body || {};
  const action = body.action || 'auth';

  // --- Auth check ---
  if (body.password !== ADMIN_PASSWORD) {
    return res.status(401).json({ ok: false, error: 'Invalid admin password' });
  }

  try {
    if (action === 'broadcast') {
      // --- Send a notification to every device ---
      if (!body.title || !body.message) {
        return res.status(400).json({ ok: false, error: 'title and message required' });
      }
      const r = await fetch(`${SUPABASE_URL}/rest/v1/islamic_broadcasts`, {
        method: 'POST',
        headers: {
          'apikey': SERVICE_KEY,
          'Authorization': `Bearer ${SERVICE_KEY}`,
          'Content-Type': 'application/json',
          'Prefer': 'return=representation'
        },
        body: JSON.stringify({ title: String(body.title).slice(0, 200), body: String(body.message).slice(0, 4000) })
      });
      if (!r.ok) throw new Error(`Supabase insert failed: ${r.status}`);
      return res.status(200).json({ ok: true, sent: true });
    }

    // --- Default: stats ---
    const usersR = await fetch(
      `${SUPABASE_URL}/rest/v1/islamic_app_users?select=id`,
      { headers: { 'apikey': SERVICE_KEY, 'Authorization': `Bearer ${SERVICE_KEY}`, 'Prefer': 'count=exact' } }
    );
    if (!usersR.ok) throw new Error(`Supabase users query failed: ${usersR.status}`);
    const total = parseInt(usersR.headers.get('content-range')?.split('/')[1] || '0', 10);

    const activeR = await fetch(
      `${SUPABASE_URL}/rest/v1/islamic_app_users?select=id&last_seen=gte.${new Date(Date.now() - 7 * 86400000).toISOString()}`,
      { headers: { 'apikey': SERVICE_KEY, 'Authorization': `Bearer ${SERVICE_KEY}`, 'Prefer': 'count=exact' } }
    );
    const active = parseInt(activeR.headers.get('content-range')?.split('/')[1] || '0', 10);

    const bR = await fetch(
      `${SUPABASE_URL}/rest/v1/islamic_broadcasts?order=created_at.desc&limit=10`,
      { headers: { 'apikey': SERVICE_KEY, 'Authorization': `Bearer ${SERVICE_KEY}` } }
    );
    const broadcasts = bR.ok ? await bR.json() : [];

    return res.status(200).json({
      ok: true,
      total_users: total,
      active_users_7d: active,
      broadcasts: broadcasts.map(b => ({
        id: b.id, title: b.title, body: b.body,
        when: new Date(b.created_at).toLocaleString('en-GB', { timeZone: 'Africa/Accra' })
      }))
    });
  } catch (e) {
    return res.status(500).json({ ok: false, error: String(e.message || e) });
  }
}
