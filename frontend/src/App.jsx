import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import './App.css'

function CopyButton({ text }) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    await navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <motion.button
      className="copy-btn"
      onClick={handleCopy}
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.95 }}
    >
      <AnimatePresence mode="wait">
        <motion.span
          key={copied ? 'copied' : 'copy'}
          initial={{ y: -10, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: 10, opacity: 0 }}
          transition={{ duration: 0.15 }}
        >
          {copied ? '✓ Copiado' : 'Copiar'}
        </motion.span>
      </AnimatePresence>
    </motion.button>
  )
}

function App() {
  const [students, setStudents] = useState([])
  const [selectedRaw, setSelectedRaw] = useState('')
  const [report, setReport] = useState('')
  const [loading, setLoading] = useState(false)
  const [reportLoading, setReportLoading] = useState(false)

  const handleUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    setLoading(true)
    setStudents([])
    setSelectedRaw('')
    setReport('')
    const fd = new FormData()
    fd.append('file', file)
    try {
      const res = await fetch('/upload', { method: 'POST', body: fd })
      if (!res.ok) throw new Error(await res.text())
      setStudents(await res.json())
    } catch (err) {
      alert('Error al subir: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleGenerate = async () => {
    if (!selectedRaw) return
    setReportLoading(true)
    setReport('')
    try {
      const res = await fetch('/student-report', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: selectedRaw }),
      })
      if (!res.ok) throw new Error(await res.text())
      const data = await res.json()
      setReport(data.report)
    } catch (err) {
      alert('Error al generar: ' + err.message)
    } finally {
      setReportLoading(false)
    }
  }

  return (
    <div className="app">
      <motion.h1
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        Generador de Reportes Pedagógicos
      </motion.h1>

      <motion.section
        className="card upload-card"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
      >
        <label className={`file-label ${loading ? 'loading' : ''}`}>
          <input type="file" accept=".xlsx" onChange={handleUpload} />
          {loading ? (
            <span className="spinner" />
          ) : (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          )}
          <span>{loading ? 'Cargando...' : 'Subir notas.xlsx'}</span>
        </label>
        <AnimatePresence>
          {students.length > 0 && (
            <motion.p
              className="count"
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
            >
              {students.length} estudiantes encontrados
            </motion.p>
          )}
        </AnimatePresence>
      </motion.section>

      <AnimatePresence>
        {students.length > 0 && (
          <motion.section
            className="card select-card"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            transition={{ delay: 0.2 }}
          >
            <select value={selectedRaw ? students.findIndex(s => s.raw === selectedRaw) : ''} onChange={(e) => {
              const idx = e.target.value
              if (!idx) { setSelectedRaw(''); return }
              setSelectedRaw(students[idx].raw)
            }}>
              <option value="">— Seleccionar estudiante —</option>
              {students.map((s, i) => (
                <option key={i} value={i}>{s.formatted}</option>
              ))}
            </select>
            <motion.button
              className="generate-btn"
              onClick={handleGenerate}
              disabled={!selectedRaw || reportLoading}
              whileHover={selectedRaw && !reportLoading ? { scale: 1.05 } : {}}
              whileTap={selectedRaw && !reportLoading ? { scale: 0.95 } : {}}
            >
              {reportLoading ? (
                <><span className="spinner small" /> Generando...</>
              ) : 'Generar Reporte'}
            </motion.button>
          </motion.section>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {report && (
          <motion.section
            className="card report-card"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            transition={{ delay: 0.3 }}
          >
            <textarea value={report} readOnly rows={10} />
            <CopyButton text={report} />
          </motion.section>
        )}
      </AnimatePresence>
    </div>
  )
}

export default App
