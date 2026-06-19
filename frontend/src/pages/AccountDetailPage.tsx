import { useState, useEffect, type FormEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { accountsApi } from '../api/accounts-api'
import { cardsApi } from '../api/cards-api'
import { transfersApi, type TransferResponse } from '../api/transfers-api'
import AppLayout from '../components/layout/AppLayout'
import Modal from '../components/ui/Modal'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import Spinner from '../components/ui/Spinner'
import type { Account, Movement } from '../models/account'
import type { Card, CardAccountLink } from '../models/card'

const currencySymbol: Record<string, string> = { USD: '$', PEN: 'S/', EUR: '\u20AC' }
const currencies = ['USD', 'PEN', 'EUR']
type Tab = 'transferencias' | 'movimientos'

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = { ACTIVE: 'bg-emerald-100 text-emerald-700', INACTIVE: 'bg-gray-100 text-gray-500', BLOCKED: 'bg-red-100 text-red-700' }
  return <span className={`text-xs font-medium px-2.5 py-0.5 rounded-full ${colors[status] ?? 'bg-gray-100 text-gray-600'}`}>{status}</span>
}

function TransferStatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    PENDING: 'bg-yellow-100 text-yellow-700',
    DEBITED: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-emerald-100 text-emerald-700',
    FAILED: 'bg-red-100 text-red-700',
    REJECTED: 'bg-orange-100 text-orange-700',
  }
  const labels: Record<string, string> = {
    PENDING: 'Pendiente',
    DEBITED: 'Procesando',
    COMPLETED: 'Completada',
    FAILED: 'Fallida',
    REJECTED: 'Rechazada',
  }
  return <span className={`text-xs font-medium px-2.5 py-0.5 rounded-full ${colors[status] ?? 'bg-gray-100 text-gray-600'}`}>{labels[status] ?? status}</span>
}

