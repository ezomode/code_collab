package extension

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject
import org.jetbrains.rpc.LOG
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket
import java.util.*
import kotlin.concurrent.fixedRateTimer

fun Socket.wireMessagingStreams(toSocket: Observable<String>, fromSocket: Subject<String>) {

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
      LOG.error("Socket writing error: ", e)
    }
  }

  val subscription = toSocket
          .subscribeOn(Schedulers.io())
          .subscribe(::writeToSocket)

  try {

    val reader = InputStreamReader(this.getInputStream())

    reader.buffered().lines().forEach(fromSocket::onNext)

  } catch (ex: IOException) {

    LOG.error("Socket error: ", ex)

  } finally {

    close()
    subscription.dispose()
    socketHeartbeat.cancel()

    LOG.debug("Socket is closed now.")
  }
}