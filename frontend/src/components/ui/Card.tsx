import type { ReactNode } from 'react'
import { cn } from '../../lib/utils'

interface Props {
  children: ReactNode
  className?: string
  padding?: 'sm' | 'md' | 'lg'
  hover?: boolean
}

export default function Card({ children, className, padding = 'md', hover = false }: Props) {
  const paddingMap = { sm: 'p-4', md: 'p-5', lg: 'p-6' }
  return (
    <div
      className={cn(
        'bg-white rounded-xl border border-gray-100 shadow-sm',
        paddingMap[padding],
        hover && 'hover:shadow-md hover:border-gray-200 transition-all duration-200',
        className
      )}
    >
      {children}
    </div>
  )
}
