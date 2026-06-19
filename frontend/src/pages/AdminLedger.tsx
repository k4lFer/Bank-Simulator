import { useState, useEffect, useRef, useCallback } from 'react'
import { toast } from 'sonner'
import axios from 'axios'
import { ledgerApi } from '../api/ledger-api'
import { useAuth } from '../hooks/useAuth'
import Button from '../components/ui/Button'
import Spinner from '../components/ui/Spinner'
import type { LedgerEntry, DailyReport, AccountBalance } from '../models/ledger'

type Tab = 'live' | 'report' | 'balance'

function EntryBadge({ type }: { type: string }) {
  return (
    <span className={`text-xs font-bold px-2 py-0.5 rounded ${type === 'CR' ? 'bg-emerald-100 text-emerald-700' : 'bg-orange-100 text-orange-700'}`}>
      {type === 'CR' ? 'CR' : 'DR'}
    </span>
  )
}

function AmountCell({ amount, type }: { amount: number; type: string }) {
  const isNegative = type === 'DR'
  return <span className={isNegative ? 'text-red-600' : 'text-emerald-600'}>{isNegative ? '-' : '+'}${Math.abs(amount).toLocaleString()}</span>
}

export default function AdminLedgerPage() {
  const { user, logout } = useAuth()
  const [tab, setTab] = useState<Tab>('live')

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <header className="bg-gray-800 text-white px-8 py-4 flex items-center gap-4">
        <h1 className="text-lg font-semibold mr-auto">Bank Simulator — Libro Mayor</h1>
        <a href="/admin" className="text-sm underline opacity-80 hover:opacity-100">Usuarios</a>
        <span className="text-sm opacity-90">{user?.firstName} {user?.lastName}</span>
        <Button variant="ghost" onClick={logout}>Salir</Button>
      </header>

      <div className="border-b border-gray-200 bg-white">
        <div className="flex px-8 max-w-6xl mx-auto">
          {(['live', 'report', 'balance'] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`px-5 py-3 text-sm font-medium border-b-2 transition-colors ${
                tab === t ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {t === 'live' ? 'En Vivo' : t === 'report' ? 'Reporte Diario' : 'Balance por Cuenta'}
            </button>
          ))}
        </div>
      </div>

      <main className="p-8 max-w-6xl mx-auto w-full">
        {tab === 'live' && <LiveStream />}
        {tab === 'report' && <DailyReportSection />}
        {tab === 'balance' && <AccountBalanceSection />}
      </main>
    </div>
  )
}

/* ───── Live Stream ───── */
function LiveStream() {
  const [entries, setEntries] = useState<LedgerEntry[]>([])
  const [connected, setConnected] = useState(false)
  const es = useRef<EventSource | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  const connect = useCallback(() => {
    const token = localStorage.getItem('accessToken')
    es.current = new EventSource(`/api/ledger/stream?token=${token}`)

    es.current.addEventListener('ledger-entry', (e) => {
      const entry: LedgerEntry = JSON.parse(e.data)
      setEntries((prev) => [entry, ...prev].slice(0, 200))
    })

    es.current.onopen = () => setConnected(true)
    es.current.onerror = () => setConnected(false)
  }, [])

  useEffect(() => {
    connect()
    return () => es.current?.close()
  }, [connect])

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <h2 className="text-lg font-semibold text-gray-800">Asientos en tiempo real</h2>
        <span className={`inline-block w-2.5 h-2.5 rounded-full ${connected ? 'bg-emerald-500' : 'bg-red-500'}`} />
        <span className="text-xs text-gray-400">{connected ? 'Conectado' : 'Desconectado'}</span>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden max-h-[600px] overflow-y-auto">
        {entries.length === 0 ? (
          <div className="p-8 text-center text-gray-400">Esperando asientos contables...</div>
        ) : (
          <table className="w-full text-sm">
            <thead className="sticky top-0 bg-gray-50">
              <tr className="text-left text-gray-400 border-b border-gray-100">
                <th className="px-4 py-3 font-medium">Cuenta</th>
                <th className="px-4 py-3 font-medium">Tipo</th>
                <th className="px-4 py-3 font-medium text-right">Monto</th>
                <th className="px-4 py-3 font-medium">Moneda</th>
                <th className="px-4 py-3 font-medium">Transferencia</th>
                <th className="px-4 py-3 font-medium">Fecha</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((e) => (
                <tr key={e.id} className="border-b border-gray-50 hover:bg-gray-50/50">
                  <td className="px-4 py-3 font-medium text-gray-800">{e.accountNumber}</td>
                  <td className="px-4 py-3"><EntryBadge type={e.entryType} /></td>
                  <td className="px-4 py-3 text-right"><AmountCell amount={e.amount} type={e.entryType} /></td>
                  <td className="px-4 py-3 text-gray-500">{e.currency}</td>
                  <td className="px-4 py-3 text-gray-500 font-mono text-xs">{e.transferId ? e.transferId.slice(0, 8) + '...' : '-'}</td>
                  <td className="px-4 py-3 text-gray-500">{new Date(e.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

/* ───── Daily Report ───── */
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
          className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm"
        />
      </div>

      {loading ? (
        <Spinner text="Cargando reporte..." />
      ) : !report ? (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-8 text-center text-gray-400">Sin datos para esta fecha</div>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-3 gap-4">
            {[
              { label: 'Total cuentas', value: report.totalAccounts },
              { label: 'Total asientos', value: report.totalEntries },
              { label: 'Fecha', value: report.date },
            ].map((s) => (
              <div key={s.label} className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 flex flex-col gap-1">
                <span className="text-xs text-gray-400 uppercase tracking-wide">{s.label}</span>
                <span className="text-xl font-bold text-gray-800">{s.value}</span>
              </div>
            ))}
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-400 border-b border-gray-100 bg-gray-50">
                  <th className="px-4 py-3 font-medium">Cuenta</th>
                  <th className="px-4 py-3 font-medium text-right">Saldo Inicial</th>
                  <th className="px-4 py-3 font-medium text-right">Débitos</th>
                  <th className="px-4 py-3 font-medium text-right">Créditos</th>
                  <th className="px-4 py-3 font-medium text-right">Saldo Final</th>
                </tr>
              </thead>
              <tbody>
                {report.accounts.map((a) => (
                  <tr key={a.accountNumber} className="border-b border-gray-50 hover:bg-gray-50/50">
                    <td className="px-4 py-3 font-medium text-gray-800">{a.accountNumber}</td>
                    <td className="px-4 py-3 text-right font-mono">${a.openingBalance.toLocaleString()}</td>
                    <td className="px-4 py-3 text-right font-mono text-red-600">${a.totalDebits.toLocaleString()}</td>
                    <td className="px-4 py-3 text-right font-mono text-emerald-600">${a.totalCredits.toLocaleString()}</td>
                    <td className="px-4 py-3 text-right font-mono font-semibold">{a.closingBalance < 0 ? '-' : ''}${Math.abs(a.closingBalance).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

/* ───── Account Balance ───── */
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
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold text-gray-800">Balance por Cuenta</h2>

      <div className="flex gap-2">
        <input
          type="text"
          value={accountNumber}
          onChange={(e) => setAccountNumber(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          placeholder="Ingrese número de cuenta"
          className="border border-gray-300 rounded-lg px-4 py-2 text-sm flex-1 max-w-sm"
        />
        <Button onClick={handleSearch}>Consultar</Button>
      </div>

      {loading && <Spinner text="Consultando balance..." />}

      {!loading && searched && !balance && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-8 text-center text-gray-400">
          No se encontraron asientos para esta cuenta
        </div>
      )}

      {!loading && balance && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 max-w-md">
          <div className="grid grid-cols-2 gap-4">
            {[
              { label: 'Cuenta', value: balance.accountNumber },
              { label: 'Moneda', value: balance.currency },
              { label: 'Balance', value: `$${balance.balance.toLocaleString()}`, big: true },
              { label: 'Consultado', value: new Date(balance.calculatedAt).toLocaleString() },
            ].map((s) => (
              <div key={s.label} className={s.big ? 'col-span-2' : ''}>
                <span className="text-xs text-gray-400 uppercase tracking-wide block">{s.label}</span>
                <span className={`${s.big ? 'text-2xl' : 'text-base'} font-bold text-gray-800`}>{s.value}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
