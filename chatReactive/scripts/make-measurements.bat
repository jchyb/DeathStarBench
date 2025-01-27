@REM Builds and measures performance of services

mkdir out
call ./scripts/build-all.bat
call docker-compose up -d
timeout 10
call locust -f load_testing/user_traffic.py --headless --users 20 --spawn-rate 5 -H http://localhost:8080 --run-time 2m
call curl -X GET "localhost:16686/api/traces?service=GatewayService" > out/reactive
call docker-compose down
call ./scripts/build-all-nr.bat
call docker-compose -f docker-compose-nonreactive.yml up -d
timeout 10
call locust -f load_testing/user_traffic.py --headless --users 20 --spawn-rate 5 -H http://localhost:8080 --run-time 2m
call curl -X GET "localhost:16686/api/traces?service=GatewayService" > out/nonreactive
call docker-compose -f docker-compose-nonreactive.yml down
call python analysis/script.py out/reactive out/nonreactive