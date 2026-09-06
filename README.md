# dugun-anisi

Tek düğün için dijital davetiye ve anı sitesi.

## Faz A (şimdi)

Yalnızca React + Vite frontend, mock verilerle çalışır. Backend henüz yok.

```bash
cd frontend
npm install
npm run dev
```

Tarayıcıda `http://localhost:5173` açılır.

- Ana sayfa: `/`
- Galeri: `/gallery`
- Masadaki yükleme QR’si: `/upload`

Düğün bilgileri ve arka plan odak noktaları `frontend/src/config/wedding.ts` içindedir.

Arka plan fotoğrafları `frontend/src/assets/couple/` klasöründen yüklenir (`1.jpg`, `2.jpg` …).

Sayfayı yenileyince yüklenen fotoğraflar ve yeni anılar sıfırlanır (mock).
