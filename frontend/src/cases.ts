export const DEMO_ORGANIZATION_ID = '00000000-0000-0000-0000-000000000001'
export const DEMO_CUSTOMER_ID = '00000000-0000-0000-0000-000000000002'
export const DEMO_REVIEWER_ID = '00000000-0000-0000-0000-000000000003'

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

export interface RecommendationCitation {
  articleId: string
  chunkId: string
  position: number
  title: string
  content: string
  sourceUrl: string | null
  retrievalScore: number
}

export interface RecommendationModel {
  provider: string
  model: string
  configuration: Record<string, unknown>
  promptVersion: string
  schemaVersion: string
  latencyMs: number
  inputTokens: number | null
  outputTokens: number | null
}

export interface AiRecommendation {
  id: string
  organizationId: string
  caseId: string
  version: number
  status: 'PENDING_REVIEW' | 'REVIEWED'
  draftResponse: string
  recommendedAction: 'RESPOND_WITH_GUIDANCE' | 'REQUEST_INFORMATION' | 'ESCALATE' | 'NO_ACTION'
  confidence: number
  escalationRequired: boolean
  citations: RecommendationCitation[]
  model: RecommendationModel
  createdAt: string
}

export type ReviewDecisionType = 'APPROVED' | 'EDITED' | 'REJECTED'

export interface HumanReview {
  id: string
  organizationId: string
  recommendationId: string
  caseId: string
  reviewerId: string
  reviewerName: string
  decision: ReviewDecisionType
  finalResponse: string | null
  rejectionReason: string | null
  reviewLatencyMs: number
  createdAt: string
}

export interface SubmitReviewInput {
  decision: ReviewDecisionType
  editedResponse?: string
  rejectionReason?: string
}

export class CaseApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'CaseApiError'
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let detail: string | undefined
    try {
      detail = (await response.json() as { detail?: string }).detail
    } catch {
      detail = undefined
    }
    throw new CaseApiError(
      detail ?? (response.status >= 500
        ? 'CaseFlow is temporarily unavailable. Please try again.'
        : 'The case request could not be completed.'),
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

export async function listRecommendations(
  caseId: string,
  signal?: AbortSignal,
): Promise<AiRecommendation[]> {
  const query = new URLSearchParams({ organizationId: DEMO_ORGANIZATION_ID })
  const response = await fetch(`/api/v1/cases/${caseId}/recommendations?${query.toString()}`, { signal })
  return parseResponse<AiRecommendation[]>(response)
}

export async function generateRecommendation(caseId: string): Promise<AiRecommendation> {
  const query = new URLSearchParams({ organizationId: DEMO_ORGANIZATION_ID })
  const response = await fetch(`/api/v1/cases/${caseId}/recommendations?${query.toString()}`, {
    method: 'POST',
  })
  return parseResponse<AiRecommendation>(response)
}

export async function getReview(
  recommendationId: string,
  signal?: AbortSignal,
): Promise<HumanReview | null> {
  const query = new URLSearchParams({ organizationId: DEMO_ORGANIZATION_ID })
  const response = await fetch(`/api/v1/recommendations/${recommendationId}/review?${query.toString()}`, {
    signal,
  })
  if (response.status === 404) return null
  return parseResponse<HumanReview>(response)
}

export async function submitReview(
  recommendationId: string,
  input: SubmitReviewInput,
): Promise<HumanReview> {
  const response = await fetch(`/api/v1/recommendations/${recommendationId}/review`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      organizationId: DEMO_ORGANIZATION_ID,
      reviewerId: DEMO_REVIEWER_ID,
      decision: input.decision,
      editedResponse: input.editedResponse,
      rejectionReason: input.rejectionReason,
    }),
  })
  return parseResponse<HumanReview>(response)
}
