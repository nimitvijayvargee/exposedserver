let ws;

function connect() {
    const port = document.getElementById('port').value || 6767;
    const host = document.getElementById('host').value || '127.0.0.1';
    const apiKey = document.getElementById('apiKey').value;
    if (!apiKey) return alert('API Key required');

    ws = new WebSocket(`ws://${host}:${port}`);
    ws.onopen = () => {
        document.getElementById('status').innerText = 'Connected';
        ws.send(JSON.stringify({ apiKey }));
    };

    ws.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            console.log(data)
            if (data.type === "status") {
                document.getElementById(`motd`).innerText = data.motd || '[MOTD not found]';
                document.getElementById(`playercount`).innerText = data.playerCount + '/' + (data.maxPlayerCount || '??');
                document.getElementById(`mspt`).innerText = (data.mspt || '??') + ' ms';
                document.getElementById(`entitycount`).innerText = data.entityCount || '??';
                document.getElementById(`chunks`).innerText = data.chunks || '??';

                let ram = data.ram;
                let maxRam = data.maxRam;
                document.getElementById(`rammb`).innerText = (ram ? Math.round(ram / 1024) : '??') + 'MB/' + (maxRam ? Math.round(maxRam / 1024) : '??') + 'MB';
                document.getElementById(`ramgb`).innerText = (ram ? (ram / 1048576).toFixed(2) : '??') + 'GB/' + (maxRam ? (maxRam / 1048576).toFixed(2) : '??') + 'GB';

                let playerlist = data.players || [];
                let playerListContainer = document.getElementById('playerlistcontainer');
                console.log(playerlist);
                if (playerlist.length === 0) {
                    playerListContainer.innerHTML = `
                        <span class="label">Players online:</span> <br />
                        <span class="infovalue" id="playerlistvalue">[Player list not found]</span>
                    `;
                } else {
                    playerListContainer.innerHTML = `
                        <span class="label">Players online:</span> <br />
                    `;
                    for (const key in playerlist) {
                        const value = playerlist[key];
                        let playerElement = document.createElement('span');
                        playerElement.className = 'playerlistvalue';
                        playerElement.innerText = value;
                        playerListContainer.appendChild(playerElement);
                        playerListContainer.appendChild(document.createElement('br'));
                    }
                }
            }

        } catch (e) {
            console.error(e);
        }
    };


    ws.onerror = (err) => {
        document.getElementById('status').innerText = 'WebSocket error';
        console.error(err);
    };
}

function sendCommand() {
    const cmd = document.querySelectorAll('#dashboard input')[0].value;
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify({ apiKey: document.getElementById('apiKey').value, command: cmd }));
}

function sendMessage() {
    const msg = document.querySelectorAll('#dashboard input')[1].value;
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify({ apiKey: document.getElementById('apiKey').value, command: `say ${msg}` }));
}
