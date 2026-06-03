import { useState, useEffect, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { accountsApi } from '../api/accounts-api'
import { transfersApi, type TransferResponse } from '../api/transfers-api'
import AppLayout from '../components/layout/AppLayout'
import Modal from '../components/ui/Modal'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import Spinner from '../components/ui/Spinner'
import type { Account } from '../models/account'

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

function AccountCard({ account, onView, onTransfer, onDeposit }: { account: Account; onView: (id: string) => void; onTransfer: (a: Account) => void; onDeposit: (a: Account) => void }) {
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
      <div className="flex gap-2 mt-1">
        <button onClick={() => onView(account.id)} className="text-sm text-blue-600 hover:text-blue-800 font-medium">
          Movimientos &rarr;
        </button>
        {account.status === 'ACTIVE' && (
          <>
            <button onClick={() => onDeposit(account)} className="text-sm text-emerald-600 hover:text-emerald-800 font-medium ml-auto">
              Depositar
            </button>
            <button onClick={() => onTransfer(account)} className="text-sm text-purple-600 hover:text-purple-800 font-medium">
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
  const [accounts, setAccounts] = useState<Account[]>([])
  const [loading, setLoading] = useState(true)

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState({ currency: 'USD', pin6: '123456', pin4: '1234' })

  const [showDeposit, setShowDeposit] = useState<Account | null>(null)
  const [depositForm, setDepositForm] = useState({ amount: 0, pin4: '1234' })

  const [showTransfer, setShowTransfer] = useState<Account | null>(null)
  const [transferForm, setTransferForm] = useState({ toAccount: '', amount: 0, description: '' })

  const [busy, setBusy] = useState(false)
  const [recentTransfers, setRecentTransfers] = useState<TransferResponse[]>([])

  const loadAccounts = () => {
    accountsApi.listFromAccounts()
      .then((res) => setAccounts(res.data.data ?? []))
      .catch(() => toast.error('Error al cargar cuentas'))
      .finally(() => setLoading(false))
  }

  useEffect(loadAccounts, [])

  const refreshTransfers = () => {
    recentTransfers.forEach((t) => {
      transfersApi.get(t.transferId).then((res) => {
        const updated = res.data.data
        if (updated) setRecentTransfers((prev) => prev.map((p) => p.transferId === updated.transferId ? updated : p))
      })
    })
  }

  const handleCreateAccount = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    try {
      await accountsApi.create(createForm.currency, createForm.pin6, createForm.pin4)
      toast.success('Cuenta creada correctamente')
      setShowCreate(false)
      loadAccounts()
    } catch {
      toast.error('Error al crear cuenta')
    } finally {
      setBusy(false)
    }
  }

  const handleDeposit = async (e: FormEvent) => {
    e.preventDefault()
    if (!showDeposit) return
    setBusy(true)
    try {
      const res = await accountsApi.deposit(showDeposit.accountNumber, showDeposit.currency, depositForm.pin4, depositForm.amount)
      const data = res.data.data
      toast.success(`Depósito exitoso — Saldo: ${currencySymbol[showDeposit.currency] ?? ''}${data?.balanceAfter.toLocaleString()}`)
      setShowDeposit(null)
      loadAccounts()
    } catch (err) {
      toast.error('Error al depositar')
    } finally {
      setBusy(false)
    }
  }

  const handleTransfer = async (e: FormEvent) => {
    e.preventDefault()
    if (!showTransfer) return
    setBusy(true)
    try {
      const res = await transfersApi.create({
        fromAccount: showTransfer.accountNumber,
        toAccount: transferForm.toAccount,
        amount: transferForm.amount,
        currency: showTransfer.currency,
        description: transferForm.description || undefined,
      })
      const data = res.data.data
      if (data) {
        setRecentTransfers((prev) => [data, ...prev].slice(0, 10))
        toast.success('Transferencia iniciada — recarga para ver el resultado')
        setShowTransfer(null)
      }
    } catch {
      toast.error('Error al iniciar la transferencia')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AppLayout>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold text-gray-800">Mis Cuentas</h2>
        <div className="flex gap-2 items-center">
          <span className="text-sm text-gray-400">{accounts.length} cuenta{accounts.length !== 1 ? 's' : ''}</span>
          <Button onClick={() => setShowCreate(true)}>+ Nueva cuenta</Button>
          <button onClick={() => { loadAccounts(); refreshTransfers() }} className="text-xs px-2 py-1 rounded border border-gray-200 hover:bg-gray-100 text-gray-500">
            Recargar
          </button>
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
               onTransfer={setShowTransfer}
            />
          ))}
        </div>
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
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Create account modal */}
      <Modal open={showCreate} onClose={() => setShowCreate(false)} title="Nueva Cuenta">
        <form onSubmit={handleCreateAccount} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Moneda</label>
            <select
              value={createForm.currency}
              onChange={(e) => setCreateForm((f) => ({ ...f, currency: e.target.value }))}
              className="w-full px-3 py-2.5 border border-gray-300 rounded text-base outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
            >
              {currencies.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <Input label="PIN de 6 dígitos" type="password" maxLength={6} value={createForm.pin6} onChange={(e) => setCreateForm((f) => ({ ...f, pin6: e.target.value }))} required />
          <Input label="PIN de 4 dígitos" type="password" maxLength={4} value={createForm.pin4} onChange={(e) => setCreateForm((f) => ({ ...f, pin4: e.target.value }))} required />
          <Button type="submit" disabled={busy}>{busy ? 'Creando...' : 'Crear cuenta'}</Button>
        </form>
      </Modal>

      {/* Deposit modal */}
      <Modal open={!!showDeposit} onClose={() => setShowDeposit(null)} title={`Depositar - ${showDeposit?.accountNumber ?? ''}`}>
        <form onSubmit={handleDeposit} className="flex flex-col gap-4">
          <p className="text-sm text-gray-500">{showDeposit?.currency} — Saldo actual: {currencySymbol[showDeposit?.currency ?? ''] ?? ''}{showDeposit?.balance.toLocaleString()}</p>
          <Input label="Monto" type="number" step="0.01" min="0.01" value={depositForm.amount || ''} onChange={(e) => setDepositForm((f) => ({ ...f, amount: parseFloat(e.target.value) || 0 }))} required />
          <Input label="PIN de 4 dígitos" type="password" maxLength={4} value={depositForm.pin4} onChange={(e) => setDepositForm((f) => ({ ...f, pin4: e.target.value }))} required />
          <Button type="submit" disabled={busy}>{busy ? 'Depositando...' : 'Depositar'}</Button>
        </form>
      </Modal>

      {/* Transfer modal */}
      <Modal open={!!showTransfer} onClose={() => setShowTransfer(null)} title={`Transferir - ${showTransfer?.accountNumber ?? ''}`}>
        <form onSubmit={handleTransfer} className="flex flex-col gap-4">
          <p className="text-sm text-gray-500">Desde: {showTransfer?.accountNumber} ({showTransfer?.currency}) — Saldo: {currencySymbol[showTransfer?.currency ?? ''] ?? ''}{showTransfer?.balance.toLocaleString()}</p>
          <Input label="Cuenta destino (número)" value={transferForm.toAccount} onChange={(e) => setTransferForm((f) => ({ ...f, toAccount: e.target.value }))} required />
          <Input label="Monto" type="number" step="0.01" min="0.01" value={transferForm.amount || ''} onChange={(e) => setTransferForm((f) => ({ ...f, amount: parseFloat(e.target.value) || 0 }))} required />
          <Input label="Descripción (opcional)" value={transferForm.description} onChange={(e) => setTransferForm((f) => ({ ...f, description: e.target.value }))} />
          <Button type="submit" disabled={busy}>
            {busy ? 'Transfiriendo...' : 'Transferir'}
          </Button>
        </form>
      </Modal>
    </AppLayout>
  )
}
