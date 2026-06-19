import { useState, useEffect, useRef, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { useAuth } from '../hooks/useAuth'
import { accountsApi } from '../api/accounts-api'
import { cardsApi } from '../api/cards-api'
import { transfersApi, type TransferResponse } from '../api/transfers-api'
import AppLayout from '../components/layout/AppLayout'
import Modal from '../components/ui/Modal'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import Spinner from '../components/ui/Spinner'
import type { Account } from '../models/account'
import type { Card, CardAccountLink } from '../models/card'

const currencySymbol: Record<string, string> = { USD: '$', PEN: 'S/', EUR: '\u20AC' }
const currencies = ['USD', 'PEN', 'EUR']

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

function AccountCard({ account, onView, onInternalTransfer, onExternalTransfer, onDeposit }: { account: Account; onView: (id: string) => void; onInternalTransfer: (a: Account) => void; onExternalTransfer: (a: Account) => void; onDeposit: (a: Account) => void }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs text-gray-400 font-mono">{account.accountNumber}</p>
          <p className="text-xs text-gray-400 mt-0.5">{account.currency}</p>
        </div>
        <StatusBadge status={account.status} />
      </div>
      <p className="text-2xl font-bold text-gray-800">
        {currencySymbol[account.currency] ?? ''}{account.balance.toLocaleString(undefined, { minimumFractionDigits: 2 })}
      </p>
      <div className="flex gap-2 mt-1 flex-wrap">
        <button onClick={() => onView(account.id)} className="text-sm text-blue-600 hover:text-blue-800 font-medium">
          Movimientos &rarr;
        </button>
        {account.status === 'ACTIVE' && (
          <>
            <button onClick={() => onDeposit(account)} className="text-sm text-emerald-600 hover:text-emerald-800 font-medium ml-auto">
              Depositar
            </button>
            <button onClick={() => onInternalTransfer(account)} className="text-sm text-purple-600 hover:text-purple-800 font-medium">
              Entre cuentas
            </button>
            <button onClick={() => onExternalTransfer(account)} className="text-sm text-orange-600 hover:text-orange-800 font-medium">
              Transferir
            </button>
          </>
        )}
      </div>
    </div>
  )
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const { user, token } = useAuth()
  const [accounts, setAccounts] = useState<Account[]>([])
  const [cards, setCards] = useState<Card[]>([])
  const [loading, setLoading] = useState(true)

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState({ currency: 'USD', selectedCardId: '' })

  // Deposit — with optional card
  const [showDeposit, setShowDeposit] = useState<Account | null>(null)
  const [depositForm, setDepositForm] = useState({ amount: 0, selectedCardId: '', selectedAccountNumber: '', pin4: '' })
  const [depositCardAccounts, setDepositCardAccounts] = useState<CardAccountLink[]>([])

  // Internal transfer
  const [showInternal, setShowInternal] = useState<Account | null>(null)
  const [internalForm, setInternalForm] = useState({ toAccount: '', amount: 0, description: '' })
  const [internalStatus, setInternalStatus] = useState<{ transferId: string; status: string; error?: string } | null>(null)

  // External/card transfer
  const [showExternal, setShowExternal] = useState<Account | null>(null)
  const [externalForm, setExternalForm] = useState({ selectedCardId: '', fromAccount: '', toAccount: '', amount: 0, description: '', pin4: '' })
  const [externalCardAccounts, setExternalCardAccounts] = useState<CardAccountLink[]>([])
  const [externalStatus, setExternalStatus] = useState<{ transferId: string; status: string; error?: string } | null>(null)

  const [busy, setBusy] = useState(false)
  const [showIssueCard, setShowIssueCard] = useState(false)
  const [issueCardForm, setIssueCardForm] = useState({ pin4: '', dailyLimit: 1000 })

  // Change PIN
  const [showChangePinCard, setShowChangePinCard] = useState<Card | null>(null)
  const [changePinForm, setChangePinForm] = useState({ currentPin4: '', newPin4: '' })

  // Block card
  const [showBlockCard, setShowBlockCard] = useState<Card | null>(null)

  const [recentTransfers, setRecentTransfers] = useState<TransferResponse[]>([])
  const pendingTransferIds = useRef<Set<string>>(new Set())

  // Single SSE connection for transfer status updates
  useEffect(() => {
    if (!token) return
    const es = new EventSource(`/api/transfers/stream?token=${encodeURIComponent(token)}`)
    es.addEventListener('transfer-status', (e) => {
      const t: TransferResponse = JSON.parse(e.data)
      if (!pendingTransferIds.current.has(t.transferId)) return

      // Update recent transfers list
      setRecentTransfers((prev) => prev.map((p) => p.transferId === t.transferId ? t : p))

      if (t.status === 'COMPLETED') {
        pendingTransferIds.current.delete(t.transferId)
        setInternalStatus(null)
        setExternalStatus(null)
        toast.success('Transferencia completada')
        loadAccounts()
      } else if (t.status === 'REJECTED') {
        pendingTransferIds.current.delete(t.transferId)
        const reason = t.rejectionReason ?? 'Rechazada'
        toast.error(`Transferencia rechazada: ${reason}`)
        setInternalStatus(null)
        setExternalStatus(null)
        loadAccounts()
      } else if (t.status === 'DEBITED') {
        setInternalStatus((s) => s?.transferId === t.transferId ? { ...s, status: 'DEBITED' } : s)
        setExternalStatus((s) => s?.transferId === t.transferId ? { ...s, status: 'DEBITED' } : s)
      }
    })
    return () => es.close()
  }, [])

  const loadAccounts = () => {
    Promise.all([
      accountsApi.listFromAccounts(),
      cardsApi.list(),
    ])
      .then(([aRes, cRes]) => {
        setAccounts(aRes.data.data ?? [])
        setCards(cRes.data.data ?? [])
      })
      .catch(() => toast.error('Error al cargar datos'))
      .finally(() => setLoading(false))
  }

  useEffect(loadAccounts, [])

  // Load linked accounts when a card is selected for deposit
  useEffect(() => {
    if (!depositForm.selectedCardId) { setDepositCardAccounts([]); return }
    cardsApi.getById(depositForm.selectedCardId).then((res) => {
      setDepositCardAccounts(res.data.data?.accounts ?? [])
    }).catch(() => toast.error('Error al cargar cuentas de la tarjeta'))
  }, [depositForm.selectedCardId])

  // Load linked accounts when a card is selected for external transfer
  useEffect(() => {
    if (!externalForm.selectedCardId) { setExternalCardAccounts([]); return }
    cardsApi.getById(externalForm.selectedCardId).then((res) => {
      setExternalCardAccounts(res.data.data?.accounts ?? [])
    }).catch(() => toast.error('Error al cargar cuentas de la tarjeta'))
  }, [externalForm.selectedCardId])

  const handleCreateAccount = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    try {
      await accountsApi.create(createForm.currency, createForm.selectedCardId)
      toast.success('Cuenta creada correctamente')
      setShowCreate(false)
      setCreateForm({ currency: 'USD', selectedCardId: '' })
      loadAccounts()
    } catch { toast.error('Error al crear cuenta') }
    finally { setBusy(false) }
  }

  const handleDeposit = async (e: FormEvent) => {
    e.preventDefault()
    if (!showDeposit) return
    setBusy(true)
    try {
      if (depositForm.selectedCardId && depositForm.selectedAccountNumber) {
        // Card deposit
        const res = await cardsApi.deposit(depositForm.selectedCardId, depositForm.selectedAccountNumber, depositForm.amount, depositForm.pin4)
        toast.success(`Depósito con tarjeta exitoso`)
      } else {
        // Direct deposit (no card)
        const res = await accountsApi.deposit(showDeposit.accountNumber, showDeposit.currency, depositForm.amount)
        const data = res.data.data
        toast.success(`Depósito exitoso — Saldo: ${currencySymbol[showDeposit.currency] ?? ''}${data?.balanceAfter.toLocaleString()}`)
      }
      setShowDeposit(null)
      setDepositForm({ amount: 0, selectedCardId: '', selectedAccountNumber: '', pin4: '' })
      loadAccounts()
    } catch { toast.error('Error al depositar') }
    finally { setBusy(false) }
  }

  const handleInternalTransfer = async (e: FormEvent) => {
    e.preventDefault()
    if (!showInternal) return
    setBusy(true)
    try {
      const res = await transfersApi.internal({
        fromAccount: showInternal.accountNumber,
        toAccount: internalForm.toAccount,
        amount: internalForm.amount,
        currency: showInternal.currency,
        description: internalForm.description || undefined,
      })
      const data = res.data.data
      if (data) {
        setRecentTransfers((prev) => [data, ...prev].slice(0, 10))
        pendingTransferIds.current.add(data.transferId)
        setInternalStatus({ transferId: data.transferId, status: 'PENDING' })
      }
    } catch { toast.error('Error al iniciar la transferencia interna') }
    finally { setBusy(false) }
  }

  const handleExternalTransfer = async (e: FormEvent) => {
    e.preventDefault()
    if (!showExternal) return
    setBusy(true)
    try {
      if (externalForm.selectedCardId) {
        // Card payment
        const res = await transfersApi.cardPayment({
          fromAccount: externalForm.fromAccount,
          toAccount: externalForm.toAccount,
          amount: externalForm.amount,
          currency: showExternal.currency,
          description: externalForm.description || undefined,
          pin4: externalForm.pin4,
          cardId: externalForm.selectedCardId,
        })
        const data = res.data.data
        if (data) {
          setRecentTransfers((prev) => [data, ...prev].slice(0, 10))
          pendingTransferIds.current.add(data.transferId)
          setExternalStatus({ transferId: data.transferId, status: 'PENDING' })
        }
      } else {
        // Legacy external transfer (no card)
        const res = await transfersApi.external({
          fromAccount: showExternal.accountNumber,
          toAccount: externalForm.toAccount,
          amount: externalForm.amount,
          currency: showExternal.currency,
          description: externalForm.description || undefined,
        })
        const data = res.data.data
        if (data) {
          setRecentTransfers((prev) => [data, ...prev].slice(0, 10))
          pendingTransferIds.current.add(data.transferId)
          setExternalStatus({ transferId: data.transferId, status: 'PENDING' })
        }
      }
    } catch { toast.error('Error al iniciar la transferencia externa') }
    finally { setBusy(false) }
  }

  const handleIssueCard = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    try {
      await cardsApi.issue(issueCardForm.pin4, issueCardForm.dailyLimit)
      toast.success('Tarjeta emitida correctamente')
      setShowIssueCard(false)
      setIssueCardForm({ pin4: '', dailyLimit: 1000 })
      loadAccounts()
    } catch { toast.error('Error al emitir tarjeta') }
    finally { setBusy(false) }
  }

  const handleChangePin = async (e: FormEvent) => {
    e.preventDefault()
    if (!showChangePinCard) return
    setBusy(true)
    try {
      await cardsApi.changePin(showChangePinCard.id, changePinForm.currentPin4, changePinForm.newPin4)
      toast.success('PIN cambiado correctamente')
      setShowChangePinCard(null)
      setChangePinForm({ currentPin4: '', newPin4: '' })
    } catch { toast.error('Error al cambiar PIN') }
    finally { setBusy(false) }
  }

  const handleBlockCard = async () => {
    if (!showBlockCard) return
    setBusy(true)
    try {
      await cardsApi.block(showBlockCard.id)
      toast.success('Tarjeta bloqueada correctamente')
      setShowBlockCard(null)
      loadAccounts()
    } catch { toast.error('Error al bloquear tarjeta') }
    finally { setBusy(false) }
  }

  const ownAccounts = accounts.filter((a) => a.status === 'ACTIVE' && a.id !== showInternal?.id)

  return (
    <AppLayout>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold text-gray-800">Mis Cuentas</h2>
        <div className="flex gap-2 items-center">
          <span className="text-sm text-gray-400">{accounts.length} cuenta{accounts.length !== 1 ? 's' : ''}</span>
          <Button onClick={() => setShowCreate(true)}>+ Nueva cuenta</Button>
        </div>
      </div>

      {loading ? (
        <Spinner text="Cargando cuentas..." />
      ) : accounts.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-400 mb-4">No tienes cuentas registradas</p>
          <Button onClick={() => setShowCreate(true)}>Crear primera cuenta</Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
          {accounts.map((acc) => (
            <AccountCard
              key={acc.id}
              account={acc}
              onView={(id) => navigate(`/accounts/${id}`)}
              onDeposit={setShowDeposit}
              onInternalTransfer={setShowInternal}
              onExternalTransfer={setShowExternal}
            />
          ))}
        </div>
      )}

      {/* Mis Tarjetas */}
      <div className="flex items-center justify-between mb-4 mt-8">
        <h3 className="text-lg font-bold text-gray-800">Mis Tarjetas</h3>
        <Button onClick={() => setShowIssueCard(true)} size="sm">+ Nueva tarjeta</Button>
      </div>
      {cards.length === 0 ? (
        <p className="text-sm text-gray-400 mb-6">No tienes tarjetas. Emite una para operar con PIN de tarjeta.</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
          {cards.map((c) => (
            <div key={c.id} className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 flex flex-col gap-2 hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-sm font-mono text-gray-800">{c.pan}</p>
                  <p className="text-xs text-gray-400">Exp: {c.expiryDate}</p>
                </div>
                <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${c.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>{c.status}</span>
              </div>
              <p className="text-xs text-gray-400">Límite diario: ${c.dailyLimit.toLocaleString()}</p>
              {c.status === 'ACTIVE' && (
                <div className="flex gap-2 mt-1">
                  <button onClick={() => { setShowChangePinCard(c); setChangePinForm({ currentPin4: '', newPin4: '' }) }} className="text-xs text-blue-600 hover:text-blue-800 font-medium">
                    Cambiar PIN
                  </button>
                  <button onClick={() => setShowBlockCard(c)} className="text-xs text-red-600 hover:text-red-800 font-medium ml-auto">
                    Bloquear
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Issue card modal */}
      <Modal open={showIssueCard} onClose={() => setShowIssueCard(false)} title="Nueva Tarjeta">
        <form onSubmit={handleIssueCard} className="flex flex-col gap-4">
          <Input label="PIN de 4 dígitos" type="password" maxLength={4} value={issueCardForm.pin4} onChange={(e) => setIssueCardForm((f) => ({ ...f, pin4: e.target.value }))} required />
          <Input label="Límite diario" type="number" min="0" value={issueCardForm.dailyLimit} onChange={(e) => setIssueCardForm((f) => ({ ...f, dailyLimit: parseFloat(e.target.value) || 0 }))} required />
          <Button type="submit" disabled={busy}>{busy ? 'Emitiendo...' : 'Emitir tarjeta'}</Button>
        </form>
      </Modal>

      {/* Change card PIN modal */}
      {showChangePinCard && (
        <Modal open={true} onClose={() => { setShowChangePinCard(null); setChangePinForm({ currentPin4: '', newPin4: '' }) }} title="Cambiar PIN de tarjeta">
          <form onSubmit={handleChangePin} className="flex flex-col gap-4">
            <p className="text-sm text-gray-500">Tarjeta: {showChangePinCard.pan}</p>
            <Input label="PIN actual" type="password" maxLength={4} value={changePinForm.currentPin4} onChange={(e) => setChangePinForm((f) => ({ ...f, currentPin4: e.target.value }))} required />
            <Input label="Nuevo PIN (4 dígitos)" type="password" maxLength={4} value={changePinForm.newPin4} onChange={(e) => setChangePinForm((f) => ({ ...f, newPin4: e.target.value }))} required />
            <Button type="submit" disabled={busy || changePinForm.currentPin4.length !== 4 || changePinForm.newPin4.length !== 4}>
              {busy ? 'Cambiando...' : 'Cambiar PIN'}
            </Button>
          </form>
        </Modal>
      )}

      {/* Block card confirmation modal */}
      {showBlockCard && (
        <Modal open={true} onClose={() => setShowBlockCard(null)} title="Bloquear tarjeta">
          <div className="flex flex-col gap-4">
            <p className="text-sm text-gray-600">¿Estás seguro de bloquear la tarjeta <strong>{showBlockCard.pan}</strong>?</p>
            <p className="text-xs text-gray-400">Una vez bloqueada, no podrás usarla para depósitos ni transferencias. Las cuentas vinculadas no se verán afectadas.</p>
            <div className="flex gap-2 justify-end">
              <Button variant="ghost" onClick={() => setShowBlockCard(null)}>Cancelar</Button>
              <Button onClick={handleBlockCard} disabled={busy} className="bg-red-600 hover:bg-red-700">{busy ? 'Bloqueando...' : 'Bloquear tarjeta'}</Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Recent transfers */}
      {recentTransfers.length > 0 && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden mb-8">
          <div className="px-6 py-4 border-b border-gray-100">
            <h3 className="text-sm font-semibold text-gray-800">Transferencias recientes</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-400 border-b border-gray-100 bg-gray-50">
                  <th className="px-6 py-3 font-medium">ID</th>
                  <th className="px-6 py-3 font-medium">Destino</th>
                  <th className="px-6 py-3 font-medium text-right">Monto</th>
                  <th className="px-6 py-3 font-medium">Estado</th>
                  <th className="px-6 py-3 font-medium">Motivo</th>
                </tr>
              </thead>
              <tbody>
                {recentTransfers.map((t) => (
                  <tr key={t.transferId} className="border-b border-gray-50">
                    <td className="px-6 py-3 text-gray-500 font-mono text-xs">{t.transferId.slice(0, 8)}...</td>
                    <td className="px-6 py-3 text-gray-800">{t.toAccount}</td>
                    <td className="px-6 py-3 text-right font-medium">
                      {currencySymbol[t.currency] ?? ''}{t.amount.toLocaleString()}
                    </td>
                    <td className="px-6 py-3"><TransferStatusBadge status={t.status} /></td>
                    <td className="px-6 py-3 text-xs text-red-500">{t.rejectionReason ?? '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Create account modal */}
      <Modal open={showCreate} onClose={() => { setShowCreate(false); setCreateForm({ currency: 'USD', selectedCardId: '' }) }} title="Nueva Cuenta">
        <form onSubmit={handleCreateAccount} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Moneda</label>
            <select value={createForm.currency} onChange={(e) => setCreateForm((f) => ({ ...f, currency: e.target.value }))}
              className="w-full px-3 py-2.5 border border-gray-300 rounded text-base outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200">
              {currencies.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Tarjeta (la cuenta se vinculará a esta tarjeta)</label>
            {cards.length === 0 ? (
              <p className="text-sm text-red-500">No tienes tarjetas. Emite una primero.</p>
            ) : (
              <select value={createForm.selectedCardId} onChange={(e) => setCreateForm((f) => ({ ...f, selectedCardId: e.target.value }))}
                className="w-full px-3 py-2.5 border border-gray-300 rounded text-base outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200" required>
                <option value="">Seleccionar tarjeta...</option>
                {cards.filter((c) => c.status === 'ACTIVE').map((c) => (
                  <option key={c.id} value={c.id}>{c.pan} — Exp: {c.expiryDate}</option>
                ))}
              </select>
            )}
          </div>
          <p className="text-xs text-gray-400">La cuenta se vinculará automáticamente a la tarjeta seleccionada.</p>
          <Button type="submit" disabled={busy || cards.length === 0 || !createForm.selectedCardId}>{busy ? 'Creando...' : 'Crear cuenta'}</Button>
        </form>
      </Modal>

      {/* Deposit modal — with optional card */}
      <Modal open={!!showDeposit} onClose={() => { setShowDeposit(null); setDepositForm({ amount: 0, selectedCardId: '', selectedAccountNumber: '', pin4: '' }) }} title={`Depositar - ${showDeposit?.accountNumber ?? ''}`}>
        <form onSubmit={handleDeposit} className="flex flex-col gap-4">
          <p className="text-sm text-gray-500">{showDeposit?.currency} — Saldo actual: {currencySymbol[showDeposit?.currency ?? ''] ?? ''}{showDeposit?.balance.toLocaleString()}</p>

          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Tarjeta (opcional — si usas tarjeta no necesitas PIN de cuenta)</label>
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
            <p className="text-xs text-gray-400">Depósito directo sin tarjeta — no requiere PIN</p>
          )}

          <Button type="submit" disabled={busy || (!!depositForm.selectedCardId && !depositForm.selectedAccountNumber)}>
            {busy ? 'Depositando...' : depositForm.selectedCardId ? 'Depositar con tarjeta' : 'Depositar'}
          </Button>
        </form>
      </Modal>

      {/* Internal transfer modal */}
      <Modal open={!!showInternal && !internalStatus} onClose={() => { setShowInternal(null); setInternalStatus(null) }} title={`Transferir entre cuentas - ${showInternal?.accountNumber ?? ''}`}>
        <form onSubmit={handleInternalTransfer} className="flex flex-col gap-4">
          <p className="text-sm text-gray-500">Origen: {showInternal?.accountNumber} ({showInternal?.currency}) — Saldo: {currencySymbol[showInternal?.currency ?? ''] ?? ''}{showInternal?.balance.toLocaleString()}</p>
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Cuenta destino (propias)</label>
            <select value={internalForm.toAccount} onChange={(e) => setInternalForm((f) => ({ ...f, toAccount: e.target.value }))}
              className="w-full px-3 py-2.5 border border-gray-300 rounded text-base outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200" required>
              <option value="">Seleccionar cuenta...</option>
              {ownAccounts.map((a) => (
                <option key={a.id} value={a.accountNumber}>{a.accountNumber} ({a.currency}) — Saldo: {currencySymbol[a.currency] ?? ''}{a.balance.toLocaleString()}</option>
              ))}
            </select>
          </div>
          <Input label="Monto" type="number" step="0.01" min="0.01" value={internalForm.amount || ''} onChange={(e) => setInternalForm((f) => ({ ...f, amount: parseFloat(e.target.value) || 0 }))} required />
          <Input label="Descripción (opcional)" value={internalForm.description} onChange={(e) => setInternalForm((f) => ({ ...f, description: e.target.value }))} />
          <p className="text-xs text-gray-400">Sin PIN — es transferencia entre tus cuentas</p>
          <Button type="submit" disabled={busy || ownAccounts.length === 0}>{busy ? 'Transfiriendo...' : 'Transferir'}</Button>
        </form>
      </Modal>

      {/* Internal transfer — processing status */}
      <Modal open={!!internalStatus} onClose={() => { setShowInternal(null); setInternalStatus(null) }} title="Transferencia interna">
        <div className="flex flex-col items-center gap-3 py-4">
          <div className={`text-sm font-medium ${internalStatus?.status === 'REJECTED' ? 'text-red-600' : internalStatus?.status === 'COMPLETED' ? 'text-emerald-600' : 'text-gray-600'}`}>
            {internalStatus?.status === 'PENDING' && 'Iniciando transferencia...'}
            {internalStatus?.status === 'DEBITED' && 'Procesando transferencia...'}
            {internalStatus?.status === 'COMPLETED' && 'Transferencia completada'}
            {internalStatus?.status === 'REJECTED' && `Transferencia rechazada: ${internalStatus.error}`}
            {internalStatus?.status === 'FAILED' && 'Transferencia fallida'}
          </div>
          {(!internalStatus || internalStatus.status === 'PENDING' || internalStatus.status === 'DEBITED') && (
            <div className="animate-spin w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full" />
          )}
        </div>
      </Modal>

      {/* External transfer modal — with card */}
      <Modal open={!!showExternal && !externalStatus} onClose={() => { setShowExternal(null); setExternalStatus(null); setExternalForm({ selectedCardId: '', fromAccount: '', toAccount: '', amount: 0, description: '', pin4: '' }) }} title={`Transferir a otra cuenta - ${showExternal?.accountNumber ?? ''}`}>
        <form onSubmit={handleExternalTransfer} className="flex flex-col gap-4">
          <p className="text-sm text-gray-500">
            Saldo actual: {currencySymbol[showExternal?.currency ?? ''] ?? ''}{showExternal?.balance.toLocaleString()}
          </p>

          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Tarjeta (para autenticar la transferencia)</label>
            <select value={externalForm.selectedCardId} onChange={(e) => setExternalForm((f) => ({ ...f, selectedCardId: e.target.value, fromAccount: '', pin4: '' }))}
              className="w-full px-3 py-2.5 border border-gray-300 rounded text-base outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200">
              <option value="">Sin tarjeta (solo cuentas propias)</option>
              {cards.filter((c) => c.status === 'ACTIVE').map((c) => (
                <option key={c.id} value={c.id}>{c.pan} — Exp: {c.expiryDate}</option>
              ))}
            </select>
          </div>

          {externalForm.selectedCardId && (
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-500">Cuenta origen (vinculada a la tarjeta)</label>
              <select value={externalForm.fromAccount} onChange={(e) => setExternalForm((f) => ({ ...f, fromAccount: e.target.value }))}
                className="w-full px-3 py-2.5 border border-gray-300 rounded text-base outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200" required>
                <option value="">Seleccionar cuenta...</option>
                {externalCardAccounts.map((link) => (
                  <option key={link.accountNumber} value={link.accountNumber}>{link.accountNumber} ({link.currency}){link.isPrimary ? ' (Principal)' : ''}</option>
                ))}
              </select>
            </div>
          )}

          <Input label="Cuenta destino (número)" value={externalForm.toAccount} onChange={(e) => setExternalForm((f) => ({ ...f, toAccount: e.target.value }))} required />
          <Input label="Monto" type="number" step="0.01" min="0.01" value={externalForm.amount || ''} onChange={(e) => setExternalForm((f) => ({ ...f, amount: parseFloat(e.target.value) || 0 }))} required />
          <Input label="Descripción (opcional)" value={externalForm.description} onChange={(e) => setExternalForm((f) => ({ ...f, description: e.target.value }))} />

          {externalForm.selectedCardId && (
            <Input label="PIN de la tarjeta" type="password" maxLength={4} value={externalForm.pin4} onChange={(e) => setExternalForm((f) => ({ ...f, pin4: e.target.value }))} required />
          )}

          {!externalForm.selectedCardId && (
            <p className="text-xs text-gray-400">Sin tarjeta — transferencia entre cuentas propias (usa transferencia interna)</p>
          )}

          <Button type="submit" disabled={busy || (!!externalForm.selectedCardId && (!externalForm.fromAccount || !externalForm.pin4))}>
            {busy ? 'Transfiriendo...' : 'Transferir'}
          </Button>
        </form>
      </Modal>

      {/* External transfer — processing status */}
      <Modal open={!!externalStatus} onClose={() => { setShowExternal(null); setExternalStatus(null) }} title="Transferencia externa">
        <div className="flex flex-col items-center gap-3 py-4">
          <div className={`text-sm font-medium ${externalStatus?.status === 'REJECTED' ? 'text-red-600' : externalStatus?.status === 'COMPLETED' ? 'text-emerald-600' : 'text-gray-600'}`}>
            {externalStatus?.status === 'PENDING' && 'Iniciando transferencia...'}
            {externalStatus?.status === 'DEBITED' && 'Procesando transferencia (esperando confirmación)...'}
            {externalStatus?.status === 'COMPLETED' && 'Transferencia completada'}
            {externalStatus?.status === 'REJECTED' && `Transferencia rechazada: ${externalStatus.error}`}
            {externalStatus?.status === 'FAILED' && 'Transferencia fallida'}
          </div>
          {(!externalStatus || externalStatus.status === 'PENDING' || externalStatus.status === 'DEBITED') && (
            <div className="animate-spin w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full" />
          )}
        </div>
      </Modal>
    </AppLayout>
  )
}
