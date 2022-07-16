#!/bin/bash

set -e

active_dir=$(basename `pwd`)
target_dir="load_testing"

if [ ${active_dir} != ${target_dir} ] ; then
    echo "Should be run from ${target_dir} directory"
    exit 1
fi

locust_options="-f /mnt/load_testing/user_traffic.py --headless --users 10 --spawn-rate 1 -H http://localhost:8080 --run-time 30s"

docker run --rm --network=host -v `pwd`/.:/mnt/load_testing locustio/locust ${locust_options} 
