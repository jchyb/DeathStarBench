### How to Run

To build a docker container for a SERVICE service, run:
```
.\cd chatReactive
.\sbt SERVICE/docker:publishLocal 
```
(Example: .\sbt gateway/docker:publishLocal)

To run the cluster, run `docker-compose up` after building all of the necessary containers.
You can also run all above steps with generating load with `.\scripts\build-and-run`

