# exposedserver
Minecraft plugin that exposes important information to a websocket, which you can access from anywhere with just the IP address, port and API Key.

# Instructions
Add the release jar to your mods folder in Fabric 1.21.1. Do this at your own risk, as it may lead to another open port on your PC/Network.
The mod is not compatible with any other dashboards, and may violate terms of service with server hosting services, as it binds to two ports rather than one.
Use at your own risk on any server hosting service, or use their dashboard instead.
When selecting a port, do not use the same port as the minecraft server, since both use their own implementation of sockets.

Fabric API is a dependency, and is not optional.
# Using the Config
By default, the websocket binds to port 6767. The default API Key for accessing the socket is `changeme`. You can change these by creating an `exposedserver.json` file in your config folder. Inside this file, paste the following content:
```js
{
  "apiKey" : "changeme",
  "port" : 6767
}
```
Use these to access the Websocket from the client.


# Client access
Visit the client site at https://exposedserver.nimitapps.com/. Add your server IP and exposedserver port (8000 by default). Use the API Key configured in your config, or `changeme` if a config file is not present. Do not use the same port as your main server. Both ports must have port forwarding enabled. Press connect, and wait for the websocket to attach. Once done, your stats should update periodically.
