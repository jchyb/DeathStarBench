#!/bin/bash

set -e

./sbt "gateway/docker:publishLocal;userservice/docker:publishLocal;messageroom/docker:publishLocal;messageregistry/docker:publishLocal"
