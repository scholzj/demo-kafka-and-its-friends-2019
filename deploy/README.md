# Developing and running Kafka based applications on Kubernetes

This demo shows deployment of Strimzi 0.11.4 on OpenShift together with a simple demo application which demonstrates different common patterns used with Apache Kafka.

## Preparation

* Create new projects or namespace for the demo

```
kubectl create ns demo
```

```
oc new-project demo
```

* Install the Prometheus and Grafana

```
oc apply -f 00-prometheus-grafana
```

* Deploy Grafana Dashboards

## Run the demo

Deploy the applicationns