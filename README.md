# BrassVault — Ekip Şifre Kasası

Şirket içi, kendi sunucunuzda çalışan ekip credential kasası. Admin ekipler
oluşturur, kullanıcıları ekler ve ortak credential'ları girer; ekip üyeleri
bunları listeler ve şifreyi kontrollü şekilde görüntüler. Her şifre
görüntüleme denetim kaydına yazılır.

## Hızlı başlangıç

```bash
cp .env.example .env
# .env içini doldur:
#   VAULT_MASTER_KEY=$(openssl rand -base64 32)
#   JWT_SECRET=$(openssl rand -base64 48)
#   ADMIN_INITIAL_PASSWORD=<ilk admin şifresi>

docker compose up --build
```

Arayüz: http://localhost:8081

İlk açılışta boş veritabanına `ADMIN_EMAIL` (varsayılan
`admin@brassvault.local`) ile bir bootstrap admin oluşturulur ve ilk girişte
şifre değiştirmeye zorlanır. `ADMIN_INITIAL_PASSWORD` boşsa admin
oluşturulmaz.

> Not: JWT cookie'si `Secure` bayraklıdır. Tarayıcılar `localhost`'u güvenli
> bağlam saydığı için lokal kullanımda sorun çıkmaz; sunucuya kurarken
> uygulamayı TLS arkasına koyun.

## Mimari

```
web (nginx, React 18 + TS + Vite)  →  /api proxy  →  api (Spring Boot 3, Java 21)  →  PostgreSQL 16
```

- **Şifreleme**: Kasa şifreleri AES-256-GCM ile şifrelenir. Master key
  `VAULT_MASTER_KEY` env değişkeninden okunur (base64, 32 byte); yoksa veya
  hatalıysa uygulama başlamaz. Blob formatı `IV(12) || ciphertext || tag(16)`,
  her şifrelemede `SecureRandom` ile yeni IV üretilir, item id'si AAD olarak
  bağlanır — bir item'ın blob'u başka item id'siyle çözülemez.
- **Login şifreleri**: BCrypt (strength 12).
- **Yetkilendirme**: Repository sorgularının içinde (`JOIN team_members`).
  Üyesi olunmayan ekibin item'ı istek sahibi için hiç yoktur → 404.
- **JWT**: 8 saatlik access token; httpOnly + Secure + SameSite=Strict
  cookie. Refresh token yok.
- **Audit**: LOGIN, VIEW_SECRET, CREATE/UPDATE/DELETE_ITEM,
  ADD/REMOVE_MEMBER kayıtları snapshot alanlarla tutulur (kullanıcı veya
  item silinse de kayıt kalır).

## API

| Method | Path | Açıklama |
|---|---|---|
| POST | `/api/auth/login` | email+password → JWT cookie |
| POST | `/api/auth/logout` | cookie temizle |
| GET | `/api/me` | profil + üye olunan ekipler |
| POST | `/api/me/password` | kendi şifresini değiştir |
| GET | `/api/teams` | üyesi olunan ekipler |
| GET | `/api/teams/{id}/items` | item listesi (şifre HARİÇ) |
| GET | `/api/items/{id}/secret` | şifreyi çözer, VIEW_SECRET audit'i yazar |
| POST/GET | `/api/admin/users` | kullanıcı oluştur (tek seferlik geçici şifre) / listele |
| PATCH | `/api/admin/users/{id}` | rol değiştir / deaktive et |
| POST/GET | `/api/admin/teams` | ekip oluştur / listele |
| GET/POST | `/api/admin/teams/{id}/members` | üyeleri listele / üye ekle |
| DELETE | `/api/admin/teams/{id}/members/{uid}` | üye çıkar |
| POST | `/api/admin/items` | item oluştur |
| PUT/DELETE | `/api/admin/items/{id}` | item güncelle / sil |
| GET | `/api/admin/audit-logs` | filtreli + sayfalı denetim kayıtları |

`must_change_password` bayrağı setliyken şifre değiştirme ve auth
endpoint'leri dışındaki her istek 403 döner.

## Geliştirme

```bash
# Backend (Docker gerekli — testler Testcontainers ile gerçek Postgres kullanır)
cd api && ./mvnw test

# Frontend
cd web && pnpm install && pnpm test && pnpm dev   # dev server /api'yi :8080'e proxy'ler
```

Backend'i lokalde çalıştırmak için:

```bash
cd api
VAULT_MASTER_KEY=$(openssl rand -base64 32) \
DB_URL=jdbc:postgresql://localhost:5432/brassvault \
ADMIN_INITIAL_PASSWORD=dev-admin-pass ./mvnw spring-boot:run
```

## Test kapsamı (özet)

- Aynı plaintext iki şifrelemede farklı ciphertext üretir (rastgele IV).
- Blob'un herhangi bir byte'ı bozulursa decrypt exception fırlatır.
- Blob başka item id'siyle (AAD) çözülemez.
- Üye olunmayan ekibin item'ı 404 döner (403 değil — varlık sızdırılmaz).
- USER rolü her `/api/admin/**` endpoint'inden 403 alır (endpoint başına test).
- Ekipten çıkarılan kullanıcı bir sonraki istekte erişimi kaybeder.
- `VAULT_MASTER_KEY` yokken uygulama başlamaz.
- Deaktive kullanıcı login olamaz.
- `must_change_password` kilidi diğer endpoint'leri kapatır.
- Frontend: 30 sn sonra şifrenin otomatik gizlenmesi, API hata yolları,
  zorunlu şifre değiştirme ekranı.
