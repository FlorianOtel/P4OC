import type { Incident, IncidentFilters, IncidentState, IncidentSummary, Service } from "./types.js";

export type Clock = () => string;

export class IncidentStore {
  readonly #services: Service[];
  readonly #incidents: Incident[];
  readonly #clock: Clock;

  constructor(services: Service[], incidents: Incident[], clock: Clock = () => new Date().toISOString()) {
    this.#services = structuredClone(services);
    this.#incidents = structuredClone(incidents);
    this.#clock = clock;
  }

  listServices(): Array<Service & { openIncidentCount: number }> {
    return this.#services.map((service) => ({
      ...service,
      openIncidentCount: this.#incidents.filter(
        (incident) => incident.serviceId === service.id && incident.state !== "resolved"
      ).length
    }));
  }

  listIncidents(filters: IncidentFilters = {}): { items: IncidentSummary[]; total: number } {
    let matches = this.#incidents.filter((incident) =>
      (!filters.serviceId || incident.serviceId === filters.serviceId) &&
      (!filters.state || incident.state === filters.state)
    );
    matches = matches.sort((left, right) => right.startedAt.localeCompare(left.startedAt));
    if (filters.limit !== undefined) matches = matches.slice(0, filters.limit);

    return {
      items: matches.map(({ updates, ...incident }) => ({
        ...incident,
        latestMessage: updates.at(-1)?.message ?? "No updates yet.",
        updateCount: updates.length
      })),
      // TODO: this should describe all matching incidents, not just this page.
      total: matches.length
    };
  }

  getIncident(id: string): Incident | undefined {
    const incident = this.#incidents.find((candidate) => candidate.id === id);
    return incident ? structuredClone(incident) : undefined;
  }

  acknowledge(id: string): Incident | undefined {
    const incident = this.#incidents.find((candidate) => candidate.id === id);
    if (!incident) return undefined;
    // TODO: resolved incidents should reject this transition rather than gaining a new acknowledgement.
    incident.acknowledgedAt = this.#clock();
    return structuredClone(incident);
  }

  addUpdate(id: string, state: IncidentState, message: string): Incident | undefined {
    const incident = this.#incidents.find((candidate) => candidate.id === id);
    if (!incident) return undefined;
    const createdAt = this.#clock();
    incident.state = state;
    incident.updates.push({ state, message, createdAt });
    if (state === "resolved") incident.resolvedAt = createdAt;
    return structuredClone(incident);
  }
}
