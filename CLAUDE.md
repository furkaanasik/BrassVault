# BrassVault — Ekip Şifre Kasası

Şirket içi, self-hosted ekip credential kasası. Admin ekipler oluşturur,
kullanıcı ekler, ortak credential girer; ekip üyeleri listeler ve şifreyi
kontrollü görüntüler. Her şifre görüntüleme audit kaydına yazılır.

## Mimari

```
web (nginx, React 18 + TS + Vite)  →  /api proxy  →  api (Spring Boot 3, Java 21)  →  PostgreSQL 16
```

- Docker Compose ile ayağa kalkar: `docker compose up --build` → arayüz http://localhost:8081
- Frontend dev server (`pnpm dev`) `/api`'yi `:8080`'e proxy'ler.

## Dizin yapısı

```
api/                          Spring Boot 3 backend (Maven, Java 21)
  src/main/java/com/brassvault/api/
    crypto/                   AES-256-GCM kasa şifrelemesi (VaultCryptoService, MasterKeyProvider)
    domain/                   JPA entity'ler: User, Team, TeamMember, Item, AuditLog + Role, AuditAction
    repo/                     Spring Data repository'ler — yetkilendirme sorgu İÇİNDE (JOIN team_members)
    security/                 JwtService, JwtAuthFilter, MustChangePasswordFilter, SecurityConfig
    service/                  AuthService, ItemService, UserAdminService, TeamAdminService, AuditService...
    web/                      Controller'lar + dto/ + admin/ (AdminUser/Team/Item/AuditLog controller)
    config/AdminBootstrap.java  Boş DB'de ilk admin'i oluşturur
  src/main/resources/db/migration/V1__init.sql   Flyway şeması
  src/test/                   Testcontainers ile gerçek Postgres üzerinde IT'ler

web/                          React 18 + TypeScript + Vite frontend (pnpm)
  src/pages/                  Login, ChangePassword, Teams, TeamVault, PersonalVault, AdminUsers, AdminTeams, AuditLogs
  src/components/SecretCell.tsx    Şifre göster/gizle hücresi
  src/hooks/useSecretReveal.ts     30 sn sonra otomatik gizleme
  src/api.ts                  Fetch wrapper; src/auth.tsx auth context
  src/index.css               Global stiller (UI iyileştirme burada başlar)
```

## Güvenlik kararları (değiştirme, nedenini bil)

- **Kasa şifreleri**: AES-256-GCM. Master key `VAULT_MASTER_KEY` env'den
  (base64, 32 byte); yoksa/hatalıysa uygulama BAŞLAMAZ. Blob formatı
  `IV(12) || ciphertext || tag(16)`; her şifrelemede SecureRandom ile yeni IV;
  item id AAD olarak bağlı — blob başka item id'siyle çözülemez.
- **Login şifreleri**: BCrypt strength 12.
- **Yetkilendirme**: Repository sorgularında (`JOIN team_members`). Üyesi
  olunmayan ekibin item'ı istek sahibi için yoktur → **404 döner, 403 değil**
  (varlık sızdırılmaz). Bu davranışı koruyan testler var.
- **JWT**: 8 saatlik access token; httpOnly + Secure + SameSite=Strict cookie.
  Refresh token yok. Localhost güvenli bağlam sayıldığı için lokalde çalışır;
  prod'da TLS şart.
- **must_change_password** setliyken şifre değiştirme + auth dışı her istek 403.
- **Audit**: LOGIN, VIEW_SECRET, CREATE/UPDATE/DELETE_ITEM, ADD/REMOVE_MEMBER —
  snapshot alanlarla (kullanıcı/item silinse de kayıt kalır).

## API özeti

| Method | Path | Açıklama |
|---|---|---|
| POST | `/api/auth/login` | email+password → JWT cookie |
| POST | `/api/auth/logout` | cookie temizle |
| GET | `/api/me` | profil + ekipler |
| POST | `/api/me/password` | kendi şifresini değiştir |
| GET | `/api/teams` | üyesi olunan ekipler |
| GET | `/api/teams/{id}/items` | item listesi (şifre HARİÇ) |
| GET | `/api/items/{id}/secret` | şifreyi çözer (ekip üyesi VEYA owner), VIEW_SECRET audit'i yazar |
| GET/POST | `/api/vault/items` | kişisel kasa: listele / oluştur (herkes, kendi kayıtları) |
| PUT/DELETE | `/api/vault/items/{id}` | kişisel kayıt güncelle / sil (sadece owner) |
| POST/GET | `/api/admin/users` | kullanıcı oluştur (geçici şifre) / listele |
| PATCH | `/api/admin/users/{id}` | rol değiştir / deaktive et |
| POST/GET | `/api/admin/teams` | ekip oluştur / listele |
| GET/POST | `/api/admin/teams/{id}/members` | üye listele / ekle |
| DELETE | `/api/admin/teams/{id}/members/{uid}` | üye çıkar |
| POST | `/api/admin/items` | item oluştur |
| PUT/DELETE | `/api/admin/items/{id}` | item güncelle / sil |
| GET | `/api/admin/audit-logs` | filtreli + sayfalı audit |

## Komutlar

```bash
# Tüm stack
docker compose up --build            # .env gerekli: VAULT_MASTER_KEY, JWT_SECRET, ADMIN_INITIAL_PASSWORD

# Backend test (Docker şart — Testcontainers gerçek Postgres kullanır)
cd api && ./mvnw test

# Backend lokal çalıştırma
cd api && VAULT_MASTER_KEY=$(openssl rand -base64 32) \
  DB_URL=jdbc:postgresql://localhost:5432/brassvault \
  ADMIN_INITIAL_PASSWORD=dev-admin-pass ./mvnw spring-boot:run

# Frontend
cd web && pnpm install && pnpm dev   # dev
cd web && pnpm test                  # vitest
cd web && pnpm lint                  # oxlint
cd web && pnpm build                 # tsc -b && vite build
```

