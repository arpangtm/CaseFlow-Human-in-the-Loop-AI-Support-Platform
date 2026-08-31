import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import {
  CaseApiError,
  createCase,
  listCases,
} from './cases'
import type { CasePriority, SupportCase } from './cases'

type PriorityFilter = 'ALL' | CasePriority

const dateFormatter = new Intl.DateTimeFormat('en', {
  month: 'short',
  day: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
})

function formatDate(value: string) {
  return dateFormatter.format(new Date(value))
}

function readableLabel(value: string) {
  return value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ')
}

function App() {
  const [cases, setCases] = useState<SupportCase[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [priority, setPriority] = useState<PriorityFilter>('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isComposerOpen, setComposerOpen] = useState(false)

  async function loadCases(signal?: AbortSignal) {
    setLoading(true)
    setError(null)
    try {
      const result = await listCases(signal)
      setCases(result)
      setSelectedId((current) => current ?? result[0]?.id ?? null)
    } catch (loadError) {
      if (loadError instanceof DOMException && loadError.name === 'AbortError') return
      setError(loadError instanceof CaseApiError ? loadError.message : 'Cases could not be loaded.')
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }

  useEffect(() => {
    const controller = new AbortController()

    async function loadInitialCases() {
      try {
        const result = await listCases(controller.signal)
        setCases(result)
        setSelectedId(result[0]?.id ?? null)
      } catch (loadError) {
        if (loadError instanceof DOMException && loadError.name === 'AbortError') return
        setError(loadError instanceof CaseApiError ? loadError.message : 'Cases could not be loaded.')
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    void loadInitialCases()
    return () => controller.abort()
  }, [])

  const filteredCases = useMemo(() => {
    const query = search.trim().toLowerCase()
    return cases.filter((supportCase) => {
      const matchesPriority = priority === 'ALL' || supportCase.priority === priority
      const matchesSearch =
        query.length === 0 ||
        supportCase.subject.toLowerCase().includes(query) ||
        supportCase.description.toLowerCase().includes(query)
      return matchesPriority && matchesSearch
    })
  }, [cases, priority, search])

  const selectedCase = cases.find((supportCase) => supportCase.id === selectedId) ?? null
  const attentionCount = cases.filter(
    (supportCase) => supportCase.priority === 'CRITICAL' || supportCase.priority === 'HIGH',
  ).length
  const openCount = cases.filter(
    (supportCase) => supportCase.status === 'NEW' || supportCase.status === 'IN_REVIEW',
  ).length

  function handleCreated(created: SupportCase) {
    setCases((current) => [created, ...current])
    setSelectedId(created.id)
    setSearch('')
    setPriority('ALL')
    setComposerOpen(false)
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">C</span>
          <span>CaseFlow</span>
        </div>
        <div className="workspace-identity">
          <span className="organization-name">CaseFlow Demo</span>
          <span className="avatar" aria-label="Signed in agent">AG</span>
        </div>
      </header>

      <main className="workspace">
        <section className="workspace-heading" aria-labelledby="queue-heading">
          <div>
            <p className="eyebrow">Agent workspace</p>
            <h1 id="queue-heading">Support queue</h1>
            <p>Prioritize new requests and understand the customer issue before taking action.</p>
          </div>
          <button className="primary-button" type="button" onClick={() => setComposerOpen(true)}>
            <span aria-hidden="true">＋</span> New case
          </button>
        </section>

        <section className="summary-row" aria-label="Queue summary">
          <div className="summary-card">
            <span className="summary-label">Open cases</span>
            <strong>{openCount}</strong>
            <span className="summary-note">Awaiting resolution</span>
          </div>
          <div className="summary-card attention">
            <span className="summary-label">Needs attention</span>
            <strong>{attentionCount}</strong>
            <span className="summary-note">High or critical priority</span>
          </div>
          <div className="summary-card">
            <span className="summary-label">Untriaged</span>
            <strong>{cases.filter((supportCase) => supportCase.status === 'NEW').length}</strong>
            <span className="summary-note">Newly submitted</span>
          </div>
        </section>

        <section className="triage-layout">
          <div className="queue-panel">
            <div className="panel-heading">
              <div>
                <h2>Cases</h2>
                <span>{filteredCases.length} shown</span>
              </div>
              <button className="icon-button" type="button" onClick={() => void loadCases()} aria-label="Refresh cases">
                ↻
              </button>
            </div>

            <div className="queue-tools">
              <label className="search-field">
                <span className="visually-hidden">Search cases</span>
                <span aria-hidden="true">⌕</span>
                <input
                  type="search"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Search subject or description"
                />
              </label>
              <label className="filter-field">
                <span className="visually-hidden">Filter by priority</span>
                <select value={priority} onChange={(event) => setPriority(event.target.value as PriorityFilter)}>
                  <option value="ALL">All priorities</option>
                  <option value="CRITICAL">Critical</option>
                  <option value="HIGH">High</option>
                  <option value="NORMAL">Normal</option>
                  <option value="LOW">Low</option>
                </select>
              </label>
            </div>

            <div className="case-list" aria-live="polite">
              {loading ? <QueueSkeleton /> : null}
              {!loading && error ? (
                <div className="state-message error-state">
                  <strong>We couldn’t load the queue.</strong>
                  <p>{error}</p>
                  <button type="button" onClick={() => void loadCases()}>Try again</button>
                </div>
              ) : null}
              {!loading && !error && filteredCases.length === 0 ? (
                <div className="state-message">
                  <span className="empty-mark" aria-hidden="true">✓</span>
                  <strong>{cases.length === 0 ? 'The queue is clear' : 'No matching cases'}</strong>
                  <p>{cases.length === 0 ? 'New customer requests will appear here.' : 'Try a different search or priority.'}</p>
                </div>
              ) : null}
              {!loading && !error
                ? filteredCases.map((supportCase) => (
                    <CaseListItem
                      key={supportCase.id}
                      supportCase={supportCase}
                      selected={supportCase.id === selectedId}
                      onSelect={() => setSelectedId(supportCase.id)}
                    />
                  ))
                : null}
            </div>
          </div>

          <div className="detail-panel">
            {selectedCase ? <CaseDetail supportCase={selectedCase} /> : <DetailEmptyState />}
          </div>
        </section>
      </main>

      {isComposerOpen ? (
        <NewCaseDialog onClose={() => setComposerOpen(false)} onCreated={handleCreated} />
      ) : null}
    </div>
  )
}

function CaseListItem({
  supportCase,
  selected,
  onSelect,
}: {
  supportCase: SupportCase
  selected: boolean
  onSelect: () => void
}) {
  return (
    <button
      type="button"
      className={`case-row${selected ? ' selected' : ''}`}
      onClick={onSelect}
      aria-pressed={selected}
    >
      <span className={`priority-dot ${supportCase.priority.toLowerCase()}`} aria-hidden="true" />
      <span className="case-copy">
        <span className="case-row-topline">
          <strong>{supportCase.subject}</strong>
          <time dateTime={supportCase.createdAt}>{formatDate(supportCase.createdAt)}</time>
        </span>
        <span className="case-preview">{supportCase.description}</span>
        <span className="case-metadata">
          <span>{readableLabel(supportCase.category)}</span>
          <span>{readableLabel(supportCase.priority)}</span>
          <span>{readableLabel(supportCase.status)}</span>
        </span>
      </span>
      <span className="chevron" aria-hidden="true">›</span>
    </button>
  )
}

function CaseDetail({ supportCase }: { supportCase: SupportCase }) {
  return (
    <article className="case-detail">
      <header className="detail-heading">
        <div className="detail-badges">
          <span className={`badge priority-${supportCase.priority.toLowerCase()}`}>
            {readableLabel(supportCase.priority)} priority
          </span>
          <span className="badge neutral">{readableLabel(supportCase.status)}</span>
        </div>
        <h2>{supportCase.subject}</h2>
        <p className="case-reference">Case {supportCase.id.slice(0, 8).toUpperCase()}</p>
      </header>

      <section className="detail-section">
        <h3>Customer request</h3>
        <p className="issue-description">{supportCase.description}</p>
      </section>

      <section className="customer-card" aria-label="Customer context">
        <span className="customer-avatar" aria-hidden="true">DC</span>
        <div>
          <strong>Demo Customer</strong>
          <span>customer@example.com</span>
        </div>
        <span className="customer-tag">CaseFlow Demo</span>
      </section>

      <section className="detail-grid" aria-label="Case details">
        <div>
          <span>Category</span>
          <strong>{readableLabel(supportCase.category)}</strong>
        </div>
        <div>
          <span>Submitted</span>
          <strong>{formatDate(supportCase.createdAt)}</strong>
        </div>
        <div>
          <span>Last updated</span>
          <strong>{formatDate(supportCase.updatedAt)}</strong>
        </div>
        <div>
          <span>Owner</span>
          <strong>Unassigned</strong>
        </div>
      </section>
    </article>
  )
}

function DetailEmptyState() {
  return (
    <div className="detail-empty">
      <span aria-hidden="true">→</span>
      <h2>Select a case</h2>
      <p>Choose a request from the queue to review its details.</p>
    </div>
  )
}

function QueueSkeleton() {
  return (
    <div className="skeleton-list" aria-label="Loading cases">
      {[0, 1, 2].map((item) => (
        <div className="skeleton-row" key={item}>
          <span />
          <div><span /><span /><span /></div>
        </div>
      ))}
    </div>
  )
}

function NewCaseDialog({
  onClose,
  onCreated,
}: {
  onClose: () => void
  onCreated: (supportCase: SupportCase) => void
}) {
  const [subject, setSubject] = useState('')
  const [description, setDescription] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const created = await createCase({ subject: subject.trim(), description: description.trim() })
      onCreated(created)
    } catch (submitError) {
      setError(submitError instanceof CaseApiError ? submitError.message : 'The case could not be submitted.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose()
    }}>
      <section className="case-dialog" role="dialog" aria-modal="true" aria-labelledby="new-case-title">
        <header>
          <div>
            <p className="eyebrow">Customer intake</p>
            <h2 id="new-case-title">Create support case</h2>
          </div>
          <button className="close-button" type="button" onClick={onClose} aria-label="Close new case form">×</button>
        </header>
        <form onSubmit={handleSubmit}>
          <label>
            <span>Subject</span>
            <input
              autoFocus
              required
              maxLength={200}
              value={subject}
              onChange={(event) => setSubject(event.target.value)}
              placeholder="Briefly describe the issue"
            />
          </label>
          <label>
            <span>Description</span>
            <textarea
              required
              maxLength={10000}
              rows={7}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Include what happened, who is affected, and any error messages."
            />
          </label>
          <div className="classification-note">
            <span aria-hidden="true">✦</span>
            Category and priority are assigned automatically after submission.
          </div>
          {error ? <p className="form-error" role="alert">{error}</p> : null}
          <footer>
            <button className="secondary-button" type="button" onClick={onClose}>Cancel</button>
            <button className="primary-button" type="submit" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create case'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  )
}

export default App
