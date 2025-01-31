###########  ignite nodes   #############
# This method creates as many identical instances as the "count" index value
resource "google_compute_instance" "web" {
    count = var.COUNT
    name = "web${count.index+1}"
    machine_type = var.GCP_MACHINE_TYPE
    zone = var.GCP_ZONE

    boot_disk {
        initialize_params {
          # image list can be found at:
          # https://cloud.google.com/compute/docs/images
          image = "ubuntu-2004-focal-v20221015"
        }
    }

    network_interface {
      network = "default"
      access_config {
      }
    }

    metadata = {
      ssh-keys = "ubuntu:${file("/home/vagrant/.ssh/id_rsa.pub")}"
    }
  tags = ["web"]
}