#!/usr/bin/env bash

CONNECT_HOST="$(kubectl get routes my-connect-cluster -o jsonpath='{.status.ingress[0].host}')"

curl -X POST \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://${CONNECT_HOST}/connectors -d @06-debezium-connector.json

echo ""
echo ""
echo "Check connector status at http://${CONNECT_HOST}/connectors/debezium-connector/status"