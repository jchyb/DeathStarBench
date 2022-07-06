#!/bin/bash

set -e

./sbt gateway/docker:publishLocal
./sbt messageregistry/docker:publishLocal
./sbt messageroom/docker:publishLocal
./sbt userservice/docker:publishLocal
#docker-compose up
