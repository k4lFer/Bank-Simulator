import type { ButtonHTMLAttributes } from 'react'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'ghost'
}

export default function Button({ variant = 'primary', className = '', ...props }: Props) {
  const base = 'rounded font-medium cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed transition-colors'
  const styles = {
    primary: 'bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 text-base',
    ghost: 'bg-white/20 hover:bg-white/30 text-white border border-white/30 px-3 py-1.5 text-sm',
  }
  return <button className={`${base} ${styles[variant]} ${className}`} {...props} />
}
