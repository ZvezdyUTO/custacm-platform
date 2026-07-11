import type {
  AcceptedSummary,
  OjName,
  PageQuery,
  ProblemFirstAcceptedReport,
  ProblemSubmissionReport,
  TrainingQueryRange,
  TrainingUser,
  UserFirstAcceptedReport,
  UserSubmissionReport,
} from '../types';
import { authHeaders, requestData } from './client';

type QueryValue = string | number | null | undefined;

function withQuery(path: string, query: Record<string, QueryValue>): string {
  const search = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== null && value !== undefined && String(value).trim().length > 0) {
      search.set(key, String(value));
    }
  });
  const suffix = search.toString();
  return suffix ? `${path}?${suffix}` : path;
}

function dateStart(value: string): string {
  return value ? `${value}T00:00:00` : '';
}

function dateEnd(value: string): string {
  return value ? `${value}T23:59:59` : '';
}

function authenticatedGet<T>(token: string, path: string, signal?: AbortSignal): Promise<T> {
  return requestData(path, {
    headers: authHeaders(token),
    signal,
  });
}

export function listTrainingUsers(
  token: string,
  signal?: AbortSignal,
): Promise<TrainingUser[]> {
  return authenticatedGet(token, '/player/training-data/users', signal);
}

export function getAcceptedSummary(
  token: string,
  username: string,
  range: TrainingQueryRange,
  ojName: OjName,
  signal?: AbortSignal,
): Promise<AcceptedSummary> {
  return authenticatedGet(token, withQuery('/player/training-data/accepted-summary', {
    ojName,
    username,
    acceptedFromDateUtcPlus8: range.acceptedFromDateUtcPlus8,
    acceptedToDateUtcPlus8: range.acceptedToDateUtcPlus8,
    minProblemRating: range.minProblemRating,
    maxProblemRating: range.maxProblemRating,
  }), signal);
}

export function getUserSubmissions(
  token: string,
  username: string,
  range: TrainingQueryRange,
  page: PageQuery,
  ojName: OjName,
  signal?: AbortSignal,
): Promise<UserSubmissionReport> {
  return authenticatedGet(token, withQuery('/player/training-data/submissions/by-user', {
    ojName,
    username,
    submittedFromUtcPlus8: dateStart(range.acceptedFromDateUtcPlus8),
    submittedToUtcPlus8: dateEnd(range.acceptedToDateUtcPlus8),
    minProblemRating: range.minProblemRating,
    maxProblemRating: range.maxProblemRating,
    page: page.page,
    limit: page.limit,
  }), signal);
}

export function getProblemSubmissions(
  token: string,
  problemKey: string,
  range: TrainingQueryRange,
  page: PageQuery,
  ojName: OjName,
  signal?: AbortSignal,
): Promise<ProblemSubmissionReport> {
  return authenticatedGet(token, withQuery('/player/training-data/submissions/by-problem', {
    ojName,
    problemKey,
    submittedFromUtcPlus8: dateStart(range.acceptedFromDateUtcPlus8),
    submittedToUtcPlus8: dateEnd(range.acceptedToDateUtcPlus8),
    page: page.page,
    limit: page.limit,
  }), signal);
}

export function getUserFirstAccepted(
  token: string,
  username: string,
  range: TrainingQueryRange,
  page: PageQuery,
  ojName: OjName,
  signal?: AbortSignal,
): Promise<UserFirstAcceptedReport> {
  return authenticatedGet(token, withQuery('/player/training-data/first-accepted/by-user', {
    ojName,
    username,
    firstAcceptedFromUtcPlus8: dateStart(range.acceptedFromDateUtcPlus8),
    firstAcceptedToUtcPlus8: dateEnd(range.acceptedToDateUtcPlus8),
    minProblemRating: range.minProblemRating,
    maxProblemRating: range.maxProblemRating,
    page: page.page,
    limit: page.limit,
  }), signal);
}

export function getProblemFirstAccepted(
  token: string,
  problemKey: string,
  range: TrainingQueryRange,
  page: PageQuery,
  ojName: OjName,
  signal?: AbortSignal,
): Promise<ProblemFirstAcceptedReport> {
  return authenticatedGet(token, withQuery('/player/training-data/first-accepted/by-problem', {
    ojName,
    problemKey,
    firstAcceptedFromUtcPlus8: dateStart(range.acceptedFromDateUtcPlus8),
    firstAcceptedToUtcPlus8: dateEnd(range.acceptedToDateUtcPlus8),
    page: page.page,
    limit: page.limit,
  }), signal);
}
