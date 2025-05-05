// server.js
const express  = require('express');
const sqlite3  = require('sqlite3').verbose();
const cors     = require('cors');
const path     = require('path');

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

/* ---------- DATABASE ---------- */
const dbPath = path.resolve(__dirname, 'database.db');
console.log('📂 Using DB:', dbPath);
const db = new sqlite3.Database(dbPath);

// Ensure portfolio table exists
db.run(
  `CREATE TABLE IF NOT EXISTS portfolio (
     id           INTEGER PRIMARY KEY AUTOINCREMENT,
     user_id      INTEGER NOT NULL,
     stock_code   TEXT    NOT NULL,
     quantity     INTEGER NOT NULL,
     avg_price    REAL    NOT NULL,
     UNIQUE(user_id, stock_code),
     FOREIGN KEY(user_id) REFERENCES users(id)
   )`
);

/* -------------------------------------------------- */
/*                    AUTH & PROFILE                  */
/* -------------------------------------------------- */

app.get('/incoming/:user_id', (req, res) => {
  const { since_id = 0 } = req.query;
  const sql = `
    SELECT  t.id,
            t.amount,
            t.date,
            su.full_name AS sender_name
    FROM    transactions t
    JOIN    user_profiles su ON su.user_id = t.user_id
    WHERE   t.target_user_id = ?
      AND   t.id > ?
    ORDER BY t.id DESC
    LIMIT 1
  `;
  db.get(sql, [req.params.user_id, since_id], (err, row) => {
    if (err)   return res.status(500).json({ error: err.message });
    if (!row)  return res.json({ new: false });
    res.json({ new: true, transfer: row });
  });
});

/* (isteğe bağlı) hız için indeks */
db.run('CREATE INDEX IF NOT EXISTS idx_transactions_target_id ON transactions(target_user_id)');


// Register
app.post('/register', (req, res) => {
  const { email, password, full_name } = req.body;
  if (!email || !password || !full_name) {
    return res.status(400).json({ error: 'All fields are required.' });
  }
  db.get(
    'SELECT * FROM users WHERE email = ?',
    [email],
    (err, existing) => {
      if (err) return res.status(500).json({ error: err.message });
      if (existing) return res.status(400).json({ error: 'User already exists' });

      db.run(
        'INSERT INTO users (email, password) VALUES (?, ?)',
        [email, password],
        function(err1) {
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
        }
      );
    }
  );
});

// Login
app.post('/login', (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required.' });
  }
  db.get(
    'SELECT id, email FROM users WHERE email = ? AND password = ?',
    [email, password],
    (err, row) => {
      if (err) return res.status(500).json({ error: err.message });
      if (!row) return res.status(401).json({ error: 'Invalid credentials' });
      res.json(row);
    }
  );
});

// Get basic user info
app.get('/users/:id', (req, res) => {
  db.get(
    'SELECT id, email FROM users WHERE id = ?',
    [req.params.id],
    (err, row) => {
      if (err) return res.status(500).json({ error: err.message });
      if (!row) return res.status(404).json({ error: 'User not found' });
      res.json(row);
    }
  );
});

// Get full profile (including balance)
app.get('/user_profiles/:user_id', (req, res) => {
  db.get(
    'SELECT * FROM user_profiles WHERE user_id = ?',
    [req.params.user_id],
    (err, row) => {
      if (err) return res.status(500).json({ error: err.message });
      if (!row) return res.status(404).json({ error: 'Profile not found' });
      res.json(row);
    }
  );
});

/* -------------------------------------------------- */
/*                    FAVORITES                       */
/* -------------------------------------------------- */

// Add a favorite
app.post('/favorites', (req, res) => {
  const { user_id, favorite_user_id } = req.body;
  db.run(
    'INSERT INTO favorites (user_id, favorite_user_id) VALUES (?, ?)',
    [user_id, favorite_user_id],
    function(err) {
      if (err) return res.status(500).json({ error: err.message });
      res.json({ success: true, id: this.lastID });
    }
  );
});

// List your favorites
app.get('/favorites/:user_id', (req, res) => {
  const sql = `
    SELECT u.id, u.email, p.full_name, p.balance
    FROM favorites f
    JOIN users u ON f.favorite_user_id = u.id
    JOIN user_profiles p ON u.id = p.user_id
    WHERE f.user_id = ?
  `;
  db.all(sql, [req.params.user_id], (err, rows) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(rows);
  });
});

/* -------------------------------------------------- */
/*                  TRANSACTIONS                      */
/* -------------------------------------------------- */

// Get your sent *and* received transfers, with full names
app.get('/transactions/:user_id', (req, res) => {
  const user_id = req.params.user_id;
  const sql = `
    SELECT
      t.id,
      t.user_id,
      t.target_user_id,
      t.amount,
      t.price,
      t.date,
      su.full_name   AS sender_name,
      tu.full_name   AS target_name
    FROM transactions t
    JOIN user_profiles su ON t.user_id = su.user_id
    JOIN user_profiles tu ON t.target_user_id = tu.user_id
    WHERE t.user_id = ? OR t.target_user_id = ?
    ORDER BY t.date DESC
  `;
  db.all(sql, [user_id, user_id], (err, rows) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(rows);
  });
});

// Lookup user_id by email
app.get('/get_user_id_by_email', (req, res) => {
  const { email } = req.query;
  if (!email) return res.status(400).json({ error: 'Email is required' });
  db.get(
    'SELECT id FROM users WHERE email = ?',
    [email],
    (err, row) => {
      if (err) return res.status(500).json({ error: err.message });
      if (!row) return res.status(404).json({ error: 'User not found' });
      res.json(row);
    }
  );
});

