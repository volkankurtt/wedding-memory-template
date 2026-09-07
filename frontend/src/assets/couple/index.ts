/**
 * Çift fotoğraflarını buraya koyun: 1.jpg, 2.jpg, 3.jpg …
 * Hangi dosyaların slaytta kullanılacağı `weddingConfig.backgroundPhotos` içindedir.
 */
const modules = import.meta.glob<string>('./*.{jpg,jpeg,png,webp,JPG,JPEG,PNG,WEBP}', {
  eager: true,
  import: 'default',
})

export const coupleByFile = Object.fromEntries(
  Object.entries(modules).map(([path, url]) => {
    const file = path.replace(/\\/g, '/').split('/').pop() ?? path
    return [file, url]
  }),
) as Record<string, string>
