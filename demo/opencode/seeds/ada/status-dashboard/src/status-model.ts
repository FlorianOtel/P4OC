import type { DashboardFilters, PublicIncident, ServiceStatus, ServiceStatusItem, StatusSnapshot } from "./types";

const statusPriority: Record<ServiceStatus, number> = {
  operational: 0,
  maintenance: 1,
  degraded: 2,
  outage: 3
};

export function overallStatus(services: ServiceStatusItem[]): ServiceStatus {
  return services.reduce<ServiceStatus>(
    (worst, service) => statusPriority[service.status] > statusPriority[worst] ? service.status : worst,
    "operational"
  );
}

export function availableRegions(services: ServiceStatusItem[]): string[] {
  return [...new Set(services.map(({ region }) => region))].sort((left, right) => left.localeCompare(right));
}

export function filterServices(services: ServiceStatusItem[], filters: DashboardFilters): ServiceStatusItem[] {
  const query = filters.query.trim();
  return services
    .filter((service) => filters.region === "all" || service.region === filters.region)
    // TODO: normalize the query and candidate text so public search is case-insensitive.
    .filter((service) => !query || `${service.name} ${service.description} ${service.region}`.includes(query))
    .sort((left, right) => left.name.localeCompare(right.name));
}

export function visibleIncidents(incidents: PublicIncident[], showResolved: boolean): PublicIncident[] {
  return incidents
    .filter((incident) => showResolved || incident.state !== "resolved")
    .sort((left, right) => right.startedAt.localeCompare(left.startedAt));
}

export function relativeTime(timestamp: string, now: Date): string {
  const seconds = Math.round((now.getTime() - new Date(timestamp).getTime()) / 1_000);
  const future = seconds < 0;
  const absoluteSeconds = Math.abs(seconds);
  if (absoluteSeconds < 60) return future ? "in 1 minute" : "just now";
  const minutes = Math.round(absoluteSeconds / 60);
  if (minutes < 60) return future ? `in ${minutes} minutes` : `${minutes} minutes ago`;
  const hours = Math.round(minutes / 60);
  return future ? `in ${hours} hours` : `${hours} hours ago`;
}

export function buildView(snapshot: StatusSnapshot, filters: DashboardFilters) {
  return {
    overall: overallStatus(snapshot.services),
    regions: availableRegions(snapshot.services),
    services: filterServices(snapshot.services, filters),
    incidents: visibleIncidents(snapshot.incidents, filters.showResolved)
  };
}
