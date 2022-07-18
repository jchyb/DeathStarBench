#!/bin/bash

set -e

active_dir=$(basename `pwd`)
target_dir="chatReactive"

if [ ${active_dir} != ${target_dir} ] ; then
    echo "Should be run from ${target_dir} directory"
    exit 1
fi

locust_options="-f /mnt/load_testing/user_traffic.py --headless --users 20 --spawn-rate 5 -H http://localhost:8080 --run-time 2m"

docker run --rm --network=host -v `pwd`/load_testing/.:/mnt/load_testing locustio/locust ${locust_options} 
