import { cn } from '../../lib/utils'
import { cva, type VariantProps } from 'class-variance-authority'

const badgeVariants = cva(
  'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium transition-colors',
  {
    variants: {
      variant: {
        default: 'bg-gray-100 text-gray-700',
        success: 'bg-emerald-100 text-emerald-700',
        warning: 'bg-yellow-100 text-yellow-700',
        danger: 'bg-red-100 text-red-700',
        info: 'bg-blue-100 text-blue-700',
        purple: 'bg-purple-100 text-purple-700',
      },
    },
    defaultVariants: { variant: 'default' },
  }
)

interface Props extends VariantProps<typeof badgeVariants> {
  label: string
  className?: string
}

export default function Badge({ label, variant, className }: Props) {
  return <span className={cn(badgeVariants({ variant }), className)}>{label}</span>
}

export function StatusBadge({ status }: { status: string }) {
  const config: Record<string, { variant: 'success' | 'warning' | 'danger' | 'info' | 'default'; label: string }> = {
    ACTIVE: { variant: 'success', label: 'Activo' },
    INACTIVE: { variant: 'default', label: 'Inactivo' },
    BLOCKED: { variant: 'danger', label: 'Bloqueado' },
    PENDING: { variant: 'warning', label: 'Pendiente' },
    DEBITED: { variant: 'info', label: 'Procesando' },
    COMPLETED: { variant: 'success', label: 'Completada' },
    FAILED: { variant: 'danger', label: 'Fallida' },
    REJECTED: { variant: 'danger', label: 'Rechazada' },
  }
  const c = config[status] ?? { variant: 'default' as const, label: status }
  return <Badge variant={c.variant} label={c.label} />
}
