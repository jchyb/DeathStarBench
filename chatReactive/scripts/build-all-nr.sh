#!/bin/bash

set -e

./sbt "gatewayNR/docker:publishLocal;messageroomNR/docker:publishLocal;userserviceNR/docker:publishLocal;messageregistryNR/docker:publishLocal"
