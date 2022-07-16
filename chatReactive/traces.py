import requests
import json

url = 'http://localhost:16686/api/traces'

services = [
'GatewayService',
#'MessageRegistryService',
#'MessageRoomService',
#'UserService',
]


for service in services:
    response = requests.get(url, params={'service': service})
    data = response.json()

    if data:
        print(json.dumps(data, indent=2), end='\n\n\n')
