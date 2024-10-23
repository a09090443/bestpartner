package tw.zipe.bastpartner.resource

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

/**
 * @author Gary
 * @created 2024/10/8
 */
@Path("/view")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ViewResource {

    @GET
    @Path("/chat")
    @Produces(MediaType.TEXT_HTML)
    fun getChatPage(): String {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>LLM Chat</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                        }
                        #chat-container {
                            width: 50%;
                            margin: 0 auto;
                            border: 1px solid #ccc;
                            padding: 20px;
                            margin-top: 50px;
                            background-color: #f9f9f9;
                        }
                        #messages {
                            height: 300px;
                            border: 1px solid #ccc;
                            overflow-y: auto;
                            margin-bottom: 20px;
                            padding: 10px;
                            background-color: #fff;
                        }
                        #input-container {
                            display: flex;
                        }
                        #input {
                            flex: 1;
                            padding: 10px;
                            border: 1px solid #ccc;
                        }
                        #send {
                            padding: 10px 20px;
                            border: 1px solid #ccc;
                            background-color: #007bff;
                            color: white;
                            cursor: pointer;
                        }
                        .message-line {
                            white-space: pre-wrap;
                            display: block; /* Ensures it starts on a new line */
                        }
                    </style>
                </head>
                <body>
                    <div id="chat-container">
                        <h2>LLM Chat</h2>
                        <div id="messages"></div>
                        <div id="input-container">
                            <input type="text" id="input" placeholder="Type a message..." />
                            <button id="send">Send</button>
                        </div>
                    </div>
                
                    <script>
                        const input = document.getElementById('input');
                        const sendButton = document.getElementById('send');
                        const messages = document.getElementById('messages');
                
                        async function sendMessage(message) {
                            // Create a new line for this response
                            const messageLine = document.createElement('span');
                            messageLine.classList.add('message-line');
                            messages.appendChild(messageLine);
                
                            const response = await fetch('/llm/chatStreaming', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify({ message })
                            });
                
                            const reader = response.body.getReader();
                            const decoder = new TextDecoder('utf-8');
                
                            while (true) {
                                const { value, done } = await reader.read();
                                if (done) {
                                    break;
                                }
                
                                const chunk = decoder.decode(value, { stream: true });
                
                                // Process the chunk and remove "data:" prefix
                                const cleanChunk = chunk.replace(/data:\s*/g, '').trim();
                                displayMessage(cleanChunk, messageLine);
                            }
                        }
                
                        function displayMessage(chunk, messageLine) {
                            // Append cleaned chunk to the same line
                            messageLine.textContent += chunk;
                            messages.scrollTop = messages.scrollHeight;
                        }
                
                        sendButton.addEventListener('click', function() {
                            const message = input.value;
                            if (message) {
                                sendMessage(message);
                                input.value = '';
                            }
                        });
                    </script>
                </body>
                </html>
        """.trimIndent()
    }
}
