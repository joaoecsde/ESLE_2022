provider "google" {
    # Create/Download your credentials from:
    # Google Console -> "APIs & services -> Credentials"
    # Choose create- > "service account key" -> compute engine service account -> JSON
    credentials = file("esle-4-de4bff470872.json")
    project = var.GCP_PROJECT_ID
    zone = var.GCP_ZONE
}
