var ws;

function startChat() {
	document.getElementById('messageInput').disabled = false;
	document.getElementById('sendButton').disabled = false;

	ws = new WebSocket('ws://localhost:8080/Bank_Application/chat');

	ws.onopen = function(event) {
		console.log('Chat started with employee.');
		ws.send('User requesting chat');
		appendMessage("Waiting for support..")
	};

	ws.onmessage = function(event) {
		console.log("Message received from server: " + event.data);

		appendMessage(event.data);
		if (event.data == 'Employee disconnected. Please start a new chat!') {
			document.getElementById('sendButton').disabled = true;
			document.getElementById('messageInput').disabled = true
		}
	};

}

const appendMessage = message => {

	var messageDiv = document.createElement("div");
	messageDiv.classList.add('message');
	messageDiv.style.display = "flex";
	messageDiv.style.alignItems = "flex-start";
	messageDiv.style.marginBottom = "15px";

	var messageBubble = document.createElement("div");
	messageBubble.classList.add('message-bubble');
	messageBubble.style.backgroundColor = "#2980b9";
	messageBubble.style.color = "white";
	messageBubble.style.padding = "8px 15px";
	messageBubble.style.borderRadius = "20px 20px 0 20px";
	messageBubble.style.maxWidth = "75%";
	messageBubble.style.marginLeft = "10px";
	messageBubble.textContent = message;
	messageDiv.appendChild(messageBubble);

	document.getElementById('chatMessages').appendChild(messageDiv);
}

function sendMessage() {
	var message = document.getElementById('messageInput').value;
	if (message.trim() !== "" && ws.readyState === WebSocket.OPEN) {
		ws.send(message);

		var messageDiv = document.createElement("div");
		messageDiv.classList.add('message');
		messageDiv.style.display = "flex";
		messageDiv.style.justifyContent = "flex-end";
		messageDiv.style.marginBottom = "15px";

		var messageBubble = document.createElement("div");
		messageBubble.classList.add('message-bubble');
		messageBubble.style.backgroundColor = "#f1f1f1";
		messageBubble.style.color = "#333";
		messageBubble.style.padding = "8px 15px";
		messageBubble.style.borderRadius = "20px 20px 20px 0";
		messageBubble.style.maxWidth = "75%";
		messageBubble.style.marginRight = "10px";
		messageBubble.textContent = message;
		messageDiv.appendChild(messageBubble);

		document.getElementById('chatMessages').appendChild(messageDiv);
		document.getElementById('messageInput').value = "";
	} else {
		console.log("WebSocket not open or message is empty.");
	}
}

function startEmployeeChat() {
	ws = new WebSocket('ws://localhost:8080/Bank_Application/chat');

	ws.onopen = function() {
		console.log('Employee connected.');
		ws.send('employee');
	};

	document.getElementById('startChatButton').disabled = true;

	ws.onmessage = function(event) {
		var data = event.data;
		console.log(data);

		if (data == "User disconnected.") {
			appendMessage(data);
			closeChat();
			toggleModal('employeeChatBox');
		} else if (data.startsWith("New user assigned: ")) {
			const userIdDiv = document.getElementById('chatUserName');
			let userIdString = "User ID: " + extractId(data);
			console.log(userIdString);
			userIdDiv.innerHTML = userIdString;
		} else appendMessage(data);
	};
}

function extractId(input) {
	const match = input.match(/New user assigned: (\S+)/);
	return match ? match[1] : null;
}
function extractMessage(input) {
	const match = input.match(/User \S+: (.+)/);
	return match ? match[1] : null;
}


function sendEmployeeMessage() {
	var message = document.getElementById('employeeMessageInput').value;
	console.log(message);
	if (message.trim() !== "") {
		ws.send(message);

		var messageDiv = document.createElement("div");
		messageDiv.classList.add('message');
		messageDiv.style.display = "flex";
		messageDiv.style.justifyContent = "flex-end";
		messageDiv.style.marginBottom = "15px";

		var messageBubble = document.createElement("div");
		messageBubble.classList.add('message-bubble');
		messageBubble.style.backgroundColor = "#f1f1f1";
		messageBubble.style.color = "#333";
		messageBubble.style.padding = "8px 15px";
		messageBubble.style.borderRadius = "20px 20px 20px 0";
		messageBubble.style.maxWidth = "75%";
		messageBubble.style.marginRight = "10px";
		messageBubble.textContent = message;
		messageDiv.appendChild(messageBubble);

		document.getElementById('chatMessages').appendChild(messageDiv);
		document.getElementById('employeeMessageInput').value = "";
	}
}

function userClose() {
	toggleModal('chatBoxModel');
	document.getElementById('sendButton').disabled = true;
	document.getElementById('messageInput').disabled = true;

	if (ws && ws.readyState === WebSocket.OPEN) {
		ws.close();
	}

	ws.onclose = function(event) {
		console.log('Chat connection closed');
	};
}


function closeChat() {
	document.getElementById('chatMessages').innerHTML = "";
	document.getElementById('chatUserName').textContent = "Connecting available user..";
	toggleModal('employeeChatBox');
	console.log('entered close method');

	if (ws && ws.readyState === WebSocket.OPEN) {
		ws.close();
	}

	ws.onclose = function(event) {
		console.log('Chat connection closed');
	};


	document.getElementById('startChatButton').disabled = false;

}

