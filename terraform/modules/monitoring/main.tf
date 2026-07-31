terraform {
  required_providers {
    helm = { source = "hashicorp/helm", version = "~> 2.0" }
  }
}

resource "helm_release" "kube_prometheus_stack" {
  name             = "kube-prometheus-stack"
  repository       = "https://prometheus-community.github.io/helm-charts"
  chart            = "kube-prometheus-stack"
  version          = "57.0.0"
  namespace        = "monitoring"
  create_namespace = true
  timeout          = 600

  values = [
    templatefile("${path.module}/values/prometheus-stack.yaml", {
      grafana_admin_password = var.grafana_admin_password
    })
  ]
}

resource "helm_release" "loki" {
  name       = "loki"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "loki-stack"
  version    = "2.10.2"
  namespace  = "monitoring"

  depends_on = [helm_release.kube_prometheus_stack]

  set {
    name  = "promtail.enabled"
    value = "true"
  }
  set {
    name  = "grafana.enabled"
    value = "false"
  }
}

resource "helm_release" "tempo" {
  name       = "tempo"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "tempo"
  version    = "1.7.1"
  namespace  = "monitoring"

  depends_on = [helm_release.kube_prometheus_stack]
}
