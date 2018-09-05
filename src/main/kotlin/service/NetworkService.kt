package service

import com.beust.klaxon.Klaxon
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import model.Message
import model.State
import org.jetbrains.rpc.LOG
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.fixedRateTimer
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

      socket.readWriteSocket(toSocket, fromSocket)

      MainService.getInstance().state = State.WRITER
    }
  }

  fun initRemoteSocket(address: String, port: Int) {
    remoteSocketThread = thread {

      val socket = Socket(address, port)

      socket.readWriteSocket(toSocket, fromSocket)

      MainService.getInstance().state = State.READER
    }
  }

  // inline maybe
  fun disconnect() {
    MainService.getInstance().state = State.IDLE

    // Not necessary currently, but when/if we run an infinite loop to accept many connections - will have to use this.
    //    serverSocketThread?.interrupt()
    //    remoteSocketThread?.interrupt()
  }

  fun connect(address: String, port: String) {
    initRemoteSocket(address, port.toInt())
  }

  fun send(text: String) {
    toSocket.onNext(text)
  }

  companion object {
    fun getInstance(): NetworkService {
      return ServiceManager.getService(NetworkService::class.java)
    }
  }

  init {
    val messageSubject = MainService.getInstance().messageSubject

    fromSocket.subscribe {
      parseRawMessage(it, messageSubject)
    }
  }

  private fun parseRawMessage(it: String, messageSubject: Subject<Message>) {
    println(Thread.currentThread().name + ": " + it)

    try {
      val message = Klaxon().parse<Message>(it)

      message?.let(messageSubject::onNext)

      println(message)
    } catch (t: Throwable) {
      LOG.info("Received non-parsable message=$it")
    }
  }
}

private fun Socket.readWriteSocket(toSocket: Observable<String>, fromSocket: Subject<String>) {

  fun socketHeartbeat(): Timer {
    return fixedRateTimer(name = "socket-heartbeat", initialDelay = 0, period = 2000000) {
      // TODO: socket heartbeat, update state accordingly
    }
  }

  val socketHeartbeat = socketHeartbeat()

  val out = DataOutputStream(getOutputStream())
  fun writeToSocket(str: String) {
    try {
      out.writeBytes(str)
    } catch (e: Exception) {
      LOG
      MainService.getInstance().state = State.IDLE
    }
  }

  val subscription = toSocket
          .subscribeOn(Schedulers.io())
          .subscribe(::writeToSocket)

  val reader = InputStreamReader(this.getInputStream())
  try {

    reader.buffered().lines().forEach(fromSocket::onNext)

  } catch (ex: IOException) {

    LOG.error("Socket error: ", ex)

  } finally {

    close()
    subscription.dispose()
    socketHeartbeat.cancel()

    MainService.getInstance().state = State.IDLE

    LOG.debug("Socket is closed now.")
  }
}

