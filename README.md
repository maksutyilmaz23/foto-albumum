# Foto Albümüm

Fotoğraf yükleyip her fotoğrafın altına not eklenebilen, ardından tüm albümü
fotokitap görünümünde PDF olarak dışa aktarabilen Android uygulaması.

## Özellikler
- Galeriden/kameradan birden fazla fotoğraf ekleme
- Her fotoğrafın altına serbest metin notu
- Fotoğrafları yukarı/aşağı taşıyarak sıralama, silme
- Kapak sayfalı, Türkçe karakter destekli PDF fotokitap çıktısı
- Veriler cihazda saklanır (localStorage), internet gerekmez

## APK nasıl üretilir
Bu depoya `main` dalına her push yapıldığında GitHub Actions otomatik olarak
debug APK üretir:
1. **Actions** sekmesi → son çalışan iş akışı → **Artifacts** bölümünden
   `FotoAlbumum-apk` dosyasını indir, veya
2. **Releases** sekmesinden en son sürümdeki `.apk` dosyasını indir.

APK'yı telefona indirip açtığında "bilinmeyen kaynaklardan yükleme" izni
istenebilir; onay verip kurulumu tamamla.
