<!-- AUTO-SYNCED from agents KB: concepts/MONITORING.md @ 8beba02.
     Do NOT edit here — edit the source in ~/projects/agents and re-run scripts/sync-conventions.sh. -->

# Monitoring & Observability

**SigNoz is the standard observability platform.** It replaces the old
Grafana + Prometheus + Loki stack. New projects instrument with OpenTelemetry and get
their **dashboards and alerts built in SigNoz** — not Grafana.

UI: `https://signoz.jannekeipert.de`. Runs in the `observability` namespace
(see [CLUSTER.md](CLUSTER.md)). Deployed via ArgoCD/Helm from
`cluster-deployment/infrastructure/signoz.yaml` (+ `signoz-k8s-infra.yaml`).

## What SigNoz gives you (one place for all three signals)

- **Traces** — distributed tracing across services (OTLP).
- **Metrics** — app + host/node + Kubernetes metrics, stored in ClickHouse.
- **Logs** — pod logs and k8s events, shipped by the `k8s-infra` DaemonSet OTel
  agents. This is the promtail→Loki replacement.

Backing components: ClickHouse (telemetry store, `local-path` PVC, 50 Gi) + Zookeeper
+ a central `signoz-otel-collector` + the UI. Cluster-wide logs/host metrics come from
the `k8s-infra` OTel agent DaemonSet, so **you get pod logs and node metrics for free**
— no per-app work needed for those.

## How to send telemetry (the collector endpoints)

Everything goes to the central collector in-cluster:

| Signal | Endpoint | Notes |
|--------|----------|-------|
| OTLP gRPC | `signoz-otel-collector.observability.svc.cluster.local:4317` | |
| OTLP HTTP | `http://signoz-otel-collector.observability.svc.cluster.local:4318` | full URL w/ scheme + port required |
| Prometheus remote-write | `:9091` on the collector | bridge only, see below |

## Instrumenting a new project

**DO:**
- **Emit OTLP.** For Spring Boot (Java 21), the simplest path is the OpenTelemetry
  Java agent (auto-instrumentation) with:
  `OTEL_EXPORTER_OTLP_ENDPOINT=http://signoz-otel-collector.observability.svc.cluster.local:4317`,
  `OTEL_SERVICE_NAME=<project>`, `OTEL_RESOURCE_ATTRIBUTES=deployment.environment=<prod|staging>`.
  Alternatively use the Micrometer OTLP registry for metrics.
- Set a clear, stable **`service.name`** per deployable (this is the primary group-by
  in SigNoz) and a `deployment.environment` resource attribute so prod/staging split
  cleanly (e.g. `cosy-prod` vs `cosy-staging`).
- **Build the project's dashboard(s) in SigNoz** and define alerts there. Keep the
  dashboard JSON in the project's `*-deployment` repo (or a `monitoring/` dir) so it's
  version-controlled, not just clicked together in the UI.
- Rely on the `k8s-infra` agents for logs + host/pod metrics; only add app-level
  instrumentation for spans and custom business metrics.

**DON'T:**
- Build new dashboards in Grafana or wire new apps into Prometheus scraping — that
  stack is being decommissioned.
- Hardcode the collector endpoint in code — inject it via env/ConfigMap so it can move.
- Use the bare `host:4317` form for the OTLP-HTTP exporter — it needs the full
  `http://…:4318` URL (scheme + HTTP port) or it errors "unsupported protocol scheme".

## Alerting (SigNoz-native since 2026-07-12)

The uptime/TLS alerts (EndpointDown, EndpointSlow, UptimeMonitoringDown,
UptimeSSLCertExpiring{Soon,Critical}) are **SigNoz alert rules**, not Prometheus
rules. The legacy `uptime-alerts` PrometheusRule was removed 2026-07-17; the
in-cluster Alertmanager only has a null receiver and sends nothing.

- **Delivery chain:** SigNoz rule → notification channel `n8n-webhook` → n8n
  workflow "SigNoz Alert Workflow" → the mail-service API
  (`mail-service.jannekeipert.de/api/v1/send`) → email to both recipients.
  (The old Grafana-format "Cluster Alert Workflow" is deactivated in n8n.)
- **Source of truth for rule definitions:** SigNoz's own DB. A version-controlled
  JSON export lives in `cluster-deployment/infrastructure/signoz-alerts/` — after
  editing a rule (UI or `PUT /api/v1/rules/<ruleId>`), re-export it there.
- **Data source:** the rules query blackbox `probe_*` metrics that the legacy
  Prometheus scrapes and remote-writes into SigNoz — blackbox-exporter,
  the Probe CRs (`uptime-probes.yaml`), and the remoteWrite block must survive
  until probing moves to an OTel-native path.

## Migration status (transitional — don't build new on the old stack)

SigNoz runs **in parallel** with the legacy stack during migration (the node has
headroom). Phasing, per the deployment repo:

1. **Phase 1** — SigNoz core (ClickHouse + collector + UI). `prune` is deliberately
   OFF on the core app until validated end-to-end.
2. **Phase 2** — `k8s-infra` DaemonSet agents for logs, k8s events, host metrics.
   Validate the SigNoz Logs tab reaches parity **before** decommissioning Loki.
3. **Phase 3** — kube-prometheus-stack Prometheus `remote_write`s into the SigNoz
   collector (`:9091`), so existing series — notably the blackbox `probe_*` uptime
   metrics — land in ClickHouse. Isolated `metrics/prometheus` pipeline so it can't
   disturb the OTLP pipelines.

The legacy `kube-prometheus-stack` (Prometheus/Grafana/Alertmanager in `default` /
`monitoring`), `loki`, `blackbox-exporter`, and `pushgateway` still exist and will be
torn down once SigNoz reaches parity. Spring backends still expose
`/actuator/prometheus` (Micrometer) — fine to keep, but the destination of record is
SigNoz. Treat Grafana as read-only legacy.
