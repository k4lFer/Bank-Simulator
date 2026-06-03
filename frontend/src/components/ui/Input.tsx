import type { InputHTMLAttributes } from 'react'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
}

export default function Input({ label, className = '', ...props }: Props) {
  return (
    <div className="flex flex-col gap-1">
      {label && <label className="text-xs text-gray-500">{label}</label>}
      <input
        className={`w-full px-3 py-2.5 border border-gray-300 rounded text-base outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 transition-all ${className}`}
        {...props}
      />
    </div>
  )
}