// Perform a transfer (updates both balances + logs a transaction)
app.post('/transactions', (req, res) => {
  const { user_id, target_user_id, type, stock_code, amount, price, date } = req.body;
  db.serialize(() => {
    db.get(
      'SELECT balance FROM user_profiles WHERE user_id = ?',
      [user_id],
      (err, row) => {
        if (err || !row) return res.status(400).json({ error: 'Sender not found' });
        if (row.balance < amount) return res.status(400).json({ error: 'Insufficient funds' });

        db.run('BEGIN TRANSACTION');
        db.run(
          'UPDATE user_profiles SET balance = balance - ? WHERE user_id = ?',
          [amount, user_id],
          (e1) => {
            if (e1) return rollback(e1);
            db.run(
              'UPDATE user_profiles SET balance = balance + ? WHERE user_id = ?',
              [amount, target_user_id],
              (e2) => {
                if (e2) return rollback(e2);
                db.run(
                  `INSERT INTO transactions
                    (user_id, target_user_id, type, stock_code, amount, price, date)
                   VALUES (?, ?, ?, ?, ?, ?, ?)`,
                  [user_id, target_user_id, type, stock_code, amount, price, date],
                  function(e3) {
                    if (e3) return rollback(e3);
                    db.run('COMMIT');
                    res.json({ success: true, transaction_id: this.lastID });
                  }
                );
              }
            );
          }
        );

        function rollback(err) {
          db.run('ROLLBACK');
          res.status(500).json({ error: err.message });
        }
      }
    );
  });
});

/* -------------------------------------------------- */
/*                      PORTFOLIO                     */
/* -------------------------------------------------- */

// Get entire portfolio
app.get('/portfolio/:user_id', (req, res) => {
  db.all(
    'SELECT stock_code, quantity, avg_price FROM portfolio WHERE user_id = ?',
    [req.params.user_id],
    (err, rows) => {
      if (err) return res.status(500).json({ error: err.message });
      res.json(rows);
    }
  );
});

// Get one stock from your portfolio
app.get('/portfolio/:user_id/:code', (req, res) => {
  const { user_id, code } = req.params;
  db.get(
    'SELECT quantity, avg_price FROM portfolio WHERE user_id = ? AND stock_code = ?',
    [user_id, code],
    (err, row) => {
      if (err) return res.status(500).json({ error: err.message });
      res.json(row || { quantity: 0, avg_price: 0 });
    }
  );
});

// Buy shares (upsert + deduct balance)
app.post('/portfolio/buy', (req, res) => {
  const { user_id, stock_code, quantity, price } = req.body;
  if (!user_id || !stock_code || !quantity || !price) {
    return res.status(400).json({ error: 'fields missing' });
  }
  const qty = parseInt(quantity, 10);
  const prc = parseFloat(price);
  if (qty <= 0 || prc <= 0) return res.status(400).json({ error: 'invalid values' });
  const cost = qty * prc;

  db.serialize(() => {
    db.run('BEGIN');
    db.get(
      'SELECT balance FROM user_profiles WHERE user_id = ?',
      [user_id],
      (err, row) => {
        if (err || !row || row.balance < cost) {
          db.run('ROLLBACK');
          return res.status(400).json({ error: 'insufficient balance' });
        }
        // upsert portfolio
        db.run(
          `INSERT INTO portfolio (user_id, stock_code, quantity, avg_price)
           VALUES (?, ?, ?, ?)
           ON CONFLICT(user_id, stock_code) DO UPDATE SET
             avg_price = (portfolio.avg_price * portfolio.quantity
                          + excluded.avg_price * excluded.quantity)
                         / (portfolio.quantity + excluded.quantity),
             quantity = portfolio.quantity + excluded.quantity`,
          [user_id, stock_code, qty, prc]
        );
        // deduct from balance
        db.run(
          'UPDATE user_profiles SET balance = balance - ? WHERE user_id = ?',
          [cost, user_id]
        );
        db.run('COMMIT', () => res.json({ success: true }));
      }
    );
  });
});

// Sell shares (update or delete + add balance)
app.post('/portfolio/sell', (req, res) => {
  const { user_id, stock_code, quantity, price } = req.body;
  if (!user_id || !stock_code || !quantity || !price) {
    return res.status(400).json({ error: 'fields missing' });
  }
  const qty    = parseInt(quantity, 10);
  const prc    = parseFloat(price);
  const income = qty * prc;

  db.serialize(() => {
    db.run('BEGIN');
    db.get(
      'SELECT quantity FROM portfolio WHERE user_id = ? AND stock_code = ?',
      [user_id, stock_code],
      (err, row) => {
        if (err || !row || row.quantity < qty) {
          db.run('ROLLBACK');
          return res.status(400).json({ error: 'not enough shares' });
        }
        const remaining = row.quantity - qty;
        if (remaining === 0) {
          db.run(
            'DELETE FROM portfolio WHERE user_id = ? AND stock_code = ?',
            [user_id, stock_code]
          );
        } else {
          db.run(
            'UPDATE portfolio SET quantity = ? WHERE user_id = ? AND stock_code = ?',
            [remaining, user_id, stock_code]
          );
        }
        db.run(
          'UPDATE user_profiles SET balance = balance + ? WHERE user_id = ?',
          [income, user_id]
        );
        db.run('COMMIT', () => res.json({ success: true }));
      }
    );
  });
});

/* -------------------------------------------------- */
const PORT = 3000;
app.listen(PORT, () => console.log(`✅ API running at http://localhost:${PORT}`));
