import { useState, useEffect, type FormEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { ArrowLeft, ArrowUpRight, ArrowDownLeft, Lock, Receipt, Send } from 'lucide-react'
import { accountsApi } from '../api/accounts-api'
import { cardsApi } from '../api/cards-api'
import { transfersApi, type TransferResponse } from '../api/transfers-api'
import AppLayout from '../components/layout/AppLayout'
import Modal from '../components/ui/Modal'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import Spinner from '../components/ui/Spinner'
import UICard from '../components/ui/Card'
import { StatusBadge } from '../components/ui/Badge'
import { formatCurrency, formatDate } from '../lib/utils'
import type { Account, Movement } from '../models/account'
import type { Card as CardModel, CardAccountLink } from '../models/card'

type Tab = 'transferencias' | 'movimientos'

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

  const [cards, setCards] = useState<CardModel[]>([])
  const [showDeposit, setShowDeposit] = useState(false)
  const [depositForm, setDepositForm] = useState({ amount: 0, selectedCardId: '', selectedAccountNumber: '', pin4: '' })
  const [depositCardAccounts, setDepositCardAccounts] = useState<CardAccountLink[]>([])
  const [depositBusy, setDepositBusy] = useState(false)
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

  useEffect(() => {
    if (!depositForm.selectedCardId) { setDepositCardAccounts([]); return }
    cardsApi.getById(depositForm.selectedCardId).then((res) => {
      setDepositCardAccounts(res.data.data?.accounts ?? [])
    }).catch(() => toast.error('Error al cargar cuentas de la tarjeta'))
  }, [depositForm.selectedCardId])

  const handleDeposit = async (e: FormEvent) => {
    e.preventDefault()
    if (!account) return
    setDepositBusy(true)
    try {
      if (depositForm.selectedCardId && depositForm.selectedAccountNumber) {
        await cardsApi.deposit(depositForm.selectedCardId, depositForm.selectedAccountNumber, depositForm.amount, depositForm.pin4)
        toast.success('Depósito con tarjeta exitoso')
      } else {
        const res = await accountsApi.deposit(account.accountNumber, account.currency, depositForm.amount)
        toast.success(`Depósito exitoso — Saldo: ${formatCurrency(res.data.data?.balanceAfter ?? 0, account.currency)}`)
      }
      setShowDeposit(false)
      setDepositForm({ amount: 0, selectedCardId: '', selectedAccountNumber: '', pin4: '' })
      accountsApi.getById(account.id).then((r) => setAccount(r.data.data ?? null))
    } catch { toast.error('Error al depositar') }
    finally { setDepositBusy(false) }
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
    } catch { toast.error('Error al iniciar la transferencia') }
    finally { setBusy(false) }
  }

  if (loading) return <AppLayout><Spinner text="Cargando cuenta..." /></AppLayout>
  if (!account) return null

  return (
    <AppLayout>
      <button onClick={() => navigate('/dashboard')} className="inline-flex items-center gap-1 text-sm text-blue-600 hover:text-blue-800 font-medium mb-4 transition-colors">
        <ArrowLeft className="w-4 h-4" /> Volver al dashboard
      </button>

      <UICard padding="lg" className="mb-6">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs text-gray-400 font-mono">{account.accountNumber}</p>
            <p className="text-xs text-gray-400 mt-0.5">{account.currency}</p>
          </div>
          <StatusBadge status={account.status} />
        </div>
        <p className="text-3xl font-bold text-gray-900 mt-3">
          {formatCurrency(account.balance, account.currency)}
        </p>
        {account.status === 'ACTIVE' && (
          <div className="flex gap-2 mt-4">
            <Button onClick={() => setShowTransfer(true)}>
              <Send className="w-4 h-4" /> Transferir
            </Button>
            <Button onClick={() => setShowDeposit(true)} variant="secondary">
              <ArrowDownLeft className="w-4 h-4" /> Depositar
            </Button>
            <Button onClick={() => setShowBlock(true)} variant="danger">
              <Lock className="w-4 h-4" /> Bloquear
            </Button>
          </div>
        )}
      </UICard>

      <div className="flex gap-1 mb-4 border-b border-gray-200">
        {(['transferencias', 'movimientos'] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              tab === t ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-400 hover:text-gray-600'
            }`}
          >
            {t === 'transferencias' ? <ArrowUpRight className="w-4 h-4" /> : <Receipt className="w-4 h-4" />}
            {t === 'transferencias' ? 'Transferencias' : 'Movimientos'}
          </button>
        ))}
      </div>

      {tab === 'transferencias' && (
        <UICard padding="sm">
          {transfersLoading ? (
            <Spinner text="Cargando transferencias..." />
          ) : transfers.length === 0 ? (
            <div className="p-8 text-center text-gray-400">Sin transferencias</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-400 border-b border-gray-100 bg-gray-50/80">
                    <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">ID</th>
                    <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">Destino</th>
                    <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider text-right">Monto</th>
                    <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">Estado</th>
                    <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider text-right">Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  {transfers.map((t) => (
                    <tr key={t.transferId} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                      <td className="px-4 py-3 text-gray-500 font-mono text-xs">{t.transferId.slice(0, 8)}...</td>
                      <td className="px-4 py-3 text-gray-800">{t.toAccount}</td>
                      <td className="px-4 py-3 text-right font-medium">{formatCurrency(t.amount, t.currency)}</td>
                      <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                      <td className="px-4 py-3 text-right text-gray-400 text-xs">{formatDate(t.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </UICard>
      )}

      {tab === 'movimientos' && (
        <UICard padding="sm">
          {movementsLoading ? (
            <Spinner text="Cargando movimientos..." />
          ) : movements.length === 0 ? (
            <div className="p-8 text-center text-gray-400">Sin movimientos</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-400 border-b border-gray-100 bg-gray-50/80">
                    <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">#</th>
                    <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider">Tipo</th>
                    <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider text-right">Monto</th>
                    <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider text-right">Saldo</th>
                    <th className="px-4 py-3 font-medium text-xs uppercase tracking-wider text-right">Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  {movements.map((m) => (
                    <tr key={m.id} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors last:border-0">
                      <td className="px-4 py-3 text-gray-500 font-mono text-xs">{m.movementNumber}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={m.type === 'CREDIT' ? 'COMPLETED' : 'FAILED'} />
                        <span className="ml-1.5 text-xs text-gray-500">{m.type === 'CREDIT' ? 'Ingreso' : 'Salida'}</span>
                      </td>
                      <td className={`px-4 py-3 text-right font-medium ${m.type === 'CREDIT' ? 'text-emerald-600' : 'text-red-600'}`}>
                        {m.type === 'CREDIT' ? '+' : '-'}{m.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </td>
                      <td className="px-4 py-3 text-right text-gray-600 font-mono">{m.balanceAfter.toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
                      <td className="px-4 py-3 text-right text-gray-400 text-xs">{formatDate(m.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </UICard>
      )}

      <Modal open={showDeposit} onClose={() => setShowDeposit(false)} title="Depositar" description={`${account.currency} — Saldo: ${formatCurrency(account.balance, account.currency)}`}>
        <form onSubmit={handleDeposit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-700">Tarjeta (opcional)</label>
            <select value={depositForm.selectedCardId} onChange={(e) => setDepositForm((f) => ({ ...f, selectedCardId: e.target.value, selectedAccountNumber: '', pin4: '' }))}
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

          {!depositForm.selectedCardId && (
            <p className="text-xs text-gray-400">Depósito directo — no requiere PIN</p>
          )}

          <Button type="submit" disabled={depositBusy || (!!depositForm.selectedCardId && !depositForm.selectedAccountNumber)} loading={depositBusy}>
            {depositForm.selectedCardId ? 'Depositar con tarjeta' : 'Depositar'}
          </Button>
        </form>
      </Modal>

      <Modal open={showTransfer} onClose={() => setShowTransfer(false)} title="Transferir" description={`Desde: ${account.accountNumber} (${account.currency}) — Saldo: ${formatCurrency(account.balance, account.currency)}`}>
        <form onSubmit={handleTransfer} className="flex flex-col gap-4">
          <Input label="Cuenta destino" value={transferForm.toAccount} onChange={(e) => setTransferForm((f) => ({ ...f, toAccount: e.target.value }))} required />
          <Input label="Monto" type="number" step="0.01" min="0.01" value={transferForm.amount || ''} onChange={(e) => setTransferForm((f) => ({ ...f, amount: parseFloat(e.target.value) || 0 }))} required />
          <Input label="Descripción (opcional)" value={transferForm.description} onChange={(e) => setTransferForm((f) => ({ ...f, description: e.target.value }))} />
          <Button type="submit" disabled={busy} loading={busy}>Transferir</Button>
        </form>
      </Modal>

      <Modal open={showBlock} onClose={() => setShowBlock(false)} title="Bloquear cuenta" description={`¿Estás seguro de bloquear la cuenta ${account.accountNumber}?`}>
        <div className="flex flex-col gap-4">
          <p className="text-sm text-gray-500">Una vez bloqueada, no podrás realizar transferencias ni depósitos desde esta cuenta.</p>
          <div className="flex gap-2 justify-end">
            <Button variant="ghost" onClick={() => setShowBlock(false)}>Cancelar</Button>
            <Button variant="danger" onClick={async () => {
              setBusy(true)
              try {
                const res = await accountsApi.block(account.id)
                setAccount((prev) => prev ? { ...prev, status: res.data.data?.status ?? 'BLOCKED' } : prev)
                toast.success('Cuenta bloqueada correctamente')
                setShowBlock(false)
              } catch { toast.error('Error al bloquear cuenta') }
              finally { setBusy(false) }
            }} disabled={busy} loading={busy}>Bloquear cuenta</Button>
          </div>
        </div>
      </Modal>
    </AppLayout>
  )
}
