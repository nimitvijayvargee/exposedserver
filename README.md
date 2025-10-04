# exposedserver
Minecraft plugin that exposes important information to a websocket, which you can access from anywhere with just the IP address, port and API Key.

# Instructions
1. Install fabric server from https://fabricmc.net/use/server/ (1.21.1, newest loader)
2. Download the mod from GitHub releases
3. Download fabric API from https://modrinth.com/mod/fabric-api
4. Run the server jar by running java -jar and your JVM flags.
5. Accept the Mojang EULA by opening eula.txt and settint false to true.
6. Put the mod and the Fabric API into the mods folder. 
7. Make a `exposedserver.json` in the config folder. Inside this file, paste the following content. Restart the server to take effect.
```js
{
  "apiKey" : "changeme",
  "port" : 6767
}
```
Use these to access the Websocket from the client.


# Client access
Visit the client site at https://exposedserver.nimitapps.com/. Add your server IP and exposedserver port (8000 by default). Use the API Key configured in your config, or `changeme` if a config file is not present. Do not use the same port as your main server. Both ports must have port forwarding enabled. Press connect, and wait for the websocket to attach. Once done, your stats should update periodically.
