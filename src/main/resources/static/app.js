const messagesEl = document.getElementById("messages");
const chatForm = document.getElementById("chatForm");
const messageInput = document.getElementById("messageInput");
const sendBtn = document.getElementById("sendBtn");
const statusPill = document.getElementById("statusPill");
const sessionIdInput = document.getElementById("sessionId");
const topKInput = document.getElementById("topK");
const topKValue = document.getElementById("topKValue");
const newSessionBtn = document.getElementById("newSessionBtn");

const STORAGE_KEY = "coredeux-rag-session-id";

function createSessionId() {
	return (crypto.randomUUID ? crypto.randomUUID() : `session-${Date.now()}`).replace(/-/g, "").slice(0, 16);
}

function setStatus(text) {
	statusPill.textContent = text;
}

function scrollToBottom() {
	messagesEl.scrollTop = messagesEl.scrollHeight;
}

function addMessage(role, text, label) {
	const message = document.createElement("article");
	message.className = `message ${role}`;

	const meta = document.createElement("div");
	meta.className = "message-meta";
	meta.textContent = label;

	const content = document.createElement("div");
	content.className = "message-content";
	content.textContent = text;

	message.append(meta, content);
	messagesEl.appendChild(message);
	scrollToBottom();
	return message;
}

function setMessageText(messageElement, text) {
	const content = messageElement.querySelector(".message-content");
	if (content) {
		content.textContent = text;
		scrollToBottom();
	}
}

function setSessionId(value) {
	sessionIdInput.value = value;
	localStorage.setItem(STORAGE_KEY, value);
}

function updateTopKLabel() {
	topKValue.textContent = topKInput.value;
}

function autoResize() {
	messageInput.style.height = "auto";
	messageInput.style.height = `${Math.min(messageInput.scrollHeight, 180)}px`;
}

messageInput.addEventListener("input", autoResize);
topKInput.addEventListener("input", updateTopKLabel);

newSessionBtn.addEventListener("click", () => {
	setSessionId(createSessionId());
	setStatus("New session");
	messageInput.focus();
});

chatForm.addEventListener("submit", async (event) => {
	event.preventDefault();

	const message = messageInput.value.trim();
	if (!message) {
		return;
	}

	if (!sessionIdInput.value.trim()) {
		setSessionId(createSessionId());
	}

	addMessage("user", message, "You");
	messageInput.value = "";
	autoResize();
	sendBtn.disabled = true;
	setStatus("Streaming...");

	const assistantMessage = addMessage("assistant", "", "RAG");

	try {
		const sessionId = sessionIdInput.value.trim();
		const topK = topKInput.value;
		const response = await fetch(`/rag/chat/stream?sessionId=${encodeURIComponent(sessionId)}&topK=${encodeURIComponent(topK)}`, {
			method: "POST",
			headers: {
				"Content-Type": "text/plain; charset=utf-8"
			},
			body: message
		});

		if (!response.ok) {
			throw new Error(`Request failed with status ${response.status}`);
		}

		let streamed = "";
		if (!response.body) {
			streamed = await response.text();
			setMessageText(assistantMessage, streamed);
		} else {
			const reader = response.body.getReader();
			const decoder = new TextDecoder();

			while (true) {
				const { done, value } = await reader.read();
				if (done) {
					break;
				}

				streamed += decoder.decode(value, { stream: true });
				setMessageText(assistantMessage, streamed);
			}

			streamed += decoder.decode();
			setMessageText(assistantMessage, streamed);
		}

		setStatus("Ready");
	} catch (error) {
		setMessageText(assistantMessage, `Request failed: ${error.message}`);
		setStatus("Error");
	} finally {
		sendBtn.disabled = false;
		messageInput.focus();
	}
});

const savedSessionId = localStorage.getItem(STORAGE_KEY);
setSessionId(savedSessionId || createSessionId());
topKInput.value = "4";
updateTopKLabel();
addMessage("assistant", "Ask a question about the indexed docs. I’ll retrieve context from pgvector and answer from that.", "Assistant");
