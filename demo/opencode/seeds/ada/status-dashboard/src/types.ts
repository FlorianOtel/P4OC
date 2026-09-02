export type ServiceStatus = "operational" | "degraded" | "outage" | "maintenance";

export interface ServiceStatusItem {
  id: string;
  name: string;
  description: string;
  region: string;
  status: ServiceStatus;
  uptime30d: number;
}

export interface PublicIncidentUpdate {
  message: string;
  publishedAt: string;
}

export interface PublicIncident {
  id: string;
  title: string;
  severity: "critical" | "major" | "minor";
  state: "investigating" | "identified" | "monitoring" | "resolved";
  affectedServiceIds: string[];
  startedAt: string;
  updates: PublicIncidentUpdate[];
}

export interface StatusSnapshot {
  generatedAt: string;
  services: ServiceStatusItem[];
  incidents: PublicIncident[];
}

export interface DashboardFilters {
  region: string;
  query: string;
  showResolved: boolean;
}
