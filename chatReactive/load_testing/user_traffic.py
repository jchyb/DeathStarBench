"""
Use the following command to run:

To run with GUI:
```
locust -f user_traffic.py
```
To run from the command line:
```
locust --headless --users 10 --spawn-rate 1 -H http://localhost:8080 --run-time 5m
```

"""

from locust import HttpUser, between, task
import random

class User(HttpUser):
    wait_time = between(5, 15)

    def on_start(self):
        self.user_id = random.randrange(1, 1000)

        username = f"user{self.user_id}"

        self.client.put(f"/set_username?user_id={self.user_id}&username={username}")

    @task
    def set_room(self):
        room_id = random.randrange(1, 100)

        self.client.put(f"/set_room?user_id={self.user_id}&room_id={room_id}")

    @task(3)
    def send_message(self):
        message = "example"

        self.client.put(f"/send_message?user_id={self.user_id}&message={message}")

    @task(3)
    def get_messages(self):
        self.client.get(f"/get_messages?user_id={self.user_id}")
