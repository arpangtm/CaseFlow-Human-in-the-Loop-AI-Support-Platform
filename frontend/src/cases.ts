export const DEMO_ORGANIZATION_ID = '00000000-0000-0000-0000-000000000001'
export const DEMO_CUSTOMER_ID = '00000000-0000-0000-0000-000000000002'

export type CaseCategory = 'ACCESS' | 'BILLING' | 'TECHNICAL' | 'GENERAL'
export type CasePriority = 'LOW' | 'NORMAL' | 'HIGH' | 'CRITICAL'
export type CaseStatus = 'NEW' | 'IN_REVIEW' | 'RESOLVED' | 'CLOSED'

export interface SupportCase {
  id: string
  organizationId: string
  customerId: string
  subject: string
  description: string
  category: CaseCategory
  priority: CasePriority
  status: CaseStatus
  createdAt: string
  updatedAt: string
}

export interface CreateCaseInput {
  subject: string
  description: string
}

export class CaseApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'CaseApiError'
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new CaseApiError(
      response.status >= 500
        ? 'CaseFlow is temporarily unavailable. Please try again.'
        : 'The case request could not be completed.',
    )
  }
  return response.json() as Promise<T>
}

export async function listCases(signal?: AbortSignal): Promise<SupportCase[]> {
  const query = new URLSearchParams({ organizationId: DEMO_ORGANIZATION_ID })
  const response = await fetch(`/api/v1/cases?${query.toString()}`, { signal })
  return parseResponse<SupportCase[]>(response)
}

export async function createCase(input: CreateCaseInput): Promise<SupportCase> {
  const response = await fetch('/api/v1/cases', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      organizationId: DEMO_ORGANIZATION_ID,
      customerId: DEMO_CUSTOMER_ID,
      subject: input.subject,
      description: input.description,
    }),
  })
  return parseResponse<SupportCase>(response)
}
