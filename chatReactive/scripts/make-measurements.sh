#!/bin/bash

set -e

mkdir out
./scripts/build-all.bat
docker-compose up -d
sleep 10
locust -f load_testing/user_traffic.py --headless --users 20 --spawn-rate 5 -H http://localhost:8080 --run-time 2m
curl -X GET "localhost:16686/api/traces?service=GatewayService" > out/reactive
docker-compose down
./scripts/build-all-nr.bat
docker-compose -f docker-compose-nonreactive.yml up -d
sleep 10
locust -f load_testing/user_traffic.py --headless --users 20 --spawn-rate 5 -H http://localhost:8080 --run-time 2m
curl -X GET "localhost:16686/api/traces?service=GatewayService" > out/nonreactive
docker-compose -f docker-compose-nonreactive.yml down
python3 analysis/script.py out/reactive out/nonreactive