@REM build all containers and run load tests
call ./scripts/build-all
call docker-compose up -d
call locust -f load_testing/user_traffic.py --headless --users 10 --spawn-rate 1 -H http://localhost:8080 --run-time 2m