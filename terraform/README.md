# Infrastructure

Terraform config to run `url-shortener` on AWS: ECS Fargate for the app, RDS Postgres for storage, ECR for the image. Uses the account's default VPC — no custom networking, because a single-service portfolio deployment doesn't need its own VPC topology.

## What this provisions

- **ECR repository** — where the CI pipeline pushes images (see `.github/workflows/ci.yml`)
- **ECS Fargate cluster + service** — runs one task, pulls from ECR
- **RDS Postgres** (`db.t3.micro`, free-tier eligible) — in a security group that only accepts traffic from the ECS service's security group
- **IAM execution role** — scoped to pulling images and reading the DB password from SSM Parameter Store, nothing broader
- **CloudWatch log group** — container stdout/stderr, 7-day retention

## Running it

```bash
cd terraform
terraform init

export TF_VAR_db_password="<a real secret, not this string>"
terraform plan -var="container_image=ghcr.io/fikratdev/url-shortener:latest"
terraform apply -var="container_image=ghcr.io/fikratdev/url-shortener:latest"
```

`db_password` is deliberately never written to a `.tfvars` file that could get committed — it's read from the `TF_VAR_db_password` environment variable instead.

## What's missing on purpose

- No Application Load Balancer — the task gets a public IP directly via Fargate. Fine for a demo, not fine for anything that needs zero-downtime deploys or HTTPS termination.
- No Redis/ElastiCache in this config — the app falls back to direct DB reads without it. Adding `elasticache.tf` would mirror the RDS module almost exactly.
- Default VPC instead of a dedicated one with private subnets — the honest tradeoff of "this is a portfolio deployment," not a production posture.
- No remote state backend (S3 + DynamoDB lock table) — state is local. Would be the first thing to fix before a second person touches this.

## Stack

Terraform · AWS (ECS Fargate, RDS, ECR, IAM, SSM, CloudWatch)
