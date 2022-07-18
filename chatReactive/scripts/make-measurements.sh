#!/bin/bash

set -e

mkdir -p out
./scripts/build-all.sh
docker-compose up -d
sleep 10
./load_testing/test.sh
curl -X GET "localhost:16686/api/traces?service=GatewayService" > out/reactive
docker-compose down
./scripts/build-all-nr.sh
docker-compose -f docker-compose-nonreactive.yml up -d
sleep 10
./load_testing/test.sh
curl -X GET "localhost:16686/api/traces?service=GatewayService" > out/nonreactive
docker-compose -f docker-compose-nonreactive.yml down
python3 analysis/script.py out/reactive out/nonreactive