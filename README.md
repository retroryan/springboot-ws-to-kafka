./mvnw spring-boot:run

kubectl apply -f knative/ws-server.yaml

kubectl label ksvc websocket-server \
  serving.knative.dev/visibility=cluster-local

kubectl get ksvc websocket-server

export HOST_NAME=$(kubectl get route websocket-server \
  -o 'jsonpath={.status.url}' | cut -c8-)

kubectl port-forward websocket-server 8080