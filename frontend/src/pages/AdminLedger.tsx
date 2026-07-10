import { useState, useEffect, useRef, useCallback } from 'react'
import { toast } from 'sonner'
import axios from 'axios'
import { ledgerApi } from '../api/ledger-api'
import { useAuth } from '../hooks/useAuth'
import Button from '../components/ui/Button'
import Spinner from '../components/ui/Spinner'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import { formatCurrency, formatDate, truncate } from '../lib/utils'
import { RefreshCw, Search, BookOpen, BarChart3, Wallet } from 'lucide-react'
import type { LedgerEntry, DailyReport, AccountBalance } from '../models/ledger'

type Tab = 'live' | 'report' | 'balance'

function EntryBadge({ type }: { type: string }) {
  return <Badge variant={type === 'CR' ? 'success' : 'warning'} label={type === 'CR' ? 'CR' : 'DR'} />
}

export default function AdminLedgerPage() {
  const { user, logout } = useAuth()
  const [tab, setTab] = useState<Tab>('live')

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <header className="bg-white border-b border-gray-200 px-4 sm:px-8 py-4 flex items-center gap-4 sticky top-0 z-40">
        <BookOpen className="w-6 h-6 text-blue-600" />
        <h1 className="text-lg font-bold text-gray-900 mr-auto">Libro Mayor</h1>
        <a href="/admin" className="text-sm text-blue-600 hover:text-blue-700 font-medium">Usuarios</a>
        <span className="text-sm text-gray-500">{user?.firstName} {user?.lastName}</span>
        <Button variant="ghost" onClick={logout}>Salir</Button>
      </header>

      <div className="border-b border-gray-200 bg-white">
        <div className="flex px-4 sm:px-8 max-w-6xl mx-auto">
          {([{ id: 'live', label: 'En Vivo', icon: RefreshCw },
             { id: 'report', label: 'Reporte Diario', icon: BarChart3 },
             { id: 'balance', label: 'Balance por Cuenta', icon: Wallet }] as const).map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setTab(id as Tab)}
              className={`inline-flex items-center gap-2 px-5 py-3 text-sm font-medium border-b-2 transition-colors ${
                tab === id ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <Icon className="w-4 h-4" />
              {label}
            </button>
          ))}
        </div>
      </div>

      <main className="p-4 sm:p-8 max-w-6xl mx-auto w-full">
        {tab === 'live' && <LiveStream />}
        {tab === 'report' && <DailyReportSection />}
        {tab === 'balance' && <AccountBalanceSection />}
      </main>
    </div>
  )
}

function LiveStream() {
  const [entries, setEntries] = useState<LedgerEntry[]>([])
  const [initialLoading, setInitialLoading] = useState(true)
  const [connected, setConnected] = useState(false)
  const es = useRef<EventSource | null>(null)
  const entryIds = useRef(new Set<number>())

  const connect = useCallback(() => {
    const token = localStorage.getItem('accessToken')
    es.current = new EventSource(`/api/ledger/stream?token=${token}`)

    es.current.addEventListener('ledger-entry', (e) => {
      const entry: LedgerEntry = JSON.parse(e.data)
      if (!entryIds.current.has(entry.id)) {
        entryIds.current.add(entry.id)
        setEntries((prev) => [entry, ...prev].slice(0, 200))
      }
    })

    es.current.onopen = () => setConnected(true)
    es.current.onerror = () => setConnected(false)
  }, [])

  useEffect(() => {
    setInitialLoading(true)
    ledgerApi.list()
      .then((res) => {
        const data = res.data.data ?? []
        data.forEach((e) => entryIds.current.add(e.id))
        setEntries(data.reverse())
      })
      .catch(() => toast.error('Error al cargar asientos contables'))
      .finally(() => setInitialLoading(false))

    connect()
    return () => es.current?.close()
  }, [connect])

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <h2 className="text-lg font-semibold text-gray-800">Asientos en tiempo real</h2>
        <span className={`inline-block w-2.5 h-2.5 rounded-full ${connected ? 'bg-emerald-500' : 'bg-red-500'}`} />
        <span className="text-xs text-gray-400">{connected ? 'Conectado' : 'Desconectado'}</span>
        <span className="text-xs text-gray-400 ml-auto">{entries.length} asientos</span>
      </div>

      {initialLoading ? (
        <Spinner text="Cargando asientos..." />
      ) : (
        <Card padding="sm" className="max-h-[600px] overflow-y-auto">
          {entries.length === 0 ? (
            <div className="p-8 text-center text-gray-400">Esperando asientos contables...</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="sticky top-0 bg-gray-50/95 backdrop-blur-sm">
                <tr className="text-left text-gray-400 border-b border-gray-100">
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">Cuenta</th>
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">Tipo</th>
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider text-right">Monto</th>
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">Moneda</th>
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">Transferencia</th>
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">Fecha</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((e) => (
                  <tr key={e.id} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                    <td className="px-4 py-3 font-medium text-gray-800 font-mono text-xs">{e.accountNumber}</td>
                    <td className="px-4 py-3"><EntryBadge type={e.entryType} /></td>
                    <td className={`px-4 py-3 text-right font-mono text-sm ${e.entryType === 'DR' ? 'text-red-600' : 'text-emerald-600'}`}>
                      {e.entryType === 'DR' ? '-' : '+'}{formatCurrency(Math.abs(e.amount), e.currency)}
                    </td>
                    <td className="px-4 py-3 text-gray-500">{e.currency}</td>
                    <td className="px-4 py-3 text-gray-500 font-mono text-xs">{e.transferId ? truncate(e.transferId, 12) : '-'}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{formatDate(e.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      )}
    </div>
  )
}

function DailyReportSection() {
  const today = new Date().toISOString().slice(0, 10)
  const [date, setDate] = useState(today)
  const [report, setReport] = useState<DailyReport | null>(null)
  const [loading, setLoading] = useState(false)

  const load = () => {
    setLoading(true)
    ledgerApi.dailyReport(date)
      .then((res) => setReport(res.data.data ?? null))
      .catch(() => toast.error('Error al cargar reporte diario'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [date])

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <h2 className="text-lg font-semibold text-gray-800">Reporte Diario</h2>
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
        />
      </div>

      {loading ? (
        <Spinner text="Cargando reporte..." />
      ) : !report ? (
        <Card className="text-center py-8">
          <BarChart3 className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-gray-400">Sin datos para esta fecha</p>
        </Card>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-3 gap-4">
            {[
              { label: 'Cuentas', value: report.totalAccounts, color: 'text-blue-600' },
              { label: 'Asientos', value: report.totalEntries, color: 'text-emerald-600' },
              { label: 'Fecha', value: report.date, color: 'text-gray-600' },
            ].map((s) => (
              <Card key={s.label} padding="sm">
                <span className="text-xs text-gray-400 uppercase tracking-wide block mb-1">{s.label}</span>
                <span className={`text-xl font-bold ${s.color}`}>{s.value}</span>
              </Card>
            ))}
          </div>

          <Card padding="sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-400 border-b border-gray-100 bg-gray-50/80">
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">Cuenta</th>
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider text-right">Saldo Inicial</th>
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider text-right">Débitos</th>
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider text-right">Créditos</th>
                  <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider text-right">Saldo Final</th>
                </tr>
              </thead>
              <tbody>
                {report.accounts.map((a) => (
                  <tr key={a.accountNumber} className="border-b border-gray-50 hover:bg-gray-50/50">
                    <td className="px-4 py-3 font-medium text-gray-800 font-mono text-xs">{a.accountNumber}</td>
                    <td className="px-4 py-3 text-right font-mono">{formatCurrency(a.openingBalance, a.currency)}</td>
                    <td className="px-4 py-3 text-right font-mono text-red-600">{formatCurrency(a.totalDebits, a.currency)}</td>
                    <td className="px-4 py-3 text-right font-mono text-emerald-600">{formatCurrency(a.totalCredits, a.currency)}</td>
                    <td className="px-4 py-3 text-right font-mono font-semibold">{formatCurrency(a.closingBalance, a.currency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        </div>
      )}
    </div>
  )
}

function AccountBalanceSection() {
  const [accountNumber, setAccountNumber] = useState('')
  const [balance, setBalance] = useState<AccountBalance | null>(null)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)

  const handleSearch = async () => {
    if (!accountNumber.trim()) return
    setLoading(true)
    setSearched(true)
    try {
      const res = await ledgerApi.balance(accountNumber.trim())
      setBalance(res.data.data ?? null)
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        setBalance(null)
      } else {
        toast.error('Error al consultar balance')
      }
    } finally { setLoading(false) }
  }

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold text-gray-800">Balance por Cuenta</h2>

      <div className="flex gap-2">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            value={accountNumber}
            onChange={(e) => setAccountNumber(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            placeholder="Ingrese número de cuenta..."
            className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
        </div>
        <Button onClick={handleSearch}>Consultar</Button>
      </div>

      {loading && <Spinner text="Consultando balance..." />}

      {!loading && searched && !balance && (
        <Card className="text-center py-8">
          <Wallet className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-gray-400">No se encontraron asientos para esta cuenta</p>
        </Card>
      )}

      {!loading && balance && (
        <Card className="max-w-md">
          <div className="grid grid-cols-2 gap-4">
            {[
              { label: 'Cuenta', value: balance.accountNumber, span: false },
              { label: 'Moneda', value: balance.currency, span: false },
              { label: 'Balance', value: formatCurrency(balance.balance, balance.currency), span: true, big: true },
              { label: 'Consultado', value: formatDate(balance.calculatedAt), span: true },
            ].map((s) => (
              <div key={s.label} className={s.span ? 'col-span-2' : ''}>
                <span className="text-xs text-gray-400 uppercase tracking-wide block">{s.label}</span>
                <span className={`${s.big ? 'text-2xl' : 'text-base'} font-bold text-gray-800`}>{s.value}</span>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  )
}
