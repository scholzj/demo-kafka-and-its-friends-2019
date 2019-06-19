#!/usr/bin/env bash

CONNECT_BRIDGE="$(kubectl get routes my-bridge -o jsonpath='{.status.ingress[0].host}')"

echo ""
echo "Creating the consumer-group"
echo ""

http ${CONNECT_BRIDGE}/consumers/my-bridge-group Content-Type:application/vnd.kafka.v2+json < 08-create-consumer.json

echo ""
echo "Subscribing to topics"
echo ""

http ${CONNECT_BRIDGE}/consumers/my-bridge-group/instances/my-client/subscription Content-Type:application/vnd.kafka.v2+json < 08-subscription.json

echo ""
echo "We should now be subscribed to the topic"
echo "Getting the messages"
echo ""

while http ${CONNECT_BRIDGE}/consumers/my-bridge-group/instances/my-client/records Accept:application/vnd.kafka.json.v2+json
do
  sleep 5
done
