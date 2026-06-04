terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-southeast-2"
}

# ------------------ Security group ---------------------------

resource "aws_security_group" "Server" {
  name        = "terraform-game-server"
  description = "Allow SSH from anywhere"

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 8000
    to_port     = 8000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Grafana"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }


  ingress {
    description = "Prometheus"
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

}

# ------------------ variables ---------------------------

variable "key_pair_name" {
  description = "Name of an existing EC2 key pair for SSH access"
  type        = string
}

variable "dockerhub_username" {
  description = "Docker Hub username (used in UserData to pull images)"
  type        = string
}

# ------------------ Instance OS ---------------------------

data "aws_ssm_parameter" "ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# ------------------ EC2 instance ---------------------------

resource "aws_instance" "tutorial" {
  ami                    = data.aws_ssm_parameter.ami.value
  instance_type          = "t3.micro"
  key_name               = var.key_pair_name
  vpc_security_group_ids = [aws_security_group.Server.id]

  user_data = <<-EOF
    #!/bin/bash
    yum update -y
    dnf install -y docker
    systemctl start docker
    systemctl enable docker
    usermod -aG docker ec2-user
     docker run -d --name app -p 80:8000 ${var.dockerhub_username}/game-server:latest
  EOF

  tags = {
    Name = "Week8-Terraform-Tutorial"
  }
}

resource "aws_eip" "app" {
  instance = aws_instance.tutorial.id
  domain   = "vpc"

  tags = {
    Name = "Week8-CICD-EIP"
  }
}

# ------------------ Outputs ---------------------------

output "instance_public_ip" {
  description = "Public IP address of the instance"
  value       = aws_instance.tutorial.public_ip
}

output "elastic_ip" {
  description = "Elastic IP address (stable — does not change)"
  value       = aws_eip.app.public_ip
}

output "instance_id" {
  description = "Instance ID"
  value       = aws_instance.tutorial.id
}