terraform {
  // aws 라이브러리 불러옴
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

# AWS 설정 시작
provider "aws" {
  region = var.region
}
# AWS 설정 끝

# --- 1. 네트워크 (VPC, Subnet, IGW, Route) ---
resource "aws_vpc" "vpc-team1" {
  cidr_block = "10.0.0.0/16"

  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.prefix}-vpc" // ${var.prefix}-vpc
    Team = var.team_tag_value
  }
}

// AZ-a (Public Subnet for EC2)
resource "aws_subnet" "subnet-team1-a" {
  vpc_id                  = aws_vpc.vpc-team1.id
  cidr_block              = "10.0.0.0/24"
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-a" // ${var.prefix}-subnet-a
    Team = var.team_tag_value
  }
}

// AZ-b (Public Subnet - 예비용 또는 확장 대비)
resource "aws_subnet" "subnet-team1-b" {
  vpc_id                  = aws_vpc.vpc-team1.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.region}b"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-b" // ${var.prefix}-subnet-b
    Team = var.team_tag_value
  }
}

resource "aws_internet_gateway" "igw-team1" {
  vpc_id = aws_vpc.vpc-team1.id

  tags = {
    Name = "${var.prefix}-igw" // ${var.prefix}-igw
    Team = var.team_tag_value
  }
}

resource "aws_route_table" "rt-team1" {
  vpc_id = aws_vpc.vpc-team1.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw-team1.id
  }

  tags = {
    Name = "${var.prefix}-rt" // ${var.prefix}-rt
    Team = var.team_tag_value
  }
}

resource "aws_route_table_association" "assoc-a" {
  subnet_id      = aws_subnet.subnet-team1-a.id
  route_table_id = aws_route_table.rt-team1.id
}

resource "aws_route_table_association" "assoc-b" {
  subnet_id      = aws_subnet.subnet-team1-b.id
  route_table_id = aws_route_table.rt-team1.id
}

resource "aws_security_group" "sg-team1" {
  name = "${var.prefix}-sg" // ${var.prefix}-sg

  // 인바운드: 모든 IP (0.0.0.0/0)에서 모든 포트 허용 (보안 강화를 위해 최소 포트만 허용하도록 변경 권장)
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "all"
    cidr_blocks = ["0.0.0.0/0"]
  }

  // 아웃바운드: 모두 허용
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "all"
    cidr_blocks = ["0.0.0.0/0"]
  }

  vpc_id = aws_vpc.vpc-team1.id

  tags = {
    Name = "${var.prefix}-sg" // ${var.prefix}-sg
    Team = var.team_tag_value
  }
}

# --- 2. IAM (EC2 Role for SSM & S3) ---
resource "aws_iam_role" "ec2-role-team1" {
  name = "${var.prefix}-ec2-role" // ${var.prefix}-ec2-role

  // EC2 서비스가 이 역할을 가정할 수 있도록 설정
  assume_role_policy = <<EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "",
        "Action": "sts:AssumeRole",
        "Principal": {
            "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow"
      }
    ]
  }
  EOF

  tags = {
    Name = "${var.prefix}-ec2-role"
    Team = var.team_tag_value
  }
}

// EC2 역할에 AmazonEC2RoleforSSM 정책을 부착 (SSM 통신을 위해 필수)
resource "aws_iam_role_policy_attachment" "ec2-ssm" {
  role       = aws_iam_role.ec2-role-team1.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}

// S3 접근 권한 (필요에 따라 최소 권한으로 변경 권장)
resource "aws_iam_role_policy_attachment" "s3-full-access" {
  role       = aws_iam_role.ec2-role-team1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

// IAM 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "instance-profile-team1" {
  name = "${var.prefix}-instance-profile" // ${var.prefix}-instance-profile
  role = aws_iam_role.ec2-role-team1.name

  tags = {
    Name = "${var.prefix}-instance-profile"
    Team = var.team_tag_value
  }
}

