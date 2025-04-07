/**
 * Script Review Chat - Client-side WebSocket handler
 */

class ScriptChat {
    constructor() {
        this.stompClient = null;
        this.token = null;
        this.scriptId = null;
        this.userInfo = null;
        this.connected = false;
        
        // DOM elements
        this.elements = {
            tokenInput: document.getElementById('token'),
            scriptIdInput: document.getElementById('scriptId'),
            connectButton: document.getElementById('connect'),
            disconnectButton: document.getElementById('disconnect'),
            messageInput: document.getElementById('messageInput'),
            sendButton: document.getElementById('sendMessage'),
            messagesContainer: document.getElementById('messages'),
            connectionStatus: document.getElementById('connectionStatus'),
            errorDisplay: document.getElementById('errorDisplay')
        };

        // Bind event handlers
        this.elements.connectButton.addEventListener('click', this.connect.bind(this));
        this.elements.disconnectButton.addEventListener('click', this.disconnect.bind(this));
        this.elements.sendButton.addEventListener('click', this.sendMessage.bind(this));
        this.elements.messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.sendMessage();
        });
    }

    /**
     * Connect to WebSocket server and subscribe to the script's messages
     */
    connect() {
        // Reset error display
        this.showError('');
        
        // Get connection details
        this.token = this.elements.tokenInput.value;
        this.scriptId = this.elements.scriptIdInput.value;

        if (!this.token) {
            this.showError('Please enter your authentication token');
            return;
        }

        if (!this.scriptId) {
            this.showError('Please enter a valid script ID');
            return;
        }

        // Set up SockJS and STOMP client
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        // Disable debug logging in production
        // this.stompClient.debug = null;

        // Ajouter le token JWT directement dans l'URL de connexion
        // Cette approche permet de s'assurer que le token est disponible dès le handshake
        const connectHeaders = {
            'Authorization': `Bearer ${this.token}`
        };

        this.updateStatus('Connecting...');

        // Connect to WebSocket server
        this.stompClient.connect(
            connectHeaders,
            this.onConnected.bind(this),
            this.onError.bind(this)
        );
    }

    /**
     * Callback when successfully connected to WebSocket server
     */
    onConnected(frame) {
        this.connected = true;
        this.updateStatus('Connected');
        
        // Extract user info from JWT token
        this.extractUserInfo();
        
        // Définir les headers pour l'authentification lors de la souscription
        const subscribeHeaders = {
            'Authorization': `Bearer ${this.token}`
        };
        
        // Subscribe to script messages topic
        this.stompClient.subscribe(
            `/topic/script.${this.scriptId}.messages`, 
            this.onMessageReceived.bind(this),
            subscribeHeaders
        );

        // Add system connection message
        this.addStatusMessage(`Connected to script #${this.scriptId} chat`);
        
        // Update UI state
        this.elements.connectButton.disabled = true;
        this.elements.disconnectButton.disabled = false;
        this.elements.messageInput.disabled = false;
        this.elements.sendButton.disabled = false;
    }

    /**
     * Parse JWT token to extract user info
     */
    extractUserInfo() {
        try {
            // Simple JWT parsing (for demo purposes)
            // In production, you might want to validate the token properly
            const payload = this.token.split('.')[1];
            const decodedPayload = atob(payload);
            this.userInfo = JSON.parse(decodedPayload);
        } catch (error) {
            console.error('Error extracting user info from token', error);
            this.userInfo = { sub: 'Unknown User' };
        }
    }

    /**
     * Handle WebSocket connection errors
     */
    onError(error) {
        this.connected = false;
        this.updateStatus('Disconnected');
        this.showError(`Connection error: ${error}`);
        
        // Reset UI state
        this.elements.connectButton.disabled = false;
        this.elements.disconnectButton.disabled = true;
        this.elements.messageInput.disabled = true;
        this.elements.sendButton.disabled = true;
    }

    /**
     * Disconnect from WebSocket server
     */
    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
            this.connected = false;
            this.updateStatus('Disconnected');
            this.addStatusMessage('Disconnected from chat');
            
            // Reset UI state
            this.elements.connectButton.disabled = false;
            this.elements.disconnectButton.disabled = true;
            this.elements.messageInput.disabled = true;
            this.elements.sendButton.disabled = true;
        }
    }

    /**
     * Send message to server
     */
    sendMessage() {
        if (!this.connected) {
            this.showError('Not connected to chat');
            return;
        }

        const content = this.elements.messageInput.value.trim();
        if (!content) return;

        const message = {
            content: content,
            timestamp: new Date().toISOString()
        };
        
        // Définir les headers pour l'authentification lors de l'envoi du message
        const sendHeaders = {
            'Authorization': `Bearer ${this.token}`
        };

        this.stompClient.send(
            `/app/script/${this.scriptId}/message.send`, 
            sendHeaders, 
            JSON.stringify(message)
        );

        this.elements.messageInput.value = '';
    }

    /**
     * Handle incoming message from server
     */
    onMessageReceived(payload) {
        const message = JSON.parse(payload.body);
        this.displayMessage(message);
    }

    /**
     * Display message in chat window
     */
    displayMessage(message) {
        const messageElement = document.createElement('div');
        messageElement.className = `message ${message.type}`;
        
        const formatter = new Intl.DateTimeFormat('default', {
            hour: '2-digit',
            minute: '2-digit'
        });
        const time = formatter.format(new Date(message.timestamp));
        
        messageElement.innerHTML = `
            <div class="message-header">
                <span>${message.senderName || 'Unknown'}</span>
            </div>
            <div class="message-content">${this.escapeHtml(message.content)}</div>
            <div class="message-timestamp">${time}</div>
        `;
        
        this.elements.messagesContainer.appendChild(messageElement);
        this.elements.messagesContainer.scrollTop = this.elements.messagesContainer.scrollHeight;
    }

    /**
     * Add status/system message to chat
     */
    addStatusMessage(text) {
        const statusElement = document.createElement('div');
        statusElement.className = 'status-message';
        statusElement.textContent = text;
        this.elements.messagesContainer.appendChild(statusElement);
        this.elements.messagesContainer.scrollTop = this.elements.messagesContainer.scrollHeight;
    }

    /**
     * Update connection status display
     */
    updateStatus(status) {
        this.elements.connectionStatus.textContent = status;
    }

    /**
     * Show error message
     */
    showError(message) {
        this.elements.errorDisplay.textContent = message;
        this.elements.errorDisplay.style.display = message ? 'block' : 'none';
    }

    /**
     * Escape HTML to prevent XSS
     */
    escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
}

// Initialize chat when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    const chat = new ScriptChat();
}); 