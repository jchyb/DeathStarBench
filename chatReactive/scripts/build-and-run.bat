@REM build all containers and run load tests

call ./scripts/build-all
call docker-compose up -d
timeout 10
call locust -f load_testing/user_traffic.py --headless --users 20 --spawn-rate 5 -H http://localhost:8080 --run-time 2m