# --- 3. EC2 인스턴스 (Blue/Green 배포 대상) ---
locals {
  ec2_user_data_base = <<-END_OF_FILE
#!/bin/bash
# 가상 메모리 4GB 설정
dd if=/dev/zero of=/swapfile bs=128M count=32
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'

# 타임존 설정
timedatectl set-timezone Asia/Seoul

# 환경변수 세팅(/etc/environment) - 통일된 이름 사용
echo "PASSWORD_1=${var.password_1}" >> /etc/environment
echo "APP_1_DOMAIN=${var.app_1_domain}" >> /etc/environment
echo "APP_1_DB_NAME=${var.app_1_db_name}" >> /etc/environment
echo "GITHUB_ACCESS_TOKEN_1_OWNER=${var.github_access_token_1_owner}" >> /etc/environment
echo "GITHUB_ACCESS_TOKEN_1=${var.github_access_token_1}" >> /etc/environment
echo "NPM_ADMIN_EMAIL=${var.admin_email}" >> /etc/environment
echo "NPM_ADMIN_PASSWORD=${var.admin_pwd}" >> /etc/environment
source /etc/environment

# 도커 설치 및 실행/활성화
yum install docker -y
systemctl enable docker
systemctl start docker

# Docker Compose 설치
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# 도커 네트워크 생성
docker network create common

# .env 파일 생성
cat <<EOT > /home/ec2-user/.env
${var.app_env}
EOT

# prometheus.yml 파일 생성
cat <<EOT > /home/ec2-user/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: "prometheus"
    static_configs:
      - targets: ["prometheus:9090"]

  - job_name: "spring-actuator"
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
    file_sd_configs:
      - files:
          - '/etc/prometheus/targets.json'
        refresh_interval: 30s
  - job_name: "redis"
    static_configs:
      - targets: ["redis_exporter:9121"]
EOT

# targets.json 파일 생성
cat <<EOT > /home/ec2-user/targets.json
[]
EOT

# nginx proxy manager 설치
docker run -d \
  --name npm \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -p 443:443 \
  -p 81:81 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/npm_1/volumes/data:/data \
  -v /dockerProjects/npm_1/volumes/etc/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest

# redis 설치 (redis_1 컨테이너 이름 유지)
docker run -d \
  --name=redis \
  --restart unless-stopped \
  --network common \
  -p 6379:6379 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/redis_1/volumes/data:/data \
  redis --requirepass ${var.password_1}

# MariaDB 설치
docker run -d \
  --name mariadb \
  --restart unless-stopped \
  -v /dockerProjects/mariadb_1/volumes/var/lib/mysql:/var/lib/mysql \
  -v /dockerProjects/mariadb_1/volumes/etc/mysql/conf.d:/etc/mysql/conf.d \
  --network common \
  -p 3306:3306 \
  -e MARIADB_ROOT_PASSWORD=${var.password_1} \
  -e MARIADB_DATABASE=${var.app_1_db_name} \
  -e TZ=Asia/Seoul \
  mariadb:11.7 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci \
  --default-time-zone=+09:00

# MariaDB 컨테이너가 준비될 때까지 대기
echo "MariaDB가 기동될 때까지 대기 중..."
until docker exec mariadb mariadb -uroot -p${var.password_1} -e "SELECT 1" &> /dev/null; do
  echo "MariaDB가 아직 준비되지 않음. 5초 후 재시도..."
  sleep 5
done
echo "MariaDB가 준비됨."

# prometheus 데이터 디렉토리 생성 및 권한 설정
mkdir -p /home/ec2-user/prometheus-data
chown -R 1000:1000 /home/ec2-user/prometheus-data
chown 1000:1000 /home/ec2-user/targets.json

# Prometheus 설치
docker run -d \
  --name prometheus \
  --restart unless-stopped \
  --network common \
  --user 1000:1000 \
  -p 9090:9090 \
  -v /home/ec2-user/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v /home/ec2-user/targets.json:/etc/prometheus/targets.json \
  -v /home/ec2-user/prometheus-data:/prometheus \
  prom/prometheus:latest

# grafana 데이터 디렉토리 생성 및 권한 설정
mkdir -p /home/ec2-user/grafana-data
chown -R 472:472 /home/ec2-user/grafana-data

# grafana 설치
docker run -d \
  --name grafana \
  --restart unless-stopped \
  --network common \
  --user 472:472 \
  -p 3000:3000 \
  -v /home/ec2-user/grafana-data:/var/lib/grafana \
  grafana/grafana:latest

# GitHub Container Registry 로그인
echo "${var.github_access_token_1}" |
docker login ghcr.io -u ${var.github_access_token_1_owner} --password-stdin

# 애플리케이션 디렉토리 생성
mkdir -p /home/ec2-user/app
cd /home/ec2-user/app

# 로그 디렉토리 생성 및 권한 설정
mkdir -p /dockerProjects/${var.prefix}-app-001/logs
mkdir -p /dockerProjects/${var.prefix}-app-002/logs
chmod -R 777 /dockerProjects/${var.prefix}-app-001/logs
chmod -R 777 /dockerProjects/${var.prefix}-app-002/logs

# influxdb 설치
docker run -d \
  --name influxdb \
  --network common \
  -p 8086:8086 \
  -e INFLUXDB_HTTP_AUTH_ENABLED=false \
  -e INFLUXDB_DB=k6 \
  influxdb:1.8

# redis exporter 설치
docker run -d \
  --name redis_exporter \
  --network common \
  -p 9121:9121 \
  -e REDIS_PASSWORD=${var.password_1} \
  oliver006/redis_exporter \
  --redis.addr=redis://redis:6379

echo "${var.github_access_token_1}" | docker login ghcr.io -u ${var.github_access_token_1_owner} --password-stdin

END_OF_FILE
}

