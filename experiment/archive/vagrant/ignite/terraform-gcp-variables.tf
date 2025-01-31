# How to define variables in terraform:
# https://www.terraform.io/docs/configuration/variables.html

# Name of the project, replace "XX" for your
# respective group ID
variable "GCP_PROJECT_ID" {
    default = "esle-4"
}

#VM machine type
variable "GCP_MACHINE_TYPE" {
    default = "n1-standard-1"
}

# Region
variable "GCP_ZONE" {
    default = "europe-west3-c"
}

# Minimum required
variable "DISK_SIZE" {
    default = "15"
}

# nr of VMs
variable "COUNT" {
    default = "1"
}