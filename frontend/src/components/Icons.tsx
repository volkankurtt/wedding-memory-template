import type { ReactNode } from 'react'

type IconProps = {
  size?: number
  className?: string
}

const stroke = {
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.7,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
}

function Svg({ size = 18, className, children }: IconProps & { children: ReactNode }) {
  return (
    <svg
      className={`icon ${className ?? ''}`.trim()}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      aria-hidden
    >
      {children}
    </svg>
  )
}

export function IconHeart({ size = 16, className }: IconProps) {
  return (
    <Svg size={size} className={className}>
      <path
        {...stroke}
        d="M12 20s-7-4.4-9.2-8.2C1.2 9 2.4 5.8 5.6 5.2c1.8-.4 3.4.4 4.4 1.8 1-1.4 2.6-2.2 4.4-1.8 3.2.6 4.4 3.8 2.8 6.6C19 15.6 12 20 12 20z"
      />
    </Svg>
  )
}

export function IconCamera({ size = 18, className }: IconProps) {
  return (
    <Svg size={size} className={className}>
      <path {...stroke} d="M4.8 8.2h2.1l1.2-1.8h7.8l1.2 1.8h2.1A1.8 1.8 0 0 1 21 10v7.2A1.8 1.8 0 0 1 19.2 19H4.8A1.8 1.8 0 0 1 3 17.2V10a1.8 1.8 0 0 1 1.8-1.8z" />
      <circle {...stroke} cx="12" cy="13.4" r="2.6" />
    </Svg>
  )
}

export function IconGallery({ size = 18, className }: IconProps) {
  return (
    <Svg size={size} className={className}>
      <rect {...stroke} x="3.4" y="5.2" width="14.2" height="12.4" rx="1.6" />
      <path {...stroke} d="M7.2 19h12.2c.9 0 1.6-.7 1.6-1.6V8.6" />
      <circle {...stroke} cx="8.4" cy="9.6" r="1.1" />
      <path {...stroke} d="m6.2 15.2 3.1-3.2 2.4 2.4 2.3-2.6 3.4 3.4" />
    </Svg>
  )
}

export function IconPin({ size = 18, className }: IconProps) {
  return (
    <Svg size={size} className={className}>
      <path {...stroke} d="M12 21s6.2-5.1 6.2-10.1A6.2 6.2 0 0 0 5.8 10.9C5.8 15.9 12 21 12 21z" />
      <circle {...stroke} cx="12" cy="10.6" r="2.1" />
    </Svg>
  )
}

export function IconArrowLeft({ size = 16, className }: IconProps) {
  return (
    <Svg size={size} className={className}>
      <path {...stroke} d="M15.2 5.5 8 12l7.2 6.5" />
      <path {...stroke} d="M8.2 12H20" />
    </Svg>
  )
}
