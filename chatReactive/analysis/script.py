import json
import sys
import matplotlib.pyplot as plt

with open(sys.argv[1], "r") as f:
    data_reactive = f.read()

with open(sys.argv[2], "r") as f:
    data_non_reactive = f.read()

parsed_reactive = json.loads(data_reactive)
parsed_non_reactive = json.loads(data_non_reactive)


calls = {"GatewayService": 0, "MessageRoomService": 0, "MessageRegistryService": 0, "UserService": 0}
callsNR = calls.copy()

def parse_data(parsed):
    clientTime = {"GatewayService": 0, "MessageRoomService": 0, "MessageRegistryService": 0, "UserService": 0}
    serverTime = clientTime.copy()
    calls = clientTime.copy()

    for trace in parsed["data"]:
        process_map = {}
        for (p_name, value) in trace["processes"].items():
            process_map[p_name] = value["serviceName"]
        for span in reversed(trace["spans"]):
            service = process_map[span["processID"]]
            duration = span["duration"]
            
            is_client = False
            for tag in span["tags"]:
                if tag["key"] == "span.kind":
                    is_client = (tag["value"] == "client")
                    break
            
            start_time = span["startTime"]
            end_time = start_time + duration
            if not is_client:
                calls[service] += 1
                serverTime[service] += duration
            else:
                clientTime[service] += duration
    return (clientTime, serverTime, calls)

(client_time_reactive, server_time_reactive, calls_reactive) = parse_data(parsed_reactive)
(client_time_non_reactive, server_time_non_reactive, calls_non_reactive) = parse_data(parsed_non_reactive)

def make_plot(title, filename, time_reactive, time_non_reactive):
    x = [2,4,6,8]
    width = 1

    fig, ax = plt.subplots()
    rects1 = ax.bar(list(map(lambda a: a - width/2, x)), time_reactive.values(), width, label='Reactive')
    rects2 = ax.bar(list(map(lambda a: a + width/2, x)), time_non_reactive.values(), width, label='Non Reactive')

    ax.set_ylabel('Sum of time spent in calls in a microservice. [ms]')
    ax.set_title(title)
    ax.set_xticks(x, list(time_reactive.keys()))
    ax.legend()
    ax.bar_label(rects1, padding=3)
    ax.bar_label(rects2, padding=3)
    fig.set_size_inches(12, 6)
    plt.savefig(filename)

make_plot('Comparison - time spent on processing server requests.', "out/server.png", server_time_reactive, server_time_non_reactive)
make_plot('Comparison - time spent waiting by clients.', "out/client.png", client_time_reactive, client_time_non_reactive)

x = [2,4,6,8]
width = 1

fig, ax = plt.subplots()
rects1 = ax.bar(list(map(lambda a: a - width/2, x)), calls_reactive.values(), width, label='Reactive')
rects2 = ax.bar(list(map(lambda a: a + width/2, x)), calls_non_reactive.values(), width, label='Non Reactive')

ax.set_ylabel('Amount of Http calls')
ax.set_title('Comparison - amount of Http calls recorded to a microservice')
ax.set_xticks(x, list(calls_reactive.keys()))
ax.legend()
ax.bar_label(rects1, padding=3)
ax.bar_label(rects2, padding=3)
fig.set_size_inches(12, 6)
plt.savefig("out/calls.png")

print("Generated out/server.png, out/client.png, and out/calls.png")