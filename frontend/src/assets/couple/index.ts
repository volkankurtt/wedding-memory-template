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
