import type { ReactNode } from 'react'
import { cn } from '../../lib/utils'

interface Column<T> {
  key: string
  header: string
  render: (item: T) => ReactNode
  className?: string
  headerClassName?: string
}

interface Props<T> {
  columns: Column<T>[]
  data: T[]
  loading?: boolean
  emptyText?: string
  onRowClick?: (item: T) => void
}

export default function Table<T extends { id: string | number }>({
  columns, data, loading, emptyText = 'Sin datos', onRowClick,
}: Props<T>) {
  if (loading) return null
  if (data.length === 0) {
    return (
      <div className="p-8 text-center">
        <p className="text-gray-400 text-sm">{emptyText}</p>
      </div>
    )
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-100 bg-gray-50/80">
            {columns.map((col) => (
              <th key={col.key} className={cn('px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider', col.headerClassName)}>
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {data.map((item) => (
            <tr
              key={item.id}
              className={cn('hover:bg-gray-50/50 transition-colors', onRowClick && 'cursor-pointer')}
              onClick={() => onRowClick?.(item)}
            >
              {columns.map((col) => (
                <td key={col.key} className={cn('px-4 py-3', col.className)}>
                  {col.render(item)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
