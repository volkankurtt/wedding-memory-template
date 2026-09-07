# dugun-anisi

Tek düğün için dijital davetiye, fotoğraf yükleme ve anı defteri.

## Geliştirme

```bash
cd frontend
npm install
npm run dev
```

Backend:

```bash
cd backend
copy .env.example .env
# .env içindeki yer tutucuları kendi değerlerinizle doldurun
.\mvnw.cmd spring-boot:run
```

Tarayıcıda `http://localhost:5173`

- Ana sayfa: `/`
- Galeri: `/gallery`
- Masadaki yükleme: `/upload`

## Yeni bir çift için proje nasıl hazırlanır?

Tasarım ve özellikler aynı kalır. Yalnızca aşağıdaki müşteri bilgilerini ve fotoğrafları değiştirin.

1. **Düğün bilgileri**  
   `frontend/src/config/weddingConfig.ts` dosyasını açın. Gelin/damat adı, tarih, saat, zaman dilimi, mekan, adres, harita araması, sayfa başlığı, indirme dosya öneki, kahraman başlıkları ve boş durum metinlerini yeni çifte göre güncelleyin.

2. **Çift fotoğrafları**  
   Dosyaları `frontend/src/assets/couple/` klasörüne koyun. Önerilen isimler: `1.jpg`, `2.jpg`, `3.jpg` …  
   Hangi dosyaların arka plan slaytında görüneceği ve kadrajı `weddingConfig.backgroundPhotos` dizisindedir. Yeni bir dosya eklediyseniz bu diziye `file`, `desktopPosition` ve `mobilePosition` satırı ekleyin.

3. **Ortam değişkenleri**  
   Kökteki `.env.example` veya `backend/.env.example` dosyasını `backend/.env` olarak kopyalayın. Gerçek veritabanı, Storage ve admin değerlerini buraya yazın; bu dosyayı commit etmeyin.

4. **Çalıştırma**  
   Frontend ve backend’i yukarıdaki gibi başlatın. Yükleme, galeri, indirme ve anı defterinin yeni isim/tarih/mekan ile göründüğünü kontrol edin.

5. **Yayın**  
   `frontend` için `npm run build`, backend için kendi host ortamınıza göre paketleyin. `CORS_ORIGINS` yayınlanan site adresini içermelidir.
