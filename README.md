# MetricPosterPlugin

you can install this plugin on these servers:
- main server
- queue server (with queueServer config turned on)

config.yml
```yaml
queueServer: false # turn on this only if the plugin is in queue server, don't use this if you have 2+ queue servers.
authorization: # set same value as AUTHORIZATION in https://github.com/acrylic-style/status.2b2t.jp/blob/master/.env.example
endPoint: # example: https://status.2b2t.jp/api/data.json
```

if you have `AUTHORIZATION=abcdef` defined in .env from acrylic-style/status.2b2t.jp, and the end point is `https://status.example.com/api/data.json`, the config.yml will look like this:
```yaml
authorization: abcdef
endPoint: "https://status.example.com/api/data.json"
```

the end point must return valid json and should be accessible from User-Agent `Java/your_jvm_version`
