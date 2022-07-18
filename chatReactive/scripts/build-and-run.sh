#!/bin/bash

set -e

./scripts/build-all
docker-compose up -d
sleep 10
locust -f load_testing/user_traffic.py --headless --users 20 --spawn-rate 5 -H http://localhost:8080 --run-time 2m