[Reactive Web App Code Copied from this article](https://developer.okta.com/blog/2018/09/25/spring-webflux-websockets-react)

[Web Socket Code Copied from this github repo](https://github.com/marios-code-path/spring-web-sockets)

[Knative Workshop](https://docs.google.com/document/d/1QKjyWAJxZahQFUc8FkM_0gVtgDRUDgJq7zcJLiFjjjw/edit#)export WS_SERVER=http://localhost:8080/ws/feed

[Kafka Code Adopted from](https://github.com/reactor/reactor-kafka)

Run Locally:
```
export WS_SERVER=ws://stackoverflow-to-ws.default.35.224.5.101.nip.io/questions
export BOOTSTRAP_SERVER=localhost:9092
export KAFKA_TOPIC=stackoverflow-questions
export STORE_KAFKA=true
```

Run in Knative:
```
export BOOTSTRAP_SERVER=my-cluster-kafka-bootstrap:9092
```


./mvnw spring-boot:run

kubectl apply -f knative/ws-server.yaml

kubectl label ksvc websocket-server \
  serving.knative.dev/visibility=cluster-local

kubectl get ksvc websocket-server

export HOST_NAME=$(kubectl get route websocket-server \
  -o 'jsonpath={.status.url}' | cut -c8-)

Lab 7 - Custom Domain


export WS_SERVER=http://websocket-server.default.35.224.148.61.nip.io/ws/feed


kubectl logs webscocket-client-59vhl-deployment-658cc6fdf5-skv45 -c user-container

kubectl get revision
kubectl get ksvc
kubectl get routes

kubectl get pods -n knative-serving
kubectl describe ksvc/websocket-server


kubectl -n knative-serving --as=system:serviceaccount:knative-serving:controller auth can-i get configmaps