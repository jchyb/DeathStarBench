@REM Builds all nonreactive services
call ./sbt gatewayNR/docker:publishLocal;messageroomNR/docker:publishLocal;userserviceNR/docker:publishLocal;messageregistryNR/docker:publishLocal
