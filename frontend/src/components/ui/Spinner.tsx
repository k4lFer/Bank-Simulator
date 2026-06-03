export default function Spinner({ text = 'Cargando...' }: { text?: string }) {
  return <div className="py-8 text-center text-gray-500">{text}</div>
}
