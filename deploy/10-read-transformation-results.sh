#!/usr/bin/env bash

CONNECT_BRIDGE="$(kubectl get routes my-bridge -o jsonpath='{.status.ingress[0].host}')"

echo ""
echo "Creating the consumer-group"
echo ""

http ${CONNECT_BRIDGE}/consumers/my-bridge-group2 Content-Type:application/vnd.kafka.v2+json < 10-create-consumer.json

echo ""
echo "Subscribing to topics"
echo ""

http ${CONNECT_BRIDGE}/consumers/my-bridge-group2/instances/my-client2/subscription Content-Type:application/vnd.kafka.v2+json < 10-subscription.json

echo ""
echo "We should now be subscribed to the topic"
echo ""
echo "Getting the messages"
echo ""

while http ${CONNECT_BRIDGE}/consumers/my-bridge-group2/instances/my-client2/records Accept:application/vnd.kafka.json.v2+json
do
  sleep 5
done