# Faults Generator Service

This Cloud Run service is designed to reproduce networking faults and test connectivity behaviors across different runtime environments (Java, Python, Node.js, Go, and Linux/cURL).

It allows you to trigger HTTP requests from the container to arbitrary destinations to observe how different language stacks handle networking errors (e.g., routing failures, timeouts, connection refusals, resets).

## Prerequisites

1.  **Artifact Registry**: Create a docker repository in Artifact Registry

    ```bash
    gcloud artifacts repositories create faults-repo \
        --repository-format=docker \
        --location=REGION \
        --description="Faults Generator Repository"
    ```

## Building with Cloud Build

Use the provided `cloudbuild.yaml` to build the container image. You must explicitly provide substitution variables for the repository details.

```bash
# Set your variables
export PROJECT_ID=$(gcloud config get-value project)
export REGION="us-central1"
export REPO_NAME="faults-repo"
export IMAGE_NAME="faults"
export FAULTS_VERSION="1.0.0"

# Submit the build from the parent directory of faults (the root directory of this repository)
gcloud builds submit . \
  --config faults/cloudbuild.yaml \
  --substitutions=_REPO_LOCATION=${REGION},_REPO_NAME=${REPO_NAME},_IMAGE_NAME=${IMAGE_NAME},_FAULTS_VERSION=${FAULTS_VERSION}
```

## Deploying to Cloud Run

Deploy the built image to Cloud Run. You can configure it with specific VPC settings to test different networking scenarios.

```bash
gcloud run deploy faults \
  --image ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}/${IMAGE_NAME}:v${FAULTS_VERSION} \
  --region ${REGION} \
  --allow-unauthenticated
```

### Networking Options

Use these Cloud Run deployment flags to configure the network environment for your tests:

* VPC Connector: `--vpc-connector my-connector --vpc-egress all-traffic`
* Direct VPC Egress: `--network my-network --subnet my-dual-stack-subnet --vpc-egress all-traffic`


## Testing Endpoints

The service exposes a REST API to trigger outbound HTTP requests.

Base URL: `https://YOUR_SERVICE_URL/faults`

### Generic HTTP Request Faults

Trigger an HTTP request to a destination URL.

POST `/http`

```bash
curl -v -X POST "${YOUR_SERVICE_URL}/faults/http" \
  -H "Content-Type: application/json" \
  -d "{\"url\": \"http://www.google.com\"}"
```

Runs the request across all supported runtimes sequentially (Linux, Java, Python, Node.js, Go).

POST `/http/{runtime}`

Runs the request using a specific runtime.

Allowed Runtimes: `java`, `linux`, `python`, `nodejs`, `golang`.

```bash
curl -v -X POST "${YOUR_SERVICE_URL}/faults/http/java" \
  -H "Content-Type: application/json" \
  -d "{\"url\": \"http://www.google.com\"}"
```

### Google APIs Faults

Trigger requests specifically designed to test Google API client behaviors or authenticated calls.

POST `/googleapis/http/{runtime}`

```bash
curl -v -X POST "${SERVICE_URL}/faults/googleapis/http/python" \
  -H "Content-Type: application/json" \
  -d "{\"url\": \"https://storage.googleapis.com/my-bucket/file.txt\"}"
```


