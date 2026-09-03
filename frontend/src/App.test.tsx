import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import type { AiRecommendation, HumanReview, ReviewDecisionType, SupportCase } from './cases'

const cases: SupportCase[] = [
  {
    id: '58ec7b42-8cbb-4ddb-b946-85d3c3bf5d3d',
    organizationId: '00000000-0000-0000-0000-000000000001',
    customerId: '00000000-0000-0000-0000-000000000002',
    subject: 'Production API is down',
    description: 'All API requests have failed with 503 responses since 08:30.',
    category: 'TECHNICAL',
    priority: 'CRITICAL',
    status: 'NEW',
    createdAt: '2026-08-31T13:30:00Z',
    updatedAt: '2026-08-31T13:30:00Z',
  },
  {
    id: '5a832b2d-1387-4097-b9e6-496bb46aeccb',
    organizationId: '00000000-0000-0000-0000-000000000001',
    customerId: '00000000-0000-0000-0000-000000000002',
    subject: 'Invoice question',
    description: 'How can I download the invoice for this month?',
    category: 'BILLING',
    priority: 'LOW',
    status: 'NEW',
    createdAt: '2026-08-31T12:00:00Z',
    updatedAt: '2026-08-31T12:00:00Z',
  },
]

const recommendation: AiRecommendation = {
  id: '12d5ea50-360f-4acf-b4e1-d4b7c8671990',
  organizationId: '00000000-0000-0000-0000-000000000001',
  caseId: cases[0].id,
  version: 1,
  status: 'PENDING_REVIEW',
  draftResponse: 'Please restart the affected API service and confirm that health checks recover.',
  recommendedAction: 'RESPOND_WITH_GUIDANCE',
  confidence: 0.84,
  escalationRequired: false,
  citations: [{
    articleId: 'ec09f9f1-31fd-48be-94ca-a8fe8acded33',
    chunkId: '764f8b50-8a8f-47b9-a734-cde174965da4',
    position: 0,
    title: 'Recovering the production API',
    content: 'Restart the affected API service and verify health checks before closing the incident.',
    sourceUrl: 'https://docs.example.com/api-recovery',
    retrievalScore: 0.77,
  }],
  model: {
    provider: 'local',
    model: 'caseflow-grounded-template-v1',
    configuration: {},
    promptVersion: 'case-recommendation-v1',
    schemaVersion: 'recommendation-output-v1',
    latencyMs: 12,
    inputTokens: null,
    outputTokens: null,
  },
  createdAt: '2026-09-03T13:30:00Z',
}

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('agent triage workspace', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => {
      const url = String(input)
      if (url.includes('/recommendations')) return jsonResponse([])
      return jsonResponse(cases)
    }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads the queue and opens a case for review', async () => {
    const user = userEvent.setup()
    render(<App />)

    expect(await screen.findByRole('button', { name: /production api is down/i })).toBeInTheDocument()
    expect(within(screen.getByText('Open cases').parentElement as HTMLElement).getByText('2')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /invoice question/i }))

    const detail = screen.getByRole('article')
    expect(within(detail).getByRole('heading', { name: 'Invoice question' })).toBeInTheDocument()
    expect(within(detail).getByText('How can I download the invoice for this month?')).toBeInTheDocument()
    expect(within(detail).getByText('Low priority')).toBeInTheDocument()
  })

  it('filters cases by searchable customer text', async () => {
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('button', { name: /production api is down/i })

    await user.type(screen.getByRole('searchbox', { name: 'Search cases' }), 'invoice')

    expect(screen.queryByRole('button', { name: /production api is down/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /invoice question/i })).toBeInTheDocument()
    expect(screen.getByText('1 shown')).toBeInTheDocument()
  })

  it('creates a case and immediately selects it', async () => {
    const user = userEvent.setup()
    const created: SupportCase = {
      ...cases[0],
      id: '82193055-e89f-41e1-a225-eb5d6267a2b9',
      subject: 'Cannot access account',
      description: 'The customer is locked out after resetting their password.',
      category: 'ACCESS',
      priority: 'HIGH',
    }
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockImplementationOnce(() => jsonResponse([]))
      .mockImplementationOnce(() => jsonResponse(created, 201))

    render(<App />)
    await screen.findByText('The queue is clear')
    await user.click(screen.getByRole('button', { name: /new case/i }))
    await user.type(screen.getByLabelText('Subject'), created.subject)
    await user.type(screen.getByLabelText('Description'), created.description)
    await user.click(screen.getByRole('button', { name: 'Create case' }))

    expect(await screen.findByRole('heading', { name: created.subject })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/cases',
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining(created.subject),
      }),
    )
  })

  it('records an approval without presenting it as a sent response', async () => {
    const user = userEvent.setup()
    const approved = reviewResult('APPROVED', recommendation.draftResponse)
    const fetchMock = installReviewFetch(approved)

    render(<App />)
    expect(await screen.findByRole('heading', { name: 'AI recommendation' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Approve draft' }))

    expect(await screen.findByRole('heading', { name: 'Recommendation reviewed' })).toBeInTheDocument()
    expect(screen.getByText('Reviewed response · not sent')).toBeInTheDocument()
    expect(screen.getByText('No customer reply was sent and the case status was not changed.')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenLastCalledWith(
      `/api/v1/recommendations/${recommendation.id}/review`,
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"decision":"APPROVED"'),
      }),
    )
  })

  it('records the human-edited response instead of the AI draft', async () => {
    const user = userEvent.setup()
    const editedResponse = 'Please restart the API service, then share the health-check result with us.'
    const edited = reviewResult('EDITED', editedResponse)
    const fetchMock = installReviewFetch(edited)

    render(<App />)
    const draft = await screen.findByRole('textbox', { name: 'Customer response draft' })
    await user.clear(draft)
    await user.type(draft, editedResponse)
    await user.click(screen.getByRole('button', { name: 'Save edited response' }))

    expect(await screen.findByText(editedResponse)).toBeInTheDocument()
    expect(fetchMock).toHaveBeenLastCalledWith(
      `/api/v1/recommendations/${recommendation.id}/review`,
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"decision":"EDITED"'),
      }),
    )
  })

  it('requires a reason before recording a rejection', async () => {
    const user = userEvent.setup()
    const reason = 'The cited recovery procedure does not apply to this service.'
    const rejected = reviewResult('REJECTED', null, reason)
    const fetchMock = installReviewFetch(rejected)

    render(<App />)
    expect(await screen.findByRole('heading', { name: 'AI recommendation' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reject recommendation' }))
    const confirm = screen.getByRole('button', { name: 'Confirm rejection' })
    expect(confirm).toBeDisabled()
    await user.type(screen.getByRole('textbox', { name: 'Reason for rejection' }), reason)
    await user.click(confirm)

    expect(await screen.findByText(reason)).toBeInTheDocument()
    expect(fetchMock).toHaveBeenLastCalledWith(
      `/api/v1/recommendations/${recommendation.id}/review`,
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"decision":"REJECTED"'),
      }),
    )
  })
})

function reviewResult(
  decision: ReviewDecisionType,
  finalResponse: string | null,
  rejectionReason: string | null = null,
): HumanReview {
  return {
    id: '2fcb3194-d6aa-4c48-b6bf-2b4557c2da66',
    organizationId: recommendation.organizationId,
    recommendationId: recommendation.id,
    caseId: recommendation.caseId,
    reviewerId: '00000000-0000-0000-0000-000000000003',
    reviewerName: 'Demo Agent',
    decision,
    finalResponse,
    rejectionReason,
    reviewLatencyMs: 90000,
    createdAt: '2026-09-03T13:31:30Z',
  }
}

function installReviewFetch(review: HumanReview) {
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    if (url.includes(`/recommendations/${recommendation.id}/review`) && init?.method === 'POST') {
      return jsonResponse(review, 201)
    }
    if (url.includes(`/recommendations/${recommendation.id}/review`)) {
      return jsonResponse({ detail: 'No review found' }, 404)
    }
    if (url.includes(`/cases/${recommendation.caseId}/recommendations`)) {
      return jsonResponse([recommendation])
    }
    return jsonResponse(cases)
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}
