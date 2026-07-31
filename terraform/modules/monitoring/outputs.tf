output "grafana_namespace" {
  value = "monitoring"
}

output "prometheus_service" {
  description = "Prometheus service endpoint within the cluster"
  value       = "http://kube-prometheus-stack-prometheus.monitoring.svc.cluster.local:9090"
}
