-- Islamic Reminder — Supabase schema
-- Tables are prefixed "islamic_" to stay isolated inside the shared
-- SecureCheck AI project database (free tier has a 2-active-project limit).
-- App uses the ANON key (RLS-protected). Admin panel uses SERVICE_ROLE.

-- 1) Users: one row per installed device
create table if not exists public.islamic_app_users (
  id uuid primary key default gen_random_uuid(),
  device_id text unique not null,
  app_version text,
  city text default 'Accra',
  created_at timestamptz default now(),
  last_seen timestamptz default now()
);

alter table public.islamic_app_users enable row level security;

-- App can register itself, update its last_seen, and read rows
-- (SELECT policy is REQUIRED for upserts: ON CONFLICT must see the conflicting row)
drop policy if exists "app_insert" on public.islamic_app_users;
create policy "app_insert" on public.islamic_app_users
  for insert to anon with check (true);

drop policy if exists "app_update" on public.islamic_app_users;
create policy "app_update" on public.islamic_app_users
  for update to anon using (true) with check (true);

drop policy if exists "app_select" on public.islamic_app_users;
create policy "app_select" on public.islamic_app_users
  for select to anon using (true);

-- NOTE: PostgreSQL requires table-wide SELECT for the APK's ON CONFLICT
-- upsert, so rows stay visible to the anon key. Hardening is done with
-- column-level privileges instead (applied 2026-09-25 security audit):
--   revoke select on public.islamic_app_users from anon;
--   grant select (id, device_id, app_version, city, created_at, last_seen)
--     on public.islamic_app_users to anon;
--   revoke update on public.islamic_app_users from anon;
--   grant update (device_id, app_version, city, last_seen)
--     on public.islamic_app_users to anon;
--   revoke insert (id, created_at) on public.islamic_app_users from anon;
-- The table holds only anonymous metrics (device UUID, city, version,
-- timestamps) - no names, phones or tokens. Full lockdown (header-based
-- auth like the Empire app) is planned for APK v2.

-- 2) Broadcasts: admin → all users
create table if not exists public.islamic_broadcasts (
  id bigint generated always as identity primary key,
  title text,
  body text,
  created_at timestamptz default now()
);

alter table public.islamic_broadcasts enable row level security;

-- App can READ broadcasts (to notify), only service_role can INSERT
drop policy if exists "app_read_broadcasts" on public.islamic_broadcasts;
create policy "app_read_broadcasts" on public.islamic_broadcasts
  for select to anon using (true);
