# Hello World Example

Start server.

```bash
clj -M:start
```

Hit routes.

```bash
curl localhost:3000

curl localhost:3000/json

# Echo json
curl -d '{"foo": "bar"}' -X POST localhost:3000/echo

# Echo bad json
curl -d '{"foo":' -X POST localhost:3000/echo
```