// 최신 Amazon Linux 2023 AMI 조회
data "aws_ami" "latest-amazon-linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
}

// EC2 인스턴스 생성
resource "aws_instance" "ec2-team1" {
  ami                         = data.aws_ami.latest-amazon-linux.id
  instance_type               = "t3.small"
  # key_name                    = var.key_name
  subnet_id                   = aws_subnet.subnet-team1-a.id
  vpc_security_group_ids      = [aws_security_group.sg-team1.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.instance-profile-team1.name

  tags = {
    Name = "${var.prefix}-backend" // ${var.prefix}-backend (배포 대상 EC2 태그)
    Team = var.team_tag_value
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 30
  }

  user_data = <<-EOF
${local.ec2_user_data_base}
EOF

  # CloudFront를 먼저 생성한 후 EC2 생성 (user_data에서 CloudFront 도메인 사용)
  depends_on = [aws_cloudfront_distribution.this]
}

# --- 4. S3 Bucket (image uploads) ---
resource "aws_s3_bucket" "app_bucket" {
  bucket = var.s3_bucket_name

  tags = {
    Name = "${var.prefix}-s3-bucket"
    Team = var.team_tag_value
  }
}

# ==================== Lambda 함수 (Node.js) ====================
resource "aws_lambda_function" "profile_image_resizer" {
  filename         = "lambda/profile_resizer.zip"
  function_name    = "${var.prefix}-profile-resizer"
  role             = aws_iam_role.lambda_profile_resizer.arn
  handler          = "index.handler"           # Node.js handler
  runtime          = "nodejs20.x"              # Node.js 20
  timeout          = 30
  memory_size      = 512
  source_code_hash = filebase64sha256("lambda/profile_resizer.zip")

  environment {
    variables = {
      BUCKET_NAME        = aws_s3_bucket.app_bucket.id
      SOURCE_PREFIX      = "members/profile/originals/"
      DESTINATION_PREFIX = "members/profile/resized/thumbnail/"
    }
  }

  tags = {
    Name = "${var.prefix}-profile-resizer"
    Team = var.team_tag_value
  }
}

# Lambda CloudWatch Logs
resource "aws_cloudwatch_log_group" "profile_resizer" {
  name              = "/aws/lambda/${var.prefix}-profile-resizer"
  retention_in_days = 7

  tags = {
    Name = "${var.prefix}-profile-resizer-logs"
    Team = var.team_tag_value
  }
}

# Lambda IAM Role
resource "aws_iam_role" "lambda_profile_resizer" {
  name = "${var.prefix}-lambda-profile-resizer-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })

  tags = {
    Name = "${var.prefix}-lambda-profile-resizer-role"
    Team = var.team_tag_value
  }
}

# Lambda 정책 - 특정 경로만 접근 (보안 강화)
resource "aws_iam_role_policy" "lambda_profile_resizer_policy" {
  name = "${var.prefix}-lambda-profile-resizer-policy"
  role = aws_iam_role.lambda_profile_resizer.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3ReadOriginals"
        Effect = "Allow"
        Action = [
          "s3:GetObject"
        ]
        # 특정 버킷의 originals 폴더만 읽기
        Resource = "${aws_s3_bucket.app_bucket.arn}/members/profile/originals/*"
      },
      {
        Sid    = "S3WriteThumbnails"
        Effect = "Allow"
        Action = [
          "s3:PutObject"
        ]
        # 특정 버킷의 resized 폴더만 쓰기
        Resource = "${aws_s3_bucket.app_bucket.arn}/members/profile/resized/*"
      },
      {
        Sid    = "CloudWatchLogs"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        # 자기 Lambda 로그만 접근
        Resource = "arn:aws:logs:${var.region}:*:log-group:/aws/lambda/${var.prefix}-profile-resizer*"
      }
    ]
  })
}

# ==================== 현재 계정 정보 ====================
data "aws_caller_identity" "current" {}

# ==================== S3 트리거 ====================
resource "aws_lambda_permission" "allow_s3_profile" {
  statement_id   = "AllowS3InvokeProfile"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.profile_image_resizer.function_name
  principal      = "s3.amazonaws.com"
  source_arn     = aws_s3_bucket.app_bucket.arn
  source_account = data.aws_caller_identity.current.account_id
}


# ==================== 게시글 Lambda ====================
resource "aws_lambda_function" "post_image_resizer" {
  filename         = "lambda/post_resizer.zip"
  function_name    = "${var.prefix}-post-resizer"
  role             = aws_iam_role.lambda_post_resizer.arn
  handler          = "post-image-resizer.handler"
  runtime          = "nodejs20.x"
  timeout          = 60
  memory_size      = 1024
  source_code_hash = filebase64sha256("lambda/post_resizer.zip")

  environment {
    variables = {
      BUCKET_NAME        = aws_s3_bucket.app_bucket.id
      SOURCE_PREFIX      = "posts/images/originals/"
      DESTINATION_PREFIX = "posts/images/resized/"
    }
  }

  tags = {
    Name = "${var.prefix}-post-resizer"
    Team = var.team_tag_value
  }
}

