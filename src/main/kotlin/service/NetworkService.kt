package service

import com.beust.klaxon.Klaxon
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import extension.wireMessagingStreams
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import model.Message
import model.State
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread


class NetworkService {

  private val LOG = Logger.getInstance(MainService::class.java)

  val fromSocket: Subject<String> = PublishSubject.create()
  val toSocket: Subject<String> = PublishSubject.create()

  private var serverSocketThread: Thread? = null
  private var remoteSocketThread: Thread? = null

  // Accept connection once, trigger a menu action to initiate a new server socket
  fun initServerSocket() {
    serverSocketThread = thread {

      val server = ServerSocket(4444)
      server.reuseAddress = true

      val socket = server.accept()

      MainService.state.onNext(State.WRITER)

      socket.wireMessagingStreams(toSocket, fromSocket)

      MainService.state.onNext(State.IDLE)
    }
  }

  fun initRemoteSocket(address: String, port: Int) {
    remoteSocketThread = thread {

      val socket: Socket
      try {
        socket = Socket(address, port)
      } catch (e: Exception) {
        LOG.error("Failed to join remote session", e)

        ApplicationManager.getApplication().invokeLater {
          Messages.showErrorDialog("Failed to join remote session", "Oh, no!")
        }

        return@thread
      }

      MainService.state.onNext(State.READER)

      socket.wireMessagingStreams(toSocket, fromSocket)

      MainService.state.onNext(State.IDLE)
    }
  }

  fun disconnect() {
    MainService.state.onNext(State.IDLE)

    // Not necessary currently, but when/if we run an infinite loop to accept many connections - will have to use this.
    //    serverSocketThread?.interrupt()
    //    remoteSocketThread?.interrupt()
  }

  fun connectRemote(address: String, port: String) {
    initRemoteSocket(address, port.toInt())
  }

  fun send(text: String) {
    if (MainService.state.value == State.WRITER) {
      toSocket.onNext(text)
    }
  }

  companion object {
    fun getInstance(): NetworkService {
      return ServiceManager.getService(NetworkService::class.java)
    }
  }

  init {

    // Accept only in READER mode
    fromSocket
//            .filter { MainService.state.value == State.READER }
            .map { Klaxon().parse<Message>(it)!! }
            .doOnError { println(it) }
            .subscribe {
//              if (MainService.state.value == State.READER) {
                MainService.incomingMessage.onNext(it)
//              }
            }
  }

  private fun parseRawMessage(rawMessage: String): Message {
    LOG.debug("Raw message: $rawMessage")

//    try {
    val message = Klaxon().parse<Message>(rawMessage)

//      message?.let(MainService.incomingMessage::onNext)

//      println(message)
//    } catch (t: Throwable) {
//      LOG.info("Received non-parsable message=$rawMessage")
//    }

//    return Message()
    return message!!
  }
}