export default function AccountDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [account, setAccount] = useState<Account | null>(null)
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState<Tab>('transferencias')

  const [transfers, setTransfers] = useState<TransferResponse[]>([])
  const [transfersLoading, setTransfersLoading] = useState(false)
  const [movements, setMovements] = useState<Movement[]>([])
  const [movementsLoading, setMovementsLoading] = useState(false)

  const [cards, setCards] = useState<Card[]>([])
  const [showDeposit, setShowDeposit] = useState(false)
  const [depositForm, setDepositForm] = useState({ amount: 0, selectedCardId: '', selectedAccountNumber: '', pin4: '' })
  const [depositCardAccounts, setDepositCardAccounts] = useState<CardAccountLink[]>([])
  const [showTransfer, setShowTransfer] = useState(false)
  const [transferForm, setTransferForm] = useState({ toAccount: '', amount: 0, description: '' })
  const [busy, setBusy] = useState(false)
  const [showBlock, setShowBlock] = useState(false)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    Promise.all([
      accountsApi.getById(id),
      cardsApi.list(),
    ]).then(([aRes, cRes]) => {
      setAccount(aRes.data.data ?? null)
      setCards(cRes.data.data ?? [])
    }).catch(() => {
      toast.error('Error al cargar datos')
      navigate('/dashboard')
    }).finally(() => setLoading(false))
  }, [id, navigate])

  useEffect(() => {
    if (!account) return
    setTransfersLoading(true)
    transfersApi.byAccount(account.accountNumber).then((res) => {
      setTransfers(res.data.data?.results ?? [])
    }).catch(() => toast.error('Error al cargar transferencias'))
    .finally(() => setTransfersLoading(false))
  }, [account])

  useEffect(() => {
    if (!account || tab !== 'movimientos') return
    setMovementsLoading(true)
    accountsApi.movements(account.id).then((res) => {
      setMovements(res.data.data ?? [])
    }).catch(() => toast.error('Error al cargar movimientos'))
    .finally(() => setMovementsLoading(false))
  }, [account, tab])

  // Load linked accounts when card is selected for deposit
  useEffect(() => {
    if (!depositForm.selectedCardId) { setDepositCardAccounts([]); return }
    cardsApi.getById(depositForm.selectedCardId).then((res) => {
      setDepositCardAccounts(res.data.data?.accounts ?? [])
    }).catch(() => toast.error('Error al cargar cuentas de la tarjeta'))
  }, [depositForm.selectedCardId])

  const handleDeposit = async (e: FormEvent) => {
    e.preventDefault()
    if (!account) return
    setBusy(true)
    try {
      if (depositForm.selectedCardId && depositForm.selectedAccountNumber) {
        await cardsApi.deposit(depositForm.selectedCardId, depositForm.selectedAccountNumber, depositForm.amount, depositForm.pin4)
        toast.success('Depósito con tarjeta exitoso')
      } else {
        const res = await accountsApi.deposit(account.accountNumber, account.currency, depositForm.amount)
        toast.success(`Depósito exitoso — Saldo: ${currencySymbol[account.currency] ?? ''}${res.data.data?.balanceAfter.toLocaleString()}`)
      }
      setShowDeposit(false)
      setDepositForm({ amount: 0, selectedCardId: '', selectedAccountNumber: '', pin4: '' })
      accountsApi.getById(account.id).then((r) => setAccount(r.data.data))
    } catch {
      toast.error('Error al depositar')
    } finally {
      setBusy(false)
    }
  }

  const handleTransfer = async (e: FormEvent) => {
    e.preventDefault()
    if (!account) return
    setBusy(true)
    try {
      const res = await transfersApi.internal({
        fromAccount: account.accountNumber,
        toAccount: transferForm.toAccount,
        amount: transferForm.amount,
        currency: account.currency,
        description: transferForm.description || undefined,
      })
      const data = res.data.data
      if (data) {
        setTransfers((prev) => [data, ...prev])
        toast.success('Transferencia iniciada')
        setShowTransfer(false)
      }
    } catch {
      toast.error('Error al iniciar la transferencia')
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <AppLayout><Spinner text="Cargando cuenta..." /></AppLayout>
  if (!account) return null

  return (
    <AppLayout>
      <button onClick={() => navigate('/dashboard')} className="text-sm text-blue-600 hover:text-blue-800 mb-4">&larr; Volver al dashboard</button>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-6">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs text-gray-400 font-mono">{account.accountNumber}</p>
            <p className="text-xs text-gray-400 mt-0.5">{account.currency}</p>
          </div>
          <StatusBadge status={account.status} />
        </div>
        <p className="text-3xl font-bold text-gray-800 mt-3">
          {currencySymbol[account.currency] ?? ''}{account.balance.toLocaleString(undefined, { minimumFractionDigits: 2 })}
        </p>
        <div className="flex gap-2 mt-4">
          {account.status === 'ACTIVE' && (
            <>
              <Button onClick={() => setShowTransfer(true)}>Transferir</Button>
              <Button onClick={() => setShowDeposit(true)} variant="ghost">Depositar</Button>
              <Button onClick={() => setShowBlock(true)} className="bg-red-600 hover:bg-red-700">Bloquear</Button>
            </>
          )}
          {account.status === 'BLOCKED' && (
            <span className="text-xs text-red-500 font-medium">Cuenta bloqueada</span>
          )}
        </div>
      </div>

      <div className="flex gap-1 mb-4">
        <button
          onClick={() => setTab('transferencias')}
          className={`px-4 py-2 text-sm font-medium rounded-t-lg border-b-2 transition-colors ${tab === 'transferencias' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-400 hover:text-gray-600'}`}
        >
          Transferencias
        </button>
        <button
          onClick={() => setTab('movimientos')}
          className={`px-4 py-2 text-sm font-medium rounded-t-lg border-b-2 transition-colors ${tab === 'movimientos' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-400 hover:text-gray-600'}`}
        >
          Movimientos
        </button>
      </div>

      {tab === 'transferencias' && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          {transfersLoading ? (
            <Spinner text="Cargando transferencias..." />
          ) : transfers.length === 0 ? (
            <p className="text-center text-gray-400 py-8">Sin transferencias</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-400 border-b border-gray-100 bg-gray-50">
                    <th className="px-6 py-3 font-medium">ID</th>
                    <th className="px-6 py-3 font-medium">Destino</th>
                    <th className="px-6 py-3 font-medium text-right">Monto</th>
                    <th className="px-6 py-3 font-medium">Estado</th>
                    <th className="px-6 py-3 font-medium text-right">Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  {transfers.map((t) => (
                    <tr key={t.transferId} className="border-b border-gray-50">
                      <td className="px-6 py-3 text-gray-500 font-mono text-xs">{t.transferId.slice(0, 8)}...</td>
                      <td className="px-6 py-3 text-gray-800">{t.toAccount}</td>
                      <td className="px-6 py-3 text-right font-medium">
                        {currencySymbol[t.currency] ?? ''}{t.amount.toLocaleString()}
                      </td>
                      <td className="px-6 py-3"><TransferStatusBadge status={t.status} /></td>
                      <td className="px-6 py-3 text-right text-gray-400 text-xs">{new Date(t.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {tab === 'movimientos' && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          {movementsLoading ? (
            <Spinner text="Cargando movimientos..." />
          ) : movements.length === 0 ? (
            <p className="text-center text-gray-400 py-8">Sin movimientos</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-400 border-b border-gray-100 bg-gray-50">
                    <th className="px-6 py-3 font-medium">#</th>
                    <th className="px-6 py-3 font-medium">Tipo</th>
                    <th className="px-6 py-3 font-medium text-right">Monto</th>
                    <th className="px-6 py-3 font-medium text-right">Saldo</th>
                    <th className="px-6 py-3 font-medium text-right">Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  {movements.map((m) => (
                    <tr key={m.id} className="border-b border-gray-50 last:border-0">
                      <td className="px-6 py-3 text-gray-500 font-mono text-xs">{m.movementNumber}</td>
                      <td className="px-6 py-3">
                        <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${m.type === 'CREDIT' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
                          {m.type === 'CREDIT' ? 'Ingreso' : 'Salida'}
                        </span>
                      </td>
                      <td className={`px-6 py-3 text-right font-medium ${m.type === 'CREDIT' ? 'text-emerald-600' : 'text-red-600'}`}>
                        {m.type === 'CREDIT' ? '+' : '-'}{m.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </td>
                      <td className="px-6 py-3 text-right text-gray-600">{m.balanceAfter.toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
                      <td className="px-6 py-3 text-right text-gray-400 text-xs">{new Date(m.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      <Modal open={showDeposit} onClose={() => setShowDeposit(false)} title={`Depositar - ${account.accountNumber}`}>
        <form onSubmit={handleDeposit} className="flex flex-col gap-4">
          <p className="text-sm text-gray-500">{account.currency} — Saldo actual: {currencySymbol[account.currency] ?? ''}{account.balance.toLocaleString()}</p>

          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Tarjeta (opcional)</label>
            <select value={depositForm.selectedCardId} onChange={(e) => setDepositForm((f) => ({ ...f, selectedCardId: e.target.value, selectedAccountNumber: '', pin4: '' }))}
              className="w-full px-3 py-2.5 border border-gray-300 rounded text-base outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200">
              <option value="">Sin tarjeta (depósito directo)</option>
              {cards.filter((c) => c.status === 'ACTIVE').map((c) => (
                <option key={c.id} value={c.id}>{c.pan} — Exp: {c.expiryDate}</option>
              ))}
            </select>
          </div>

          {depositForm.selectedCardId && depositCardAccounts.length > 0 && (
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-500">Cuenta a depositar (vinculada a la tarjeta)</label>
              <select value={depositForm.selectedAccountNumber} onChange={(e) => setDepositForm((f) => ({ ...f, selectedAccountNumber: e.target.value }))}
                className="w-full px-3 py-2.5 border border-gray-300 rounded text-base outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200" required>
                <option value="">Seleccionar cuenta...</option>
                {depositCardAccounts.map((link) => (
                  <option key={link.accountNumber} value={link.accountNumber}>{link.accountNumber} ({link.currency}){link.isPrimary ? ' (Principal)' : ''}</option>
                ))}
              </select>
            </div>
          )}

          <Input label="Monto" type="number" step="0.01" min="0.01" value={depositForm.amount || ''} onChange={(e) => setDepositForm((f) => ({ ...f, amount: parseFloat(e.target.value) || 0 }))} required />

          {depositForm.selectedCardId && (
            <Input label="PIN de la tarjeta" type="password" maxLength={4} value={depositForm.pin4} onChange={(e) => setDepositForm((f) => ({ ...f, pin4: e.target.value }))} required />
          )}

          {!depositForm.selectedCardId && (
            <p className="text-xs text-gray-400">Depósito directo — no requiere PIN</p>
          )}

          <Button type="submit" disabled={busy || (!!depositForm.selectedCardId && !depositForm.selectedAccountNumber)}>
            {busy ? 'Depositando...' : depositForm.selectedCardId ? 'Depositar con tarjeta' : 'Depositar'}
          </Button>
        </form>
      </Modal>

      <Modal open={showTransfer} onClose={() => setShowTransfer(false)} title={`Transferir - ${account.accountNumber}`}>
        <form onSubmit={handleTransfer} className="flex flex-col gap-4">
          <p className="text-sm text-gray-500">Desde: {account.accountNumber} ({account.currency}) — Saldo: {currencySymbol[account.currency] ?? ''}{account.balance.toLocaleString()}</p>
          <Input label="Cuenta destino (número)" value={transferForm.toAccount} onChange={(e) => setTransferForm((f) => ({ ...f, toAccount: e.target.value }))} required />
          <Input label="Monto" type="number" step="0.01" min="0.01" value={transferForm.amount || ''} onChange={(e) => setTransferForm((f) => ({ ...f, amount: parseFloat(e.target.value) || 0 }))} required />
          <Input label="Descripción (opcional)" value={transferForm.description} onChange={(e) => setTransferForm((f) => ({ ...f, description: e.target.value }))} />
          <Button type="submit" disabled={busy}>{busy ? 'Transfiriendo...' : 'Transferir'}</Button>
        </form>
      </Modal>

      <Modal open={showBlock} onClose={() => setShowBlock(false)} title="Bloquear cuenta">
        <div className="flex flex-col gap-4">
          <p className="text-sm text-gray-600">¿Estás seguro de bloquear la cuenta <strong>{account.accountNumber}</strong>?</p>
          <p className="text-xs text-gray-400">Una vez bloqueada, no podrás realizar transferencias ni depósitos desde esta cuenta.</p>
          <div className="flex gap-2 justify-end">
            <Button variant="ghost" onClick={() => setShowBlock(false)}>Cancelar</Button>
            <Button onClick={async () => {
              setBusy(true)
              try {
                const res = await accountsApi.block(account.id)
                setAccount((prev) => prev ? { ...prev, status: res.data.data?.status ?? 'BLOCKED' } : prev)
                toast.success('Cuenta bloqueada correctamente')
                setShowBlock(false)
              } catch { toast.error('Error al bloquear cuenta') }
              finally { setBusy(false) }
            }} disabled={busy} className="bg-red-600 hover:bg-red-700">{busy ? 'Bloqueando...' : 'Bloquear cuenta'}</Button>
          </div>
        </div>
      </Modal>
    </AppLayout>
  )
}
