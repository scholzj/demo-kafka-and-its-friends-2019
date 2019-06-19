#!/usr/bin/env bash

CONNECT_HOST="$(kubectl get routes my-connect-cluster -o jsonpath='{.status.ingress[0].host}')"

http http://${CONNECT_HOST}/connectors < 06-debezium-connector.json

echo ""
echo ""
echo "Check connector status at http://${CONNECT_HOST}/connectors/debezium-connector/status"