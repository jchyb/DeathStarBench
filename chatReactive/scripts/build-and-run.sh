#!/bin/bash

set -e

./scripts/build-all
docker-compose up -d
locust -f load_testing/user_traffic.py --headless --users 10 --spawn-rate 1 -H http://localhost:8080 --run-time 2m