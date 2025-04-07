const WebSocket = require('ws');
const readline = require('readline');
const SockJS = require('sockjs-client');
const Stomp = require('stompjs');

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

let stompClient = null;
let currentUser = null;
let currentScript = null;

function connect(userId, scriptId) {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('Connected to WebSocket server');
        currentUser = userId;
        currentScript = scriptId;
        
        // Subscribe to private messages for this script
        const destination = `/user/queue/script.${scriptId}.messages`;
        stompClient.subscribe(destination, function(message) {
            const messageData = JSON.parse(message.body);
            console.log('\nReceived message:');
            console.log(`From: ${messageData.senderName}`);
            console.log(`Content: ${messageData.content}`);
            console.log(`Time: ${new Date(messageData.timestamp).toLocaleString()}`);
            console.log('-------------------');
        });
    }, function(error) {
        console.error('Connection error:', error);
        setTimeout(() => connect(userId, scriptId), 5000); // Retry connection after 5s
    });
}

function disconnect() {
    if (stompClient) {
        stompClient.disconnect();
        console.log('Disconnected from WebSocket server');
    }
}

function sendMessage(content) {
    if (!stompClient || !currentUser || !currentScript) {
        console.error('Not properly connected');
        return;
    }

    const message = {
        content: content,
        scriptId: currentScript,
        senderId: currentUser,
        senderName: "Test User",
        timestamp: new Date().toISOString()
    };

    stompClient.send("/app/message.send", {}, JSON.stringify(message));
}

function readMessage() {
    rl.question('Enter message (or "quit" to exit): ', (input) => {
        if (input.toLowerCase() === 'quit') {
            disconnect();
            rl.close();
            return;
        }

        sendMessage(input);
        readMessage();
    });
}

connect(1, 1);
readMessage();