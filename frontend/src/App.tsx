import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import {
  CaseApiError,
  createCase,
  generateRecommendation,
  getReview,
  listCases,
  listRecommendations,
  submitReview,
} from './cases'
import type { AiRecommendation, CasePriority, HumanReview, ReviewDecisionType, SupportCase } from './cases'

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
            {selectedCase ? <CaseDetail key={selectedCase.id} supportCase={selectedCase} /> : <DetailEmptyState />}
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

      <CaseReviewPanel caseId={supportCase.id} />
    </article>
  )
}

function CaseReviewPanel({ caseId }: { caseId: string }) {
  const [recommendation, setRecommendation] = useState<AiRecommendation | null>(null)
  const [review, setReview] = useState<HumanReview | null>(null)
  const [draft, setDraft] = useState('')
  const [rejectionReason, setRejectionReason] = useState('')
  const [showRejection, setShowRejection] = useState(false)
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    async function loadRecommendation() {
      try {
        const recommendations = await listRecommendations(caseId, controller.signal)
        const latest = recommendations[0] ?? null
        setRecommendation(latest)
        setDraft(latest?.draftResponse ?? '')
        if (latest) {
          setReview(await getReview(latest.id, controller.signal))
        }
      } catch (loadError) {
        if (loadError instanceof DOMException && loadError.name === 'AbortError') return
        setError(loadError instanceof CaseApiError ? loadError.message : 'Review data could not be loaded.')
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    void loadRecommendation()
    return () => controller.abort()
  }, [caseId])

  async function handleGenerate() {
    setWorking(true)
    setError(null)
    try {
      const generated = await generateRecommendation(caseId)
      setRecommendation(generated)
      setDraft(generated.draftResponse)
      setReview(null)
    } catch (generationError) {
      setError(generationError instanceof CaseApiError
        ? generationError.message
        : 'A recommendation could not be generated.')
    } finally {
      setWorking(false)
    }
  }

  async function handleReview(decision: ReviewDecisionType) {
    if (!recommendation) return
    setWorking(true)
    setError(null)
    try {
      const recorded = await submitReview(recommendation.id, {
        decision,
        editedResponse: decision === 'EDITED' ? draft.trim() : undefined,
        rejectionReason: decision === 'REJECTED' ? rejectionReason.trim() : undefined,
      })
      setReview(recorded)
      setShowRejection(false)
    } catch (reviewError) {
      setError(reviewError instanceof CaseApiError ? reviewError.message : 'The review could not be recorded.')
    } finally {
      setWorking(false)
    }
  }

  if (loading) {
    return <div className="review-loading" aria-label="Loading recommendation"><span /><span /><span /></div>
  }

  if (!recommendation) {
    return (
      <section className="recommendation-panel" aria-labelledby="recommendation-title">
        <div className="recommendation-heading">
          <div>
            <p className="eyebrow">Human-in-the-loop</p>
            <h3 id="recommendation-title">AI recommendation</h3>
          </div>
          <span className="review-status neutral">Not generated</span>
        </div>
        <p className="recommendation-intro">
          Retrieve verified guidance and prepare a draft for a human decision.
        </p>
        {error ? <p className="form-error" role="alert">{error}</p> : null}
        <button className="primary-button" type="button" onClick={() => void handleGenerate()} disabled={working}>
          {working ? 'Generating…' : 'Generate recommendation'}
        </button>
      </section>
    )
  }

  if (review) {
    return <CompletedReview recommendation={recommendation} review={review} />
  }

  const draftChanged = draft.trim() !== recommendation.draftResponse.trim()
  const confidence = Math.round(recommendation.confidence * 100)

  return (
    <section className="recommendation-panel" aria-labelledby="recommendation-title">
      <div className="recommendation-heading">
        <div>
          <p className="eyebrow">Human-in-the-loop</p>
          <h3 id="recommendation-title">AI recommendation</h3>
        </div>
        <span className="review-status pending">Awaiting review</span>
      </div>

      <div className="recommendation-callout">
        <span aria-hidden="true">✦</span>
        <p>AI-generated draft. Verify the response and cited evidence before recording a decision.</p>
      </div>

      <div className="recommendation-facts" aria-label="Recommendation details">
        <div><span>Suggested action</span><strong>{readableLabel(recommendation.recommendedAction)}</strong></div>
        <div><span>Confidence</span><strong>{confidence}%</strong></div>
        <div><span>Version</span><strong>{recommendation.version}</strong></div>
      </div>

      <label className="draft-field">
        <span>Customer response draft</span>
        <textarea
          rows={7}
          maxLength={20000}
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
        />
      </label>

      <div className="evidence-list" aria-label="Cited evidence">
        <div className="evidence-heading">
          <strong>Cited evidence</strong>
          <span>{recommendation.citations.length} sources</span>
        </div>
        {recommendation.citations.length === 0 ? (
          <p>No verified evidence was found. The suggested action is to escalate.</p>
        ) : recommendation.citations.map((citation) => (
          <details key={citation.chunkId}>
            <summary>{citation.title}</summary>
            <p>{citation.content}</p>
            {citation.sourceUrl ? <a href={citation.sourceUrl} target="_blank" rel="noreferrer">Open source</a> : null}
          </details>
        ))}
      </div>

      {showRejection ? (
        <div className="rejection-box">
          <label>
            <span>Reason for rejection</span>
            <textarea
              rows={3}
              maxLength={2000}
              value={rejectionReason}
              onChange={(event) => setRejectionReason(event.target.value)}
              placeholder="Explain why this recommendation should not be used."
            />
          </label>
          <div>
            <button className="secondary-button" type="button" onClick={() => setShowRejection(false)}>Cancel</button>
            <button
              className="danger-button"
              type="button"
              disabled={working || rejectionReason.trim().length === 0}
              onClick={() => void handleReview('REJECTED')}
            >
              {working ? 'Recording…' : 'Confirm rejection'}
            </button>
          </div>
        </div>
      ) : null}

      {error ? <p className="form-error" role="alert">{error}</p> : null}
      <div className="review-actions">
        <div>
          <button className="primary-button" type="button" disabled={working || draftChanged} onClick={() => void handleReview('APPROVED')}>
            {working ? 'Recording…' : 'Approve draft'}
          </button>
          <button className="secondary-button" type="button" disabled={working || !draftChanged || draft.trim().length === 0} onClick={() => void handleReview('EDITED')}>
            Save edited response
          </button>
        </div>
        <button className="text-danger-button" type="button" disabled={working} onClick={() => setShowRejection(true)}>
          Reject recommendation
        </button>
      </div>
      <p className="safety-note">Recording a decision does not send a customer reply or resolve the case.</p>
    </section>
  )
}

function CompletedReview({
  recommendation,
  review,
}: {
  recommendation: AiRecommendation
  review: HumanReview
}) {
  return (
    <section className="recommendation-panel completed-review" aria-labelledby="recommendation-title">
      <div className="recommendation-heading">
        <div>
          <p className="eyebrow">Human decision</p>
          <h3 id="recommendation-title">Recommendation reviewed</h3>
        </div>
        <span className={`review-status ${review.decision.toLowerCase()}`}>{readableLabel(review.decision)}</span>
      </div>
      <p className="review-byline">
        Recorded by <strong>{review.reviewerName}</strong> on {formatDate(review.createdAt)}
      </p>
      {review.finalResponse ? (
        <div className="review-result">
          <span>Reviewed response · not sent</span>
          <p>{review.finalResponse}</p>
        </div>
      ) : (
        <div className="review-result rejected-result">
          <span>Rejection reason</span>
          <p>{review.rejectionReason}</p>
        </div>
      )}
      <div className="recommendation-footnote">
        Recommendation version {recommendation.version} · {readableLabel(recommendation.recommendedAction)}
      </div>
      <p className="safety-note">No customer reply was sent and the case status was not changed.</p>
    </section>
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
