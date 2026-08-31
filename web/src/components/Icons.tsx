interface IconProps {
  size?: number
}

function icon(path: React.ReactNode) {
  return function Icon({ size = 17 }: IconProps) {
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        {path}
      </svg>
    )
  }
}

export const ShieldIcon = icon(
  <>
    <path d="M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6l7-3z" />
    <path d="M9.5 12l2 2 3.5-3.5" />
  </>,
)

export const VaultIcon = icon(
  <>
    <rect x="3.5" y="4" width="17" height="16" rx="2.5" />
    <circle cx="12" cy="12" r="3.8" />
    <path d="M12 8.2V6.8M12 17.2v-1.4M15.8 12h1.4M6.8 12h1.4" />
  </>,
)

export const UsersIcon = icon(
  <>
    <circle cx="9" cy="8.5" r="3" />
    <path d="M3.5 19.5c0-3 2.5-5 5.5-5s5.5 2 5.5 5" />
    <path d="M16 5.8a3 3 0 010 5.4M17.5 14.7c2 .7 3 2.4 3 4.8" />
  </>,
)

export const TeamIcon = icon(
  <>
    <rect x="3.5" y="7.5" width="17" height="12" rx="2" />
    <path d="M8.5 7.5v-2a2 2 0 012-2h3a2 2 0 012 2v2" />
    <path d="M3.5 13h17" />
  </>,
)

export const AuditIcon = icon(
  <>
    <path d="M6 3.5h9l4 4v13H6a1.5 1.5 0 01-1.5-1.5V5A1.5 1.5 0 016 3.5z" />
    <path d="M15 3.5v4h4" />
    <path d="M8.5 12h7M8.5 15.5h5" />
  </>,
)

export const KeyIcon = icon(
  <>
    <circle cx="8" cy="14.5" r="4" />
    <path d="M11 11.5l8.5-8.5M16 6.5l2.5 2.5M13.5 9l2 2" />
  </>,
)

export const LogoutIcon = icon(
  <>
    <path d="M14 4h4a1.5 1.5 0 011.5 1.5v13A1.5 1.5 0 0118 20h-4" />
    <path d="M10 8l-4 4 4 4M6 12h10" />
  </>,
)

export const LockIcon = icon(
  <>
    <rect x="5" y="10.5" width="14" height="9.5" rx="2" />
    <path d="M8 10.5V7.5a4 4 0 018 0v3" />
  </>,
)

export const CopyIcon = icon(
  <>
    <rect x="9" y="9" width="11" height="11" rx="2" />
    <path d="M5.5 15H5a1.5 1.5 0 01-1.5-1.5v-9A1.5 1.5 0 015 3h9a1.5 1.5 0 011.5 1.5V5" />
  </>,
)

export const CheckIcon = icon(<path d="M5 12.5l4.5 4.5L19 7.5" />)
