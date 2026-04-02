package com.restaurantcomm.messaging

import com.restaurantcomm.data.model.Message
import com.restaurantcomm.discovery.DiscoveredDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL

class LocalNetworkTransport(
    private val onMessageReceived: suspend (Message) -> Unit
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    fun start(port: Int) {
        if (serverSocket != null) return
        val socket = ServerSocket(port)
        serverSocket = socket

        scope.launch {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                scope.launch { handleConnection(client) }
            }
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    fun send(peer: DiscoveredDevice, message: Message): Boolean {
        val host = peer.host ?: return false
        val endpoint = URL("http://$host:${peer.port}/message")
        return runCatching {
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 2_000
                readTimeout = 2_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            connection.outputStream.use { stream ->
                BufferedWriter(OutputStreamWriter(stream)).use { writer ->
                    writer.write(message.toJsonString())
                    writer.flush()
                }
            }

            val code = connection.responseCode
            connection.disconnect()
            code in 200..299
        }.getOrDefault(false)
    }

    private suspend fun handleConnection(socket: Socket) {
        socket.use { client ->
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream()))

            val requestLine = reader.readLine() ?: return
            if (!requestLine.startsWith("POST /message")) {
                writeResponse(writer, 404, "Not Found")
                return
            }

            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: return
                if (line.isBlank()) break
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(':').trim().toIntOrNull() ?: 0
                }
            }

            if (contentLength <= 0) {
                writeResponse(writer, 400, "Bad Request")
                return
            }

            val payload = CharArray(contentLength)
            var totalRead = 0
            while (totalRead < contentLength) {
                val read = reader.read(payload, totalRead, contentLength - totalRead)
                if (read == -1) break
                totalRead += read
            }

            val body = String(payload, 0, totalRead)
            val message = runCatching { Message.fromJsonString(body) }.getOrNull()
            if (message == null) {
                writeResponse(writer, 400, "Bad Request")
                return
            }

            onMessageReceived(message)
            writeResponse(writer, 200, "OK")
        }
    }

    private fun writeResponse(writer: BufferedWriter, statusCode: Int, statusMessage: String) {
        val body = if (statusCode == 200) "OK" else statusMessage
        writer.write("HTTP/1.1 $statusCode $statusMessage\r\n")
        writer.write("Content-Type: text/plain\r\n")
        writer.write("Content-Length: ${body.toByteArray().size}\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.write(body)
        writer.flush()
    }
}
