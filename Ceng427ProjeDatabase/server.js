// server.js
const express  = require('express');
const sqlite3  = require('sqlite3').verbose();
const cors     = require('cors');
const path     = require('path');

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

/* ---------- DB ---------- */
const dbPath = path.resolve(__dirname, 'database.db');
console.log('📂 DB dosyası:', dbPath);
const db = new sqlite3.Database(dbPath);

/* tablo – varsa dokunmaz */
db.run(`CREATE TABLE IF NOT EXISTS portfolio(
          id          INTEGER PRIMARY KEY AUTOINCREMENT,
          user_id     INTEGER NOT NULL,
          stock_code  TEXT    NOT NULL,
          quantity    INTEGER NOT NULL,
          avg_price   REAL    NOT NULL,
          UNIQUE(user_id, stock_code),
          FOREIGN KEY(user_id) REFERENCES users(id)
)`);

/* -------------------------------------------------- */
/*                AUTH & PROFİL ENDPOINT’LER          */
/* -------------------------------------------------- */
// … (register, login, get_user, user_profiles, favorites vs. KODUNUZLA AYNI — DEĞİŞMEDİ)


// ✅ Kullanıcı kaydı (email + şifre, ardından profil)
app.post('/register', (req, res) => {
  const { email, password, full_name } = req.body;

  if (!email || !password || !full_name) {
    return res.status(400).json({ error: 'All fields are required.' });
  }

  db.get('SELECT * FROM users WHERE email = ?', [email], (err, user) => {
    if (err) return res.status(500).json({ error: err.message });
    if (user) return res.status(400).json({ error: 'User already exists' });

    db.run('INSERT INTO users (email, password) VALUES (?, ?)', [email, password], function (err1) {
      if (err1) return res.status(500).json({ error: err1.message });

      const userId = this.lastID;
      db.run(
        'INSERT INTO user_profiles (user_id, full_name, balance) VALUES (?, ?, ?)',
        [userId, full_name, 0],
        (err2) => {
          if (err2) return res.status(500).json({ error: err2.message });
          res.json({ success: true, id: userId });
        }
      );
    });
  });
});

// ✅ Kullanıcı girişi
app.post('/login', (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required.' });
  }

  db.get('SELECT id, email FROM users WHERE email = ? AND password = ?', [email, password], (err, row) => {
    if (err) return res.status(500).json({ error: err.message });
    if (!row) return res.status(401).json({ error: 'Invalid credentials' });
    res.json(row);
  });
});

// ✅ Kullanıcı bilgisi (sadece e-mail ve id)
app.get('/users/:id', (req, res) => {
  const id = req.params.id;
  db.get('SELECT id, email FROM users WHERE id = ?', [id], (err, row) => {
    if (err) return res.status(500).json({ error: err.message });
    if (!row) return res.status(404).json({ error: 'User not found' });
    res.json(row);
  });
});

// ✅ Kullanıcı profili (isim ve bakiye)
app.get('/user_profiles/:user_id', (req, res) => {
  const user_id = req.params.user_id;
  db.get('SELECT * FROM user_profiles WHERE user_id = ?', [user_id], (err, row) => {
    if (err) return res.status(500).json({ error: err.message });
    if (!row) return res.status(404).json({ error: 'Profile not found' });
    res.json(row);
  });
});

// ✅ Yeni favori kullanıcı ekle
app.post('/favorites', (req, res) => {
  const { user_id, favorite_user_id } = req.body;
  db.run(
    'INSERT INTO favorites (user_id, favorite_user_id) VALUES (?, ?)',
    [user_id, favorite_user_id],
    function (err) {
      if (err) return res.status(500).json({ error: err.message });
      res.json({ success: true, id: this.lastID });
    }
  );
});

// ✅ İşlem geçmişi (gönderen)
app.get('/transactions/:user_id', (req, res) => {
  const user_id = req.params.user_id;
  db.all('SELECT * FROM transactions WHERE user_id = ? ORDER BY date DESC', [user_id], (err, rows) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(rows);
  });
});



// Kullanıcı ID'sini email ile getir
app.get('/get_user_id_by_email', (req, res) => {
  const { email } = req.query;
  if (!email) return res.status(400).json({ error: 'Email is required' });

  db.get('SELECT id FROM users WHERE email = ?', [email], (err, row) => {
    if (err) return res.status(500).json({ error: err.message });
    if (!row) return res.status(404).json({ error: 'User not found' });
    res.json(row);
  });
});



