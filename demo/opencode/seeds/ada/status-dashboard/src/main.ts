import "./styles.css";
import { renderDashboard } from "./render";
import { loadStatusSnapshot } from "./status-client";
import type { DashboardFilters, StatusSnapshot } from "./types";

const root = document.querySelector<HTMLDivElement>("#app");
if (!root) throw new Error("Missing #app mount point");
const mount = root;

const filters: DashboardFilters = { region: "all", query: "", showResolved: false };
let snapshot: StatusSnapshot | undefined;

function render(): void {
  if (!snapshot) return;
  const activeElement = document.activeElement;
  const restoreSearch = activeElement instanceof HTMLInputElement && activeElement.id === "service-query";
  const selectionStart = restoreSearch ? activeElement.selectionStart : null;
  mount.innerHTML = renderDashboard(snapshot, filters);
  if (restoreSearch) {
    const search = mount.querySelector<HTMLInputElement>("#service-query");
    search?.focus();
    if (selectionStart !== null) search?.setSelectionRange(selectionStart, selectionStart);
  }
}

async function refresh(): Promise<void> {
  try {
    snapshot = await loadStatusSnapshot(import.meta.env.VITE_STATUS_API_URL);
    render();
  } catch (error) {
    console.error("Could not refresh status", error);
    const announcement = mount.querySelector<HTMLElement>("#refresh-status");
    if (announcement) announcement.textContent = "Status refresh failed. Showing the last available update.";
  }
}

mount.innerHTML = `<main class="loading"><p>Loading Northstar status…</p></main>`;
mount.addEventListener("input", (event) => {
  const target = event.target;
  if (target instanceof HTMLInputElement && target.id === "service-query") {
    filters.query = target.value;
    render();
  }
});
mount.addEventListener("change", (event) => {
  const target = event.target;
  if (target instanceof HTMLSelectElement && target.id === "region-filter") filters.region = target.value;
  if (target instanceof HTMLInputElement && target.id === "show-resolved") filters.showResolved = target.checked;
  render();
});

void refresh();
window.setInterval(() => void refresh(), 60_000);
