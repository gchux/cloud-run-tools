# Echo Server

This service accepts HTTP/1.1 requests and responds with a JSON object including information about the request:

- protocol
- method
- URL
- query params
- headers
- body
- Cloud Run details:
  - project ID
  - project number
  - regions
  - service
  - revision
  - instance ID