# Lambda CloudWatch Logs - 게시글
resource "aws_cloudwatch_log_group" "post_resizer" {
  name              = "/aws/lambda/${var.prefix}-post-resizer"
  retention_in_days = 7

  tags = {
    Name = "${var.prefix}-post-resizer-logs"
    Team = var.team_tag_value
  }
}

# Lambda IAM Role - 게시글
resource "aws_iam_role" "lambda_post_resizer" {
  name = "${var.prefix}-lambda-post-resizer-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })

  tags = {
    Name = "${var.prefix}-lambda-post-resizer-role"
    Team = var.team_tag_value
  }
}

# Lambda 정책 - 게시글용
resource "aws_iam_role_policy" "lambda_post_resizer_policy" {
  name = "${var.prefix}-lambda-post-resizer-policy"
  role = aws_iam_role.lambda_post_resizer.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3ReadOriginalsPost"
        Effect = "Allow"
        Action = [
          "s3:GetObject"
        ]
        Resource = "${aws_s3_bucket.app_bucket.arn}/posts/images/originals/*"
      },
      {
        Sid    = "S3WriteResizedPost"
        Effect = "Allow"
        Action = [
          "s3:PutObject"
        ]
        Resource = "${aws_s3_bucket.app_bucket.arn}/posts/images/resized/*"
      },
      {
        Sid    = "CloudWatchLogsPost"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:${var.region}:*:log-group:/aws/lambda/${var.prefix}-post-resizer*"
      }
    ]
  })
}

# S3 트리거 - 게시글
resource "aws_lambda_permission" "allow_s3_post" {
  statement_id   = "AllowS3InvokePost"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.post_image_resizer.function_name
  principal      = "s3.amazonaws.com"
  source_arn     = aws_s3_bucket.app_bucket.arn
  source_account = data.aws_caller_identity.current.account_id
}

# ==================== S3 Notification - 통합 ====================
resource "aws_s3_bucket_notification" "image_upload" {
  bucket = aws_s3_bucket.app_bucket.id

  # 프로필 이미지
  lambda_function {
    lambda_function_arn = aws_lambda_function.profile_image_resizer.arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "members/profile/originals/"
  }

  # 게시글 이미지
  lambda_function {
    lambda_function_arn = aws_lambda_function.post_image_resizer.arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "posts/images/originals/"
  }

  depends_on = [
    aws_lambda_permission.allow_s3_profile,
    aws_lambda_permission.allow_s3_post
  ]
}

# ==================== CloudFront ====================
resource "aws_cloudfront_origin_access_identity" "this" {
  comment = "OAI for ${var.prefix}"
}

resource "aws_cloudfront_distribution" "this" {
  enabled         = true
  is_ipv6_enabled = true
  comment         = "${var.prefix} CDN"
  price_class     = "PriceClass_200" # 북미, 유럽, 아시아

  origin {
    domain_name = aws_s3_bucket.app_bucket.bucket_regional_domain_name
    origin_id   = "S3-${aws_s3_bucket.app_bucket.id}"

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.this.cloudfront_access_identity_path
    }
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-${aws_s3_bucket.app_bucket.id}"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    min_ttl     = 0
    default_ttl = 86400    # 1일
    max_ttl     = 31536000 # 1년

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Name = "${var.prefix}-cloudfront"
    Team = var.team_tag_value
  }
}

# S3 버킷 정책 - CloudFront 접근 허용
resource "aws_s3_bucket_policy" "cloudfront_access" {
  bucket = aws_s3_bucket.app_bucket.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontOAI"
        Effect = "Allow"
        Principal = {
          AWS = aws_cloudfront_origin_access_identity.this.iam_arn
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.app_bucket.arn}/*"
      }
    ]
  })
}

# ==================== Outputs ====================
output "cloudfront_domain" {
  value       = aws_cloudfront_distribution.this.domain_name
  description = "CloudFront domain name"
}

output "s3_bucket_name" {
  value       = aws_s3_bucket.app_bucket.id
  description = "S3 bucket name"
}

output "profile_lambda_function_name" {
  value       = aws_lambda_function.profile_image_resizer.function_name
  description = "Profile Lambda function name"
}

output "post_lambda_function_name" {
  value       = aws_lambda_function.post_image_resizer.function_name
  description = "Post Lambda function name"
}

output "public_ip" {
  value       = aws_instance.ec2-team1.public_ip
  description = "EC2 Public IP"
}