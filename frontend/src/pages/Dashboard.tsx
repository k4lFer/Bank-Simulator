import { useState, useEffect, useRef, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { Plus, Eye, ArrowUpRight, ArrowDownLeft, CreditCard, Lock, Key, RefreshCw, Landmark } from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { accountsApi } from '../api/accounts-api'
import { cardsApi } from '../api/cards-api'
import { transfersApi, type TransferResponse } from '../api/transfers-api'
import AppLayout from '../components/layout/AppLayout'
import Modal from '../components/ui/Modal'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import Spinner from '../components/ui/Spinner'
import Card from '../components/ui/Card'
import { StatusBadge } from '../components/ui/Badge'
import { formatCurrency, currencies } from '../lib/utils'
import type { Account } from '../models/account'
import type { Card as CardType, CardAccountLink } from '../models/card'

export default function DashboardPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [accounts, setAccounts] = useState<Account[]>([])
  const [cards, setCards] = useState<CardType[]>([])
  const [loading, setLoading] = useState(true)

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState({ currency: 'USD', selectedCardId: '' })

  const [showDeposit, setShowDeposit] = useState<Account | null>(null)
  const [depositForm, setDepositForm] = useState({ amount: 0, selectedCardId: '', selectedAccountNumber: '', pin4: '', pin6: '' })
  const [depositCardAccounts, setDepositCardAccounts] = useState<CardAccountLink[]>([])
  const [depositBusy, setDepositBusy] = useState(false)

  const [showInternal, setShowInternal] = useState<Account | null>(null)
  const [internalForm, setInternalForm] = useState({ toAccount: '', amount: 0, description: '' })
  const [internalStatus, setInternalStatus] = useState<{ transferId: string; status: string; error?: string } | null>(null)

  const [showExternal, setShowExternal] = useState<Account | null>(null)
  const [externalForm, setExternalForm] = useState({ selectedCardId: '', fromAccount: '', toAccount: '', amount: 0, description: '', pin4: '' })
  const [externalCardAccounts, setExternalCardAccounts] = useState<CardAccountLink[]>([])
  const [externalStatus, setExternalStatus] = useState<{ transferId: string; status: string; error?: string } | null>(null)

  const [busy, setBusy] = useState(false)
  const [showIssueCard, setShowIssueCard] = useState(false)
  const [issueCardForm, setIssueCardForm] = useState({ pin4: '', pin6: '' })

  const [showChangePinCard, setShowChangePinCard] = useState<CardType | null>(null)
  const [changePinForm, setChangePinForm] = useState({ currentPin4: '', newPin4: '' })

  const [showBlockCard, setShowBlockCard] = useState<CardType | null>(null)

  const [recentTransfers, setRecentTransfers] = useState<TransferResponse[]>([])
  const pendingTransferIds = useRef<Set<string>>(new Set())

  useEffect(() => {
    if (!user) return
    const token = localStorage.getItem('accessToken')
    if (!token) return
    const es = new EventSource(`/api/transfers/stream?token=${encodeURIComponent(token)}`)
    es.addEventListener('transfer-status', (e) => {
      const t: TransferResponse = JSON.parse(e.data)
      if (!pendingTransferIds.current.has(t.transferId)) return
      setRecentTransfers((prev) => prev.map((p) => p.transferId === t.transferId ? t : p))
      if (t.status === 'COMPLETED') {
        pendingTransferIds.current.delete(t.transferId)
        setInternalStatus(null)
        setExternalStatus(null)
        toast.success('Transferencia completada')
        loadAccounts()
      } else if (t.status === 'REJECTED') {
        pendingTransferIds.current.delete(t.transferId)
        toast.error(`Transferencia rechazada: ${t.rejectionReason ?? ''}`)
        setInternalStatus(null)
        setExternalStatus(null)
        loadAccounts()
      } else if (t.status === 'DEBITED') {
        setInternalStatus((s) => s?.transferId === t.transferId ? { ...s, status: 'DEBITED' } : s)
        setExternalStatus((s) => s?.transferId === t.transferId ? { ...s, status: 'DEBITED' } : s)
      }
    })
    return () => es.close()
  }, [user])

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

  useEffect(() => {
    if (!depositForm.selectedCardId) { setDepositCardAccounts([]); return }
    cardsApi.getById(depositForm.selectedCardId).then((res) => {
      setDepositCardAccounts(res.data.data?.accounts ?? [])
    }).catch(() => toast.error('Error al cargar cuentas de la tarjeta'))
  }, [depositForm.selectedCardId])

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
    setDepositBusy(true)
    try {
      if (depositForm.selectedCardId && depositForm.selectedAccountNumber) {
        await cardsApi.deposit(depositForm.selectedCardId, depositForm.selectedAccountNumber, depositForm.amount, depositForm.pin4, depositForm.pin6)
        toast.success('Depósito con tarjeta exitoso')
      } else {
        const res = await accountsApi.deposit(showDeposit.accountNumber, showDeposit.currency, depositForm.amount)
        const data = res.data.data
        toast.success(`Depósito exitoso — Saldo: ${formatCurrency(data?.balanceAfter ?? 0, showDeposit.currency)}`)
      }
      setShowDeposit(null)
      setDepositForm({ amount: 0, selectedCardId: '', selectedAccountNumber: '', pin4: '', pin6: '' })
      loadAccounts()
    } catch { toast.error('Error al depositar') }
    finally { setDepositBusy(false) }
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
      let res
      if (externalForm.selectedCardId) {
        res = await transfersApi.cardPayment({
          fromAccount: externalForm.fromAccount,
          toAccount: externalForm.toAccount,
          amount: externalForm.amount,
          currency: showExternal.currency,
          description: externalForm.description || undefined,
          pin4: externalForm.pin4,
          cardId: externalForm.selectedCardId,
        })
      } else {
        res = await transfersApi.external({
          fromAccount: showExternal.accountNumber,
          toAccount: externalForm.toAccount,
          amount: externalForm.amount,
          currency: showExternal.currency,
          description: externalForm.description || undefined,
        })
      }
      const data = res.data.data
      if (data) {
        setRecentTransfers((prev) => [data, ...prev].slice(0, 10))
        pendingTransferIds.current.add(data.transferId)
        setExternalStatus({ transferId: data.transferId, status: 'PENDING' })
      }
    } catch { toast.error('Error al iniciar la transferencia externa') }
    finally { setBusy(false) }
  }

  const handleIssueCard = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    try {
      await cardsApi.issue(issueCardForm.pin4, issueCardForm.pin6)
      toast.success('Tarjeta emitida correctamente')
      setShowIssueCard(false)
      setIssueCardForm({ pin4: '', pin6: '' })
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
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Mis Tarjetas</h2>
          <p className="text-sm text-gray-400 mt-0.5">{cards.length} tarjeta{cards.length !== 1 ? 's' : ''}</p>
        </div>
        <Button onClick={() => setShowIssueCard(true)} size="sm">
          <Plus className="w-4 h-4" /> Nueva tarjeta
        </Button>
      </div>

      {loading ? (
        <Spinner text="Cargando tarjetas..." />
      ) : cards.length === 0 ? (
        <Card className="text-center py-8 mb-6">
          <CreditCard className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">No tienes tarjetas. Emite una para operar con PIN de tarjeta.</p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
          {cards.map((c) => (
            <Card key={c.id} hover>
              <div className="flex flex-col gap-2">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-sm font-mono text-gray-900 tracking-wider">{c.pan}</p>
                    <p className="text-xs text-gray-400">Exp: {c.expiryDate}</p>
                  </div>
                  <StatusBadge status={c.status} />
                </div>
                {c.status === 'ACTIVE' && (
                  <div className="flex gap-2 mt-1">
                    <button onClick={() => { setShowChangePinCard(c); setChangePinForm({ currentPin4: '', newPin4: '' }) }} className="inline-flex items-center gap-1 text-xs text-blue-600 hover:text-blue-800 font-medium transition-colors">
                      <Key className="w-3.5 h-3.5" /> Cambiar PIN
                    </button>
                    <button onClick={() => setShowBlockCard(c)} className="inline-flex items-center gap-1 text-xs text-red-600 hover:text-red-800 font-medium transition-colors ml-auto">
                      <Lock className="w-3.5 h-3.5" /> Bloquear
                    </button>
                  </div>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      <div className="flex items-center justify-between mb-6 mt-8">
        <div>
          <h3 className="text-lg font-bold text-gray-900">Mis Cuentas</h3>
          <p className="text-sm text-gray-400 mt-0.5">{accounts.length} cuenta{accounts.length !== 1 ? 's' : ''}</p>
        </div>
        <Button onClick={() => setShowCreate(true)}>
          <Plus className="w-4 h-4" /> Nueva cuenta
        </Button>
      </div>

      {accounts.length === 0 ? (
        <Card className="text-center py-12">
          <Landmark className="w-12 h-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-400 mb-4">No tienes cuentas registradas</p>
          <Button onClick={() => setShowCreate(true)}>Crear primera cuenta</Button>
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
          {accounts.map((acc) => (
            <Card key={acc.id} hover>
              <div className="flex flex-col gap-3">
                <div className="flex items-start justify-between">
                  <div className="min-w-0">
                    <p className="text-xs text-gray-400 font-mono truncate">{acc.accountNumber}</p>
                    <p className="text-xs text-gray-400 mt-0.5">{acc.currency}</p>
                  </div>
                  <StatusBadge status={acc.status} />
                </div>
                <p className="text-2xl font-bold text-gray-900">
                  {formatCurrency(acc.balance, acc.currency)}
                </p>
                <div className="flex gap-2 mt-1 flex-wrap">
                  {acc.status === 'ACTIVE' ? (
                    <>
                      <button onClick={() => navigate(`/accounts/${acc.id}`)} className="inline-flex items-center gap-1 text-xs text-blue-600 hover:text-blue-800 font-medium transition-colors">
                        <Eye className="w-3.5 h-3.5" /> Detalle
                      </button>
                      <button onClick={() => setShowDeposit(acc)} className="inline-flex items-center gap-1 text-xs text-emerald-600 hover:text-emerald-800 font-medium transition-colors ml-auto">
                        <ArrowDownLeft className="w-3.5 h-3.5" /> Depositar
                      </button>
                      <button onClick={() => setShowInternal(acc)} className="inline-flex items-center gap-1 text-xs text-purple-600 hover:text-purple-800 font-medium transition-colors">
                        <ArrowUpRight className="w-3.5 h-3.5" /> Entre cuentas
                      </button>
                      <button onClick={() => setShowExternal(acc)} className="inline-flex items-center gap-1 text-xs text-orange-600 hover:text-orange-800 font-medium transition-colors">
                        <ArrowUpRight className="w-3.5 h-3.5" /> Transferir
                      </button>
                    </>
                  ) : (
                    <span className="text-xs text-red-500 font-medium">Cuenta bloqueada</span>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {recentTransfers.length > 0 && (
        <Card padding="sm" className="mb-8">
          <div className="px-4 py-3 border-b border-gray-100">
            <h3 className="text-sm font-semibold text-gray-800">Transferencias recientes</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-400 border-b border-gray-100 bg-gray-50/80">
                  <th className="px-4 py-2.5 font-medium text-xs uppercase tracking-wider">ID</th>
                  <th className="px-4 py-2.5 font-medium text-xs uppercase tracking-wider">Destino</th>
                  <th className="px-4 py-2.5 font-medium text-xs uppercase tracking-wider text-right">Monto</th>
                  <th className="px-4 py-2.5 font-medium text-xs uppercase tracking-wider">Estado</th>
                  <th className="px-4 py-2.5 font-medium text-xs uppercase tracking-wider">Motivo</th>
                </tr>
              </thead>
              <tbody>
                {recentTransfers.map((t) => (
                  <tr key={t.transferId} className="border-b border-gray-50 hover:bg-gray-50/50">
                    <td className="px-4 py-2.5 text-gray-500 font-mono text-xs">{t.transferId.slice(0, 8)}...</td>
                    <td className="px-4 py-2.5 text-gray-800">{t.toAccount}</td>
                    <td className="px-4 py-2.5 text-right font-medium">{formatCurrency(t.amount, t.currency)}</td>
                    <td className="px-4 py-2.5"><StatusBadge status={t.status} /></td>
                    <td className="px-4 py-2.5 text-xs text-red-500">{t.rejectionReason ?? '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal open={showCreate} onClose={() => { setShowCreate(false); setCreateForm({ currency: 'USD', selectedCardId: '' }) }} title="Nueva Cuenta" description="Selecciona la moneda y la tarjeta para vincular">
        <form onSubmit={handleCreateAccount} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-700">Moneda</label>
            <select value={createForm.currency} onChange={(e) => setCreateForm((f) => ({ ...f, currency: e.target.value }))}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500">
              {currencies.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-700">Tarjeta (la cuenta se vinculará)</label>
            {cards.length === 0 ? (
              <p className="text-sm text-red-500">No tienes tarjetas. Emite una primero.</p>
            ) : (
              <select value={createForm.selectedCardId} onChange={(e) => setCreateForm((f) => ({ ...f, selectedCardId: e.target.value }))}
                className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500" required>
                <option value="">Seleccionar tarjeta...</option>
                {cards.filter((c) => c.status === 'ACTIVE').map((c) => (
                  <option key={c.id} value={c.id}>{c.pan} — Exp: {c.expiryDate}</option>
                ))}
              </select>
            )}
          </div>
          <Button type="submit" disabled={busy || cards.length === 0 || !createForm.selectedCardId} loading={busy}>Crear cuenta</Button>
        </form>
      </Modal>

      <Modal
        open={!!showDeposit}
        onClose={() => { setShowDeposit(null); setDepositForm({ amount: 0, selectedCardId: '', selectedAccountNumber: '', pin4: '', pin6: '' }) }}
        title="Depositar"
        description={showDeposit ? `${showDeposit.currency} — Saldo: ${formatCurrency(showDeposit.balance, showDeposit.currency)}` : ''}
      >
        <form onSubmit={handleDeposit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-700">Tarjeta (opcional)</label>
            <select value={depositForm.selectedCardId} onChange={(e) => setDepositForm((f) => ({ ...f, selectedCardId: e.target.value, selectedAccountNumber: '', pin4: '', pin6: '' }))}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500">
              <option value="">Sin tarjeta (depósito directo)</option>
              {cards.filter((c) => c.status === 'ACTIVE').map((c) => (
                <option key={c.id} value={c.id}>{c.pan} — Exp: {c.expiryDate}</option>
              ))}
            </select>
          </div>

          {depositForm.selectedCardId && depositCardAccounts.length > 0 && (
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-gray-700">Cuenta a depositar (vinculada a la tarjeta)</label>
              <select value={depositForm.selectedAccountNumber} onChange={(e) => setDepositForm((f) => ({ ...f, selectedAccountNumber: e.target.value }))}
                className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500" required>
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

          {depositForm.selectedCardId && (
            <Input label="PIN de 6 dígitos" type="password" maxLength={6} value={depositForm.pin6} onChange={(e) => setDepositForm((f) => ({ ...f, pin6: e.target.value }))} required />
          )}

          {!depositForm.selectedCardId && (
            <p className="text-xs text-gray-400">Depósito directo — no requiere PIN</p>
          )}

          <Button type="submit" disabled={depositBusy || (!!depositForm.selectedCardId && (!depositForm.selectedAccountNumber || depositForm.pin6.length !== 6))} loading={depositBusy}>
            {depositForm.selectedCardId ? 'Depositar con tarjeta' : 'Depositar'}
          </Button>
        </form>
      </Modal>

      <Modal open={!!showInternal && !internalStatus} onClose={() => { setShowInternal(null); setInternalStatus(null) }} title="Transferir entre cuentas" description={`Origen: ${showInternal?.accountNumber} (${showInternal?.currency}) — Saldo: ${showInternal ? formatCurrency(showInternal.balance, showInternal.currency) : ''}`}>
        <form onSubmit={handleInternalTransfer} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-700">Cuenta destino (propias)</label>
            <select value={internalForm.toAccount} onChange={(e) => setInternalForm((f) => ({ ...f, toAccount: e.target.value }))}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500" required>
              <option value="">Seleccionar cuenta...</option>
              {ownAccounts.map((a) => (
                <option key={a.id} value={a.accountNumber}>{a.accountNumber} ({a.currency}) — {formatCurrency(a.balance, a.currency)}</option>
              ))}
            </select>
          </div>
          <Input label="Monto" type="number" step="0.01" min="0.01" value={internalForm.amount || ''} onChange={(e) => setInternalForm((f) => ({ ...f, amount: parseFloat(e.target.value) || 0 }))} required />
          <Input label="Descripción (opcional)" value={internalForm.description} onChange={(e) => setInternalForm((f) => ({ ...f, description: e.target.value }))} />
          <p className="text-xs text-gray-400">Sin PIN — transferencia entre tus cuentas</p>
          <Button type="submit" disabled={busy || ownAccounts.length === 0} loading={busy}>Transferir</Button>
        </form>
      </Modal>

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
            <RefreshCw className="w-6 h-6 text-blue-600 animate-spin" />
          )}
        </div>
      </Modal>

      <Modal open={!!showExternal && !externalStatus} onClose={() => { setShowExternal(null); setExternalStatus(null); setExternalForm({ selectedCardId: '', fromAccount: '', toAccount: '', amount: 0, description: '', pin4: '' }) }} title="Transferir a otra cuenta" description={`Saldo: ${showExternal ? formatCurrency(showExternal.balance, showExternal.currency) : ''}`}>
        <form onSubmit={handleExternalTransfer} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-700">Tarjeta (para autenticar)</label>
            <select value={externalForm.selectedCardId} onChange={(e) => setExternalForm((f) => ({ ...f, selectedCardId: e.target.value, fromAccount: '', pin4: '' }))}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500">
              <option value="">Sin tarjeta (solo cuentas propias)</option>
              {cards.filter((c) => c.status === 'ACTIVE').map((c) => (
                <option key={c.id} value={c.id}>{c.pan} — Exp: {c.expiryDate}</option>
              ))}
            </select>
          </div>

          {externalForm.selectedCardId && (
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-gray-700">Cuenta origen (vinculada a la tarjeta)</label>
              <select value={externalForm.fromAccount} onChange={(e) => setExternalForm((f) => ({ ...f, fromAccount: e.target.value }))}
                className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500" required>
                <option value="">Seleccionar cuenta...</option>
                {externalCardAccounts.map((link) => (
                  <option key={link.accountNumber} value={link.accountNumber}>{link.accountNumber} ({link.currency}){link.isPrimary ? ' (Principal)' : ''}</option>
                ))}
              </select>
            </div>
          )}

          <Input label="Cuenta destino" value={externalForm.toAccount} onChange={(e) => setExternalForm((f) => ({ ...f, toAccount: e.target.value }))} required />
          <Input label="Monto" type="number" step="0.01" min="0.01" value={externalForm.amount || ''} onChange={(e) => setExternalForm((f) => ({ ...f, amount: parseFloat(e.target.value) || 0 }))} required />
          <Input label="Descripción (opcional)" value={externalForm.description} onChange={(e) => setExternalForm((f) => ({ ...f, description: e.target.value }))} />

          {externalForm.selectedCardId && (
            <Input label="PIN de la tarjeta" type="password" maxLength={4} value={externalForm.pin4} onChange={(e) => setExternalForm((f) => ({ ...f, pin4: e.target.value }))} required />
          )}

          <Button type="submit" disabled={busy || (!!externalForm.selectedCardId && (!externalForm.fromAccount || !externalForm.pin4))} loading={busy}>
            Transferir
          </Button>
        </form>
      </Modal>

      <Modal open={!!externalStatus} onClose={() => { setShowExternal(null); setExternalStatus(null) }} title="Transferencia externa">
        <div className="flex flex-col items-center gap-3 py-4">
          <div className={`text-sm font-medium ${externalStatus?.status === 'REJECTED' ? 'text-red-600' : externalStatus?.status === 'COMPLETED' ? 'text-emerald-600' : 'text-gray-600'}`}>
            {externalStatus?.status === 'PENDING' && 'Iniciando transferencia...'}
            {externalStatus?.status === 'DEBITED' && 'Procesando transferencia...'}
            {externalStatus?.status === 'COMPLETED' && 'Transferencia completada'}
            {externalStatus?.status === 'REJECTED' && `Rechazada: ${externalStatus.error}`}
            {externalStatus?.status === 'FAILED' && 'Transferencia fallida'}
          </div>
          {(!externalStatus || externalStatus.status === 'PENDING' || externalStatus.status === 'DEBITED') && (
            <RefreshCw className="w-6 h-6 text-blue-600 animate-spin" />
          )}
        </div>
      </Modal>

      <Modal open={showIssueCard} onClose={() => setShowIssueCard(false)} title="Nueva Tarjeta">
        <form onSubmit={handleIssueCard} className="flex flex-col gap-4">
          <Input label="PIN de 4 dígitos" type="password" maxLength={4} value={issueCardForm.pin4} onChange={(e) => setIssueCardForm((f) => ({ ...f, pin4: e.target.value }))} required />
          <Input label="PIN de 6 dígitos" type="password" maxLength={6} value={issueCardForm.pin6} onChange={(e) => setIssueCardForm((f) => ({ ...f, pin6: e.target.value }))} required />
          <Button type="submit" disabled={busy} loading={busy}>Emitir tarjeta</Button>
        </form>
      </Modal>

      {showChangePinCard && (
        <Modal open={true} onClose={() => { setShowChangePinCard(null); setChangePinForm({ currentPin4: '', newPin4: '' }) }} title="Cambiar PIN de tarjeta" description={`Tarjeta: ${showChangePinCard.pan}`}>
          <form onSubmit={handleChangePin} className="flex flex-col gap-4">
            <Input label="PIN actual" type="password" maxLength={4} value={changePinForm.currentPin4} onChange={(e) => setChangePinForm((f) => ({ ...f, currentPin4: e.target.value }))} required />
            <Input label="Nuevo PIN (4 dígitos)" type="password" maxLength={4} value={changePinForm.newPin4} onChange={(e) => setChangePinForm((f) => ({ ...f, newPin4: e.target.value }))} required />
            <Button type="submit" disabled={busy || changePinForm.currentPin4.length !== 4 || changePinForm.newPin4.length !== 4} loading={busy}>Cambiar PIN</Button>
          </form>
        </Modal>
      )}

      {showBlockCard && (
        <Modal open={true} onClose={() => setShowBlockCard(null)} title="Bloquear tarjeta" description={`¿Estás seguro de bloquear la tarjeta ${showBlockCard.pan}?`}>
          <div className="flex flex-col gap-4">
            <p className="text-sm text-gray-500">Una vez bloqueada, no podrás usarla para depósitos ni transferencias. Las cuentas vinculadas no se verán afectadas.</p>
            <div className="flex gap-2 justify-end">
              <Button variant="ghost" onClick={() => setShowBlockCard(null)}>Cancelar</Button>
              <Button variant="danger" onClick={handleBlockCard} disabled={busy} loading={busy}>Bloquear tarjeta</Button>
            </div>
          </div>
        </Modal>
      )}
    </AppLayout>
  )
}
