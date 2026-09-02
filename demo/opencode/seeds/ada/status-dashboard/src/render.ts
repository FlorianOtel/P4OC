import { buildView, relativeTime } from "./status-model";
import type { DashboardFilters, PublicIncident, ServiceStatusItem, StatusSnapshot } from "./types";

export function escapeHtml(value: string): string {
  return value.replace(/[&<>'"]/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "'": "&#39;",
    '"': "&quot;"
  })[character] ?? character);
}

function renderService(service: ServiceStatusItem): string {
  return `<article class="service-card">
    <div>
      <h3>${escapeHtml(service.name)}</h3>
      <p>${escapeHtml(service.description)}</p>
      <span class="region">${escapeHtml(service.region)}</span>
    </div>
    <div class="service-metrics">
      <span class="status status--${service.status}">${escapeHtml(service.status)}</span>
      <span class="uptime" title="30-day uptime">${service.uptime30d.toFixed(2)}%</span>
    </div>
  </article>`;
}

function renderIncident(incident: PublicIncident, snapshot: StatusSnapshot, now: Date): string {
  const affectedNames = incident.affectedServiceIds.map((id) =>
    snapshot.services.find((service) => service.id === id)?.name ?? id
  );
  const updates = incident.updates.map((update) => `<li>
    <time datetime="${escapeHtml(update.publishedAt)}">${relativeTime(update.publishedAt, now)}</time>
    <p>${escapeHtml(update.message)}</p>
  </li>`).join("");
  return `<article class="incident incident--${incident.severity}">
    <header>
      <span class="eyebrow">${escapeHtml(incident.state)} · ${escapeHtml(incident.severity)}</span>
      <h3>${escapeHtml(incident.title)}</h3>
      <p>Affecting ${affectedNames.map(escapeHtml).join(", ")}</p>
    </header>
    <ol class="timeline">${updates}</ol>
  </article>`;
}

export function renderDashboard(snapshot: StatusSnapshot, filters: DashboardFilters, now = new Date()): string {
  const view = buildView(snapshot, filters);
  const regionOptions = ["all", ...view.regions].map((region) => {
    const label = region === "all" ? "All regions" : region;
    const selected = filters.region === region ? " selected" : "";
    return `<option value="${escapeHtml(region)}"${selected}>${escapeHtml(label)}</option>`;
  }).join("");
  const serviceCards = view.services.length
    ? view.services.map(renderService).join("")
    : `<p class="empty">No services match these filters.</p>`;
  const incidentCards = view.incidents.length
    ? view.incidents.map((incident) => renderIncident(incident, snapshot, now)).join("")
    : `<p class="empty">No active incidents.</p>`;

  return `<header class="masthead">
    <a class="brand" href="/">Northstar <span>Status</span></a>
    <p>Last refreshed <time datetime="${escapeHtml(snapshot.generatedAt)}">${relativeTime(snapshot.generatedAt, now)}</time></p>
  </header>
  <main>
    <section class="hero hero--${view.overall}" aria-labelledby="system-heading">
      <span class="pulse" aria-hidden="true"></span>
      <div><p class="eyebrow">Current system status</p><h1 id="system-heading">${escapeHtml(view.overall)}</h1></div>
    </section>
    <section aria-labelledby="services-heading">
      <div class="section-heading"><div><p class="eyebrow">Infrastructure</p><h2 id="services-heading">Services</h2></div>
        <div class="filters">
          <label>Search <input id="service-query" type="search" value="${escapeHtml(filters.query)}" placeholder="Name, region…"></label>
          <label>Region <select id="region-filter">${regionOptions}</select></label>
        </div>
      </div>
      <div class="service-grid">${serviceCards}</div>
    </section>
    <section aria-labelledby="incidents-heading">
      <div class="section-heading"><div><p class="eyebrow">Event history</p><h2 id="incidents-heading">Incidents</h2></div>
        <label class="resolved-toggle"><input id="show-resolved" type="checkbox"${filters.showResolved ? " checked" : ""}> Show resolved</label>
      </div>
      <div class="incident-list">${incidentCards}</div>
    </section>
  </main>
  <footer><p>Northstar Operations · Updates are refreshed every minute.</p></footer>
  <div class="sr-only" aria-live="polite" id="refresh-status"></div>`;
}
