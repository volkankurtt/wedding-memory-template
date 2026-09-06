export type Photo = {
  id: string
  fileName: string
  fileUrl: string
  createdAt: string
  isLocal?: boolean
}

export type Memory = {
  id: string
  name: string
  message: string
  createdAt: string
}

export type BackgroundImage = {
  file: string
  src: string
  desktopPosition: string
  mobilePosition: string
}

export type WeddingInfo = {
  brideName: string
  groomName: string
  weddingDate: string
  weddingTime: string
  timezone: string
  venueName: string
  address: string
  venueShort: string
  googleMapsUrl: string
  heroTitle: string
  heroTitleLines: [string, string]
  heroDescription: string
  galleryEmpty: string
  memoriesEmpty: string
  memoryLead: string
}
