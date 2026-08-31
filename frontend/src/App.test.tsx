import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import type { SupportCase } from './cases'

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
    vi.stubGlobal('fetch', vi.fn(() => jsonResponse(cases)))
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
    expect(fetchMock).toHaveBeenLastCalledWith(
      '/api/v1/cases',
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining(created.subject),
      }),
    )
  })
})
