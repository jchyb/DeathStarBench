#!/bin/bash

set -e

./scripts/build-all
docker-compose up -d
sleep 10
./load_testing/test.sh