## Konvansiyonlar

- Frontend paket yöneticisi **pnpm** (npm değil).
- Frontend'de UI kütüphanesi yok — saf React + `index.css` tasarım sistemi
  ("Brass Vault": OKLCH koyu yeşil-füme + pirinç accent; Sora / Instrument
  Sans / JetBrains Mono, @fontsource-variable ile self-hosted). İkonlar
  `src/components/Icons.tsx` içinde el yazımı inline SVG. Tek istisna:
  **three.js** — sadece login'deki 3D kasa çarkı için, lazy-load ayrı
  chunk'ta (kullanıcı onayıyla eklendi). Başka bağımlılık eklemeden önce sor.
- Backend testleri davranış odaklı IT'ler; güvenlik davranışlarını
  (404-vs-403, admin 403, deaktive kullanıcı, master key zorunluluğu)
  bozan değişiklikte önce testi anla.
- Değişiklik sonrası: `./mvnw test` (api) ve `pnpm test && pnpm lint` (web)
  yeşil olmalı.

## Bilinen durum

- Fonksiyonellik tamam ve çalışıyor.
- Kişisel kasa (2026-09-01): her kullanıcı `/vault`'ta kendi credential'larını
  CRUD eder (`/api/vault/items`, `items.owner_id` — V1'den beri şemada, V2 sadece
  index ekledi). **Tamamen özel**: admin dahil başkası kişisel kayda erişemez —
  admin item endpoint'lerinde service-level guard (`ownerId != null` → 404),
  diğer her yol repository sorgusunda owner filtresiyle 404. Secret endpoint'i
  tek (`findByIdForUser` owner-OR-üyelik, EXISTS ile). Audit `teamName=null` ile
  kişisel kaydı işaretler (başlık admin'e görünür — bilinçli karar);
  AuditLogs UI'da "Kişisel" göstergesi. Testler: `PersonalVaultFlowIT`.
- UI 2026-08-31'de baştan tasarlandı: sidebar'lı shell, split-hero login,
  tablo/badge/empty-state bileşenleri. Motion CSS-only + bir React hook:
  - Timing: `--ease: cubic-bezier(0.2,0,0,1)`, `--t-fast/base/slow` =
    150/250/550ms. Girişler fade + rise + hafif scale; kullanıcı belirgin
    animasyon istedi, o yüzden mesafeler büyük (18-32px).
  - İmza parçalar: login'de **gerçek 3D kasa çarkı** (three.js, WebGL —
    `VaultDial3D.tsx`, lazy-load ayrı chunk; pirinç metal halkalar + jant,
    mouse'a ~30° tilt). Canvas TÜM sahneyi kaplar (`.dial { inset: 0 }`) —
    çark, grid ayrım çizgisine (1.1fr/1fr) SAHNE İÇİNDE konumlanır ve
    resize'da ~780px'e ölçeklenir; canvas'ı kutuya küçültme, halkalar
    kenarda kırpılır (yaşandı). Glow katmanları `z-index: -1` (çarkın
    arkası), içerik `z-index: 1`. + mouse'u
    izleyen glow (`useMouseTilt` hook `--mx/--my` CSS var yazar; form
    karşı-tilt + hero parallax da bundan beslenir); secret reveal'de
    **scramble/decrypt efekti**
    (`SecretCell.tsx` içindeki `useScramble`, rAF, 700ms) + 30 sn linear
    countdown bar; sidebar nav cascade + aktif link göstergesi; tablo
    satırlarında `--i` ile micro-cascade (25ms, 300ms cap); form açılışı
    `grid-template-rows` keyframe; kopyalamada pop.
  - İç ekranlar (2026-09-01 premium pas): shell'de login'le aynı ambiyans
    ışıkları + sağda **eş merkezli kadran filigranı** (`.shell::after`,
    radial-gradient halkalar); sayfa başlıklarında pirinç dikey çizgi
    (`.page-head::before`); primary butonlar metalik gradyan + üst kenar
    ışığı; kart/tablo yüzeylerinde gradyan + 1px iç ışık çizgisi; aktif
    nav'da pirinç çerçeveli pill; avatar'da konik pirinç halka.
  - Kasa tablosu `table-layout: fixed` (`.table-fixed`): kolon genişlikleri
    yalnız başlık satırından gelir — Göster/reveal/loading içeriği kolonları
    asla oynatamaz (auto layout'ta frame'lik jiggle yaşandı, çözüm bu).
    Şifre kolonu 380px, aksiyon 170px.
  - `prefers-reduced-motion` her şeyi kapatır (scramble dahil — hook
    içinde matchMedia kontrolü). Kullanıcının KDE'sinde animasyonlar açık
    (AnimationDurationFactor 0.25), bu kural tetiklenmiyor.
  Tüm testler yeşil.
- Doğrulama pratiği: UI değişikliği sonrası playwright-core script'le
  (scratchpad; chromium `~/.cache/ms-playwright/chromium-1234/...`,
  headless'ta `--enable-unsafe-swiftshader` WebGL için) 8081'den screenshot
  alınıp göze bakılıyor. `localhost:8081` docker'daki BUILD'i gösterir —
  değişiklik görünmüyorsa önce `docker compose up --build -d web`.
