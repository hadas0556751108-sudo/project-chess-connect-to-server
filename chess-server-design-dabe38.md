# תכנון ארכיטקטורת שרת שחמט בעומסים גבוהים

תכנון מלא לארכיטקטורת צד-שרת מבוססת מיקרו-שירותים ב-Java, עם מיפוי חדרים, פיזור עומסים, חישובי תעבורה ו-Docker Desktop לסביבת פיתוח, שמיועד לתמוך ב-100 מיליון משתמשים רשומים וב-10 מיליון משחקים במקביל.

## 1. סקלביליות ומסדי נתונים

### האם SQLite מתאים?

**לא.** SQLite מתאים רק לדגם מקומי או סביבת פיתוח בודדת:

- מבוסס קובץ אחד — לא ניתן לשcale أفقی (horizontal scaling).
- כתיבה סדרתית (single writer) — ב-10 מיליון משחקים במקביל ייווצר צוואר בקבוק בלתי-סביל.
- אין שכפול מובנה (replication) או שרתים.
- לא ניתן לחלק נתונים בין שרתי משחק (sharding).

### מסדי הנתונים המומלצים

| תפקיד | מסד נתונים | הסיבה |
|-------|------------|--------|
| פרופילים, אימות, ELO, סטטיסטיקות | **MongoDB** (replica set + sharding) | Schema גמיש, אינדקסים עוצמתיים, scaling אופקי, תמיכה ב-100M+ מסמכים. מתאים למשתמשים והיסטוריית משחקים. |
| מצב משחק בזמן אמת, חדרים, sessions, נוכחות | **Redis Cluster** | תת-שנייה latency, pub/sub, data structures (hashes, sorted sets, streams), TTL אוטומטי. |
| תור Matchmaking | **Redis Sorted Set** | חיפוש ELO range מהיר (`ZRANGEBYSCORE`), atomic operations. |
| אירועים בין שירותים | **Redis Pub/Sub** או **NATS/Kafka** | reliable streaming, decoupling, replay capabilities. |
| היסטוריית מהלכים / time-series ענק | **Cassandra / ScyllaDB** (אופציונלי) | כתיבה מאסיבית של move logs, זול יותר מ-MongoDB לנתוני time-series. |

**MongoDB** יהיה בסיס הנתונים הראשי לנתונים עסקיים (משתמשים, ELO, היסטוריה, תוצאות), אך לא ל-state בזמן אמת של לוחות וחדרים.

## 2. ארכיטקטורת שירותים (Microservices)

```
Clients (CLI + Swing UI + WebSockets)
        |
   [DNS / CDN]
        |
   [L4/L7 Load Balancer]
        |
   [API Gateway / WS Gateway]
        |
   +----------------+----------------+----------------+
   |                |                |                |
Auth Service   Matchmaking    Game Servers    User/Stats
   |             Service         (stateful)      Service
   |                |                |                |
MongoDB        Redis Queue      Redis Cluster     MongoDB
               Room Directory   (state + pub/sub)
```

### שירותים עיקריים

1. **API Gateway / WS Gateway**
   - מקבל חיבורי WebSocket מאימות (JWT).
   - מנתב הודעות לפי `userId` / `roomId` לשרת המשחק המתאים.
   - Rate limiting, TLS, DDoS protection.

2. **Auth Service**
   - Login/Register, JWT issuance, password hashing (bcrypt/Argon2).
   - קריאה/כתיבה ל-MongoDB `users` collection.

3. **Matchmaking Service**
   - מנהל תור שחקנים ב-Redis Sorted Set לפי ELO.
   - מחפש זיווג בטווח ±100 ELO.
   - אם אין זיווג בתוך 60 שניות — שולח `NO_MATCH`.
   - כשנמצאת התאמה: יוצר חדר, בוחר Game Server, מעדכן Room Directory ומתחיל משחק.

4. **Game Server (Java + Netty / Spring WebFlux)**
   - מחזיק את כל חדרים שהוקצו לו בזיכרון.
   - מטפל בתורות, מהלכים, התנתקויות, כלים שרירים, סוף משחק.
   - מפרסם עדכונים ל-viewers דרך Redis Pub/Sub.
   - כותב תוצאה והיסטוריה ל-MongoDB בסיום.

