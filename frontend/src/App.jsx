import { useState, useEffect } from 'react'
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

function GradeBadge({ grade }) {
  const cls = {
    LT: 'grade-lt',
    LP: 'grade-lp',
    EP: 'grade-ep',
    I: 'grade-i',
  }[grade] || 'grade-default'
  const label = {
    LT: 'LT',
    LP: 'LP',
    EP: 'EP',
    I: 'I',
  }[grade] || grade
  return <span className={`badge ${cls}`}>{label}</span>
}

function AreaCard({ area }) {
  return (
    <motion.div
      className="area-card"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <h3 className="area-title">{area.name}</h3>
      <div className="indicators">
        {area.indicators.map((ind, i) => (
            <div key={i} className="indicator-row" style={{ display: 'flex', alignItems: '-moz-initial', gap: '8px', justifyContent: 'stretch' }}>
              <span className="ind-name">{ind.name}</span>
            <GradeBadge grade={ind.grade} />
          </div>
        ))}
      </div>
    </motion.div>
  )
}

function Dashboard({ rawName }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!rawName) return
    setLoading(true)
    setData(null)
    setError(null)
    fetch(`/student-data?name=${encodeURIComponent(rawName)}`)
      .then(r => r.ok ? r.json() : r.text().then(t => { throw new Error(t) }))
      .then(setData)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [rawName])

  if (loading) return <div className="dashboard-loading"><span className="spinner" /> Cargando notas...</div>
  if (error) return <div className="dashboard-error">Error: {error}</div>
  if (!data) return null

  return (
    <motion.div
      className="dashboard"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
    >
      <div className="dashboard-header">
        <h2 style = {{ color: '#333' }}>{data.name}</h2>
        <span className="literal-badge">{data.literal}</span>
        <span className="momento">{data.momento}</span>
      </div>
      <div className="areas-grid">
        {data.areas.map((area, i) => (
          <AreaCard key={i} area={area} />
        ))}
      </div>
    </motion.div>
  )
}

function App() {
  const [students, setStudents] = useState([])
  const [selectedRaw, setSelectedRaw] = useState('')
  const [report, setReport] = useState('')
  const [loading, setLoading] = useState(false)
  const [reportLoading, setReportLoading] = useState(false)
  const [showDashboard, setShowDashboard] = useState(false)

  const handleUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    setLoading(true)
    setStudents([])
    setSelectedRaw('')
    setReport('')
    setShowDashboard(false)
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

  const handleSelect = (e) => {
    const idx = e.target.value
    if (!idx) { setSelectedRaw(''); setShowDashboard(false); return }
    setSelectedRaw(students[idx].raw)
    setShowDashboard(false)
    setReport('')
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

  const selectedFormatted = selectedRaw
    ? students.find(s => s.raw === selectedRaw)?.formatted
    : ''

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
            <select value={selectedRaw ? students.findIndex(s => s.raw === selectedRaw) : ''} onChange={handleSelect}>
              <option value="">— Seleccionar estudiante —</option>
              {students.map((s, i) => (
                <option key={i} value={i}>{s.formatted}</option>
              ))}
            </select>
            <div className="select-actions">
              <motion.button
                className="generate-btn"
                onClick={() => setShowDashboard(true)}
                disabled={!selectedRaw}
                whileHover={selectedRaw ? { scale: 1.05 } : {}}
                whileTap={selectedRaw ? { scale: 0.95 } : {}}
              >
                Ver Notas
              </motion.button>
              <motion.button
                className="generate-btn primary"
                onClick={handleGenerate}
                disabled={!selectedRaw || reportLoading}
                whileHover={selectedRaw && !reportLoading ? { scale: 1.05 } : {}}
                whileTap={selectedRaw && !reportLoading ? { scale: 0.95 } : {}}
              >
                {reportLoading ? (
                  <><span className="spinner small" /> Generando...</>
                ) : 'Generar Reporte'}
              </motion.button>
            </div>
          </motion.section>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {selectedRaw && showDashboard && <Dashboard rawName={selectedRaw} key={selectedRaw} />}
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