// ✅ Para transferi (güvenli işlem)
app.post('/transactions', (req, res) => {
  const { user_id, target_user_id, type, stock_code, amount, price, date } = req.body;

  db.serialize(() => {
    db.get('SELECT balance FROM user_profiles WHERE user_id = ?', [user_id], (err, row) => {
      if (err || !row) return res.status(400).json({ error: 'Sender not found' });

      const senderBalance = row.balance;
      if (senderBalance < amount) {
        return res.status(400).json({ error: 'Insufficient funds' });
      }

      db.run('BEGIN TRANSACTION');

      db.run(
        'UPDATE user_profiles SET balance = balance - ? WHERE user_id = ?',
        [amount, user_id],
        (err1) => {
          if (err1) return rollback(err1);
          db.run(
            'UPDATE user_profiles SET balance = balance + ? WHERE user_id = ?',
            [amount, target_user_id],
            (err2) => {
              if (err2) return rollback(err2);

              db.run(
                'INSERT INTO transactions (user_id, target_user_id, type, stock_code, amount, price, date) VALUES (?, ?, ?, ?, ?, ?, ?)',
                [user_id, target_user_id, type, stock_code, amount, price, date],
                function (err3) {
                  if (err3) return rollback(err3);
                  db.run('COMMIT');
                  return res.json({ success: true, transaction_id: this.lastID });
                }
              );
            }
          );
        }
      );
    });

    function rollback(err) {
      db.run('ROLLBACK');
      return res.status(500).json({ error: err.message });
    }
  });
});











/* -------------------------------------------------- */
/*                      PORTFÖY                       */
/* -------------------------------------------------- */

/* Tek hisse (user_id + code) */
app.get('/portfolio/:user_id/:code', (req,res)=>{
  const { user_id, code } = req.params;
  db.get(`SELECT quantity, avg_price
          FROM portfolio WHERE user_id=? AND stock_code=?`,
         [user_id, code],
         (e,row)=> res.json(row || { quantity:0, avg_price:0 }));
});

/* Tüm portföy */
app.get('/portfolio/:user_id', (req,res)=>{
  db.all(`SELECT stock_code, quantity, avg_price
          FROM portfolio WHERE user_id=?`,
         [req.params.user_id],
         (e,rows)=> e?res.status(500).json({error:e.message}):res.json(rows));
});

/* ---------------- BUY ---------------- */
app.post('/portfolio/buy', (req,res)=>{
  const { user_id, stock_code, quantity, price } = req.body;
  if(!user_id||!stock_code||!quantity||!price)
      return res.status(400).json({error:'fields'});
  const qty = parseInt(quantity);
  const prc = parseFloat(price);
  if(qty<=0||prc<=0)        return res.status(400).json({error:'vals'});
  const cost = qty*prc;

  db.serialize(()=>{
    db.run('BEGIN');
    /* yeterli bakiye? */
    db.get('SELECT balance FROM user_profiles WHERE user_id=?',[user_id],(e,row)=>{
      if(e||!row||row.balance<cost){ db.run('ROLLBACK');
        return res.status(400).json({error:'bal'}); }

      /* portföy upsert */
      db.run(`INSERT INTO portfolio(user_id,stock_code,quantity,avg_price)
              VALUES(?,?,?,?)
              ON CONFLICT(user_id,stock_code) DO UPDATE SET
                avg_price=(portfolio.avg_price*portfolio.quantity + excluded.avg_price*excluded.quantity)/
                          (portfolio.quantity+excluded.quantity),
                quantity = portfolio.quantity + excluded.quantity`,
              [user_id,stock_code,qty,prc]);

      /* bakiye düş */
      db.run('UPDATE user_profiles SET balance = balance - ? WHERE user_id=?',
             [cost, user_id]);

      db.run('COMMIT',()=>res.json({success:true}));
    });
  });
});

/* ---------------- SELL ---------------- */
app.post('/portfolio/sell', (req,res)=>{
  const { user_id, stock_code, quantity, price } = req.body;
  if(!user_id||!stock_code||!quantity||!price)
      return res.status(400).json({error:'fields'});
  const qty = parseInt(quantity);
  const prc = parseFloat(price);
  const income = qty*prc;

  db.serialize(()=>{
    db.run('BEGIN');
    db.get('SELECT quantity FROM portfolio WHERE user_id=? AND stock_code=?',
           [user_id, stock_code], (e,row)=>{
      if(e||!row||row.quantity<qty){ db.run('ROLLBACK');
        return res.status(400).json({error:'qty'}); }

      /* portföyden düş veya sıfırsa satır sil */
      const newQty = row.quantity - qty;
      if(newQty===0)
         db.run('DELETE FROM portfolio WHERE user_id=? AND stock_code=?',[user_id,stock_code]);
      else
         db.run('UPDATE portfolio SET quantity=? WHERE user_id=? AND stock_code=?',
                [newQty,user_id,stock_code]);

      /* bakiye ekle */
      db.run('UPDATE user_profiles SET balance = balance + ? WHERE user_id=?',
             [income, user_id]);

      db.run('COMMIT',()=>res.json({success:true}));
    });
  });
});

const PORT = 3000;
app.listen(PORT, ()=> console.log(`✅ API running → http://localhost:${PORT}`));