5. **Room Directory Service**
   - מיפוי `roomId -> gameServerId` ו-`userId -> roomId/serverId` ב-Redis Hash.
   - Gateway משתמש בו כדי לדעת לאיזה שרת לנתב.

6. **Spectator Fan-Out Service (optional but critical for massive viewers)**
   - מקבל עדכוני מהלכים מ-Redis Streams.
   - מפיץ לשרתי קצה (edge WebSocket nodes) שמחזיקים חיבורי צופים.
   - מאפשר מיליוני צופים בחדר בודד מבלי לחנוק את Game Server.

7. **Event Bus**
   - Redis Pub/Sub לעדכונים בזמן אמת (ELO update, sounds, animations).
   - Kafka/NATS לאירועים עסקיים ו-reliable processing (למשל עדכון ELO, analytics).

8. **User/Stats Service**
   - ELO updates, leaderboards, game history, statistics.
   - MongoDB + secondary indexes.

## 3. ניהול שרתים וחדרים

### חלוקת עומס בין שרתים

- **Consistent Hashing** על `roomId` כדי לקבוע איזה Game Server מחזיק חדר מסוים.
- Gateway שומר טבלה מרכזית (Redis) של `room -> server` ו-`user -> server`.
- כל משתמש וכל צופה מנותב לשרת שבו החדר שלהם נמצא (או ל-edge spectator node).
- **Load-aware assignment**: כשיוצרים חדר חדש, בוחרים את השרת הכי פנוי מבחינת חדרים/CPU/זיכרון, במסגרת ה-hash.

### איך יודעים איפה כל שחקן נמצא?

- Redis מחזיק מפתח: `user:{username}:server -> {serverId}`.
- כששחקן מתחבר, Gateway שומר את `connectionId -> user -> server`.
- כששחקן מתנתק, מסומן כ-`disconnected` עם TTL של 20 שניות (disconnection countdown).
- Reconnect: Gateway מחפש את השרת הקודם; אם השרת נפל, עוברים לשרת חלופי ושחזור מ-Redis WAL/events.

### ניהול חדרים מרובי משתמשים

- כל חדר כולל 2 שחקנים + N צופים (ללא הגבלה קשיחה).
- Game Server שומר את מצב החדר בזיכרון (board state, turn, timers, disconnected players).
- State events נכתבים גם ל-Redis Stream / WAL לשחזור אם השרת נופל.
- Viewers מנותבים ל-**Spectator Edge Nodes** שמקבלים את מהלכים מ-Redis Pub/Sub; כך Game Server לא מטפל ישירות במיליוני צופים.

### התמודדות עם נפילת שרת

- Rooms כתובים ל-Redis Stream כל מהלך.
- שרת חדש יכול לשחזר מצב חדר מה-event log.
- שחקנים מחוברים מחדש לשרת חלופי; countdown disconnection מתחדש או נשמר ב-Redis TTL.

## 4. חישוב תעבורת רשת

### הנחות

- 10 מיליון משחקים במקביל (concurrent games).
- כל משחק נמשך בממוצע 30–90 שניות.
- שחקן פעיל מבצע מהלך כל 2 שניות (כלומר: 1 מהלך ל-2 שניות למשחק, כי תורות לסירוגין).
- גודל הודעת MOVE כולל JSON + WebSocket/TCP overhead: **~200 bytes**.
- Keepalive פעם ב-30 שניות, ~10 bytes.

### קצב מהלכים

```
10,000,000 משחקים / 2 שניות = 5,000,000 מהלכים לשנייה
```

### תעבורה לפי מספר צופים ממוצע לחדר

