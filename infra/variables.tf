variable "region" {
  description = "region"
  default     = "ap-northeast-2"
}

variable "prefix" {
  description = "Prefix for all resources"
  default     = "team1" // 팀 이름으로 사용
}

variable "team_tag_value" { // 팀 태그 값 추가
  description = "Value for the 'Team' tag (e.g., devcos-team01)"
  default     = "devcos-team01"
}

variable "app_1_domain" {
  description = "app_1 domain"
  default     = "api.chwimeet.store"
}