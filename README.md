## 🇹🇷 SmartSaver – Mobil Finans Yönetimi Uygulaması 💸📊

SmartSaver, Android platformu için geliştirilmiş, **kapsamlı bir kişisel finans yönetim uygulamasıdır**. Kullanıcıların hem **para transferlerini** hem de **hisse yatırımlarını** yönetmesini sağlar. Proje, **CENG427** dersi kapsamında gerçekleştirilmiştir.

---

### 🔍 Ne Sunar?

* Kayıt / Giriş ekranı
* Kullanıcı bakiyesi takibi
* Para gönderme/alma işlemleri
* Hisse senedi alım/satım ekranı (grafikli)
* Geçmiş transferlerin listelenmesi
* Portföy ve istatistik ekranları

---

### 🗃️ Nasıl Kullanılır?

1. **Splash Screen:** Uygulama açılırken karşılar.
2. **Login/Register:** Email, şifre ve isim ile giriş yapılır.
3. **Dashboard:** Bakiyeyi ve geçiş ekranlarını görürsünüz.
4. **Transfer:** Email girerek para gönderilir.
5. **Stocks:** Gerçek zamanlı fiyatlar ve grafiklerle hisse al/sat.
6. **MyStats:** Yatırım özeti, grafikler ve transfer geçmişi.

---

### ⚙️ Kullanılan Teknolojiler

| Katman      | Teknoloji         | Açıklama                     |
| ----------- | ----------------- | ---------------------------- |
| 📱 Mobil    | Java (Android)    | Uygulama gel.ü frontend      |
| 🔁 API      | Volley / JSON     | Veri iletişimi               |
| 📈 Grafik   | MPAndroidChart    | Hisse fiyat grafikleri       |
| 🧠 Yerel DB | SQLite            | Yerel veri saklama           |
| 🌐 Sunucu   | Node.js + Express | API backend                  |
| 📃 Database | SQLite3           | Kullanıcı, transfer, portföy |

---

### 🚀 Kurulum Talimatı

```bash
cd Ceng427Database/
npm init -y
npm install express sqlite3 cors
node server.js
```

* Android Studio'da uygulamayı çalıştır
* `http://10.0.2.2:3000` adresine bağlanarak API ile haberleşir

---

## 🇺🇸 SmartSaver – Mobile Finance Manager 💸📊

SmartSaver is an advanced **personal finance management app** developed for Android. It enables users to **track balance, transfer money, and manage stock investments**. This full-stack project was developed as part of **CENG427 course**.

---

### 🔍 Features

* Login / Register
* Real-time balance tracking
* Send/receive funds
* Buy/sell stocks with live charts
* View investment performance
* Transaction history with names and timestamps

---

### 🗃️ Usage Flow

1. **Splash Screen** on launch
2. **Login/Register** with email, password
3. **Dashboard** shows current balance
4. **Transfer**: enter email + amount
5. **Stock Details**: see charts, trade
6. **Stats Page**: track profit/loss & history

---

### ⚙️ Tech Stack

| Layer       | Technology        | Description                 |
| ----------- | ----------------- | --------------------------- |
| 📱 Mobile   | Java (Android)    | Frontend app logic          |
| 🔁 API      | Volley / JSON     | Server communication        |
| 📈 Charts   | MPAndroidChart    | Price chart visualization   |
| 🧠 Local DB | SQLite            | Offline stock data          |
| 🌐 Backend  | Node.js + Express | API server                  |
| 📃 Database | SQLite3           | Users, transfers, portfolio |

---

### 🚀 Setup Instructions

```bash
cd Ceng427Database/
npm init -y
npm install express sqlite3 cors
node server.js
```

* Run the Android app in Android Studio
* Connects to `http://10.0.2.2:3000` for backend APIs