| ממוצע צופים לחדר | סה"כ חיבורים | Inbound (מהלכים) | Outbound (broadcast) | סה"כ רוחב פס לערוץ תעבורה |
|------------------|---------------|------------------|-----------------------|-----------------------------|
| 0 (רק 2 שחקנים)  | 20 מיליון     | 1 GB/s           | 2 GB/s                | ~24 Gbps                    |
| 10               | 120 מיליון    | 1 GB/s           | 12 GB/s               | ~104 Gbps                   |
| 100              | 1.02 מיליארד  | 1 GB/s           | 102 GB/s              | ~824 Gbps                   |
| 1,000            | 10.02 מיליארד | 1 GB/s           | ~1 TB/s               | ~8.2 Tbps                   |

חישוב לדוגמה (10 צופים ממוצע):

- **Inbound**: 5M moves/sec × 200 bytes = **1,000 MB/s = 1 GB/s = 8 Gbps**
- **Outbound**: 5M moves/sec × (2 players + 10 viewers) × 200 bytes = **12,000 MB/s = 12 GB/s = 96 Gbps**
- **Keepalive**: 120M connections / 30s = 4M ping/sec + 4M pong/sec = 8M × 10 bytes = 80 MB/s = 640 Mbps (זניח)

### מסקנות מתעבורה

- ל-10M משחקים ללא צופים רבים, Game Servers צריכים לטפל ב-~24 Gbps בסך הכל — בר השגה עם רשת 10/25 Gbps ופיזור על עשרות/מאות שרתים.
- עם ממוצע של 100+ צופים לחדר, חייבים **Spectator Edge Nodes** ואינפראSTRUCTURE פיזור/הפצה (CDN-like edges או Redis Streams fan-out).
- לא ניתם לשדר מהלך אחד ב-uniicast למיליון צופים משאב Game Server יחיד; לכן נדרש **application-level multicast** דרך pub/sub בין edge servers.

## 5. Docker Desktop לסביבת פיתוח מקומית

Docker Desktop ישמש להרמת כל השירותים הנדרשים לפיתוח ובדיקות אינטגרציה. הוא **לא מתאים ל-Production בscale 10M משחקים**, אך מאפשר חיקוי ארכיטקטורה מלאה מקומית.

### שירותים ב-docker-compose

| שירות | תמונה / מימוש | תפקיד |
|-------|---------------|--------|
| `nginx` | nginx:latest | API Gateway + WebSocket reverse proxy |
| `auth-service` | Java 21 image | Login/Register, JWT |
| `matchmaking-service` | Java 21 image | Matchmaking queue |
| `game-server` | Java 21 image | משחקים בזמן אמת; ניתן ל-scale עם `--scale game-server=3` |
| `redis` | redis:7-alpine | Cache, session, room directory, pub/sub |
| `mongodb` | mongo:7 | מסד נתונים ראשי |
| `prometheus` | prom/prometheus | מדדים |
| `grafana` | grafana/grafana | ויזואליזציה |
| `jaeger` | jaegertracing/all-in-one | distributed tracing |

### הרצה מומלצת

```bash
docker compose up -d --scale game-server=3
```

לבדיקת scale גבוהה יותר מקומית, ניתן להפעיל את ה-Kubernetes המובנה ב-Docker Desktop (`Enable Kubernetes`) ולפרוס את השירותים עם Deployments ו-HPA.

### כללים

- כל שירות Java ירוץ בתוך container נפרד עם health checks.
- Redis ו-MongoDB ישתמשו ב-Docker volumes לנתינות נתונים.
- הגדרות דרך `application.yml` / environment variables.
- לא ישמרו secrets בתוך images; ישתמשו ב-Docker secrets / env files.

## 6. אבני דרך ליישום

1. הקמת Gateway + Auth Service + MongoDB + Redis.
2. מעבר מ-SQLite ל-MongoDB (`UserRepository`) והוספת JWT.
3. פיתוח Game Server מבוסס Netty עם WebSocket.
4. מימוש Room Directory ב-Redis ו-matchmaking ב-Redis Sorted Set.
5. שילוב Event Bus (Redis Pub/Sub) וסטור של game events.
6. בניית Spectator Fan-Out Service לצופים המוניים.
7. הגדרת Docker Compose ו-Kubernetes manifests.
8. Observability: metrics, logs, tracing.
