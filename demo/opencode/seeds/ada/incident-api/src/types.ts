export const severities = ["critical", "major", "minor"] as const;
export type Severity = (typeof severities)[number];

export const incidentStates = ["investigating", "identified", "monitoring", "resolved"] as const;
export type IncidentState = (typeof incidentStates)[number];

export interface Service {
  id: string;
  name: string;
  tier: 1 | 2 | 3;
  owner: string;
  region: string;
}

export interface IncidentUpdate {
  state: IncidentState;
  message: string;
  createdAt: string;
}

export interface Incident {
  id: string;
  serviceId: string;
  title: string;
  severity: Severity;
  state: IncidentState;
  startedAt: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  updates: IncidentUpdate[];
}

export interface IncidentSummary extends Omit<Incident, "updates"> {
  latestMessage: string;
  updateCount: number;
}

export interface IncidentFilters {
  serviceId?: string;
  state?: IncidentState;
  limit?: number;
}
