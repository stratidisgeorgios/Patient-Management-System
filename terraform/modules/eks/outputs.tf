output "cluster_name" {
  description = "EKS cluster name"
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "API server endpoint — used to configure kubectl and the kubernetes provider"
  value       = module.eks.cluster_endpoint
}

output "cluster_certificate_authority_data" {
  description = "Base64-encoded CA certificate — used by kubectl to verify the API server"
  value       = module.eks.cluster_certificate_authority_data
}

output "oidc_provider_arn" {
  description = "ARN of the OIDC provider — required when creating IRSA roles for pods"
  value       = module.eks.oidc_provider_arn
}

output "oidc_provider_url" {
  description = "Issuer URL of the OIDC provider (without https://)"
  value       = module.eks.cluster_oidc_issuer_url
}

output "node_security_group_id" {
  description = "Security group attached to every worker node — add ingress rules to RDS/MSK/OpenSearch SGs to allow pod access"
  value       = module.eks.node_security_group_id
}
