package service

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.junit.Ignore
import org.junit.Test
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.fixedRateTimer


class NetworkServiceTest : LightPlatformCodeInsightFixtureTestCase() {

  fun testRemoteSocket() {

    val serverSocket = ServerSocket(4444)

    val ns = NetworkService.getInstance()
    ns.connect("localhost", "4444")

    ns.fromSocket.subscribe(::println)

    fixedRateTimer(name = "socket-timer", initialDelay = 0, period = 2000) {
      ns.send("tick " + Date() + "\n")
    }

    val socket = serverSocket.accept()
    try {
      val isr = InputStreamReader(socket.getInputStream())
      isr.buffered().lines().forEach {
        println(it + "!")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @Test
  fun testServerSocket() {

    val ns = NetworkService()
    ns.initServerSocket()

    ns.fromSocket.subscribe { println("fromSocket $it") }
//        ns.toSocket.subscribe { println("toSocket $it") }

    val socket = Socket("localhost", 4444)
    val dataOutputStream = DataOutputStream(socket.getOutputStream())

    fixedRateTimer(name = "socket-timer", initialDelay = 0, period = 2000) {
      ns.send("NetworkService tick " + Date() + "\n")

      dataOutputStream.writeBytes("test socket tick " + Date() + "\n")
    }

    try {
      val isr = InputStreamReader(socket.getInputStream())
      isr.buffered().lines().forEach {
        println("test socket received: $it")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @Ignore
  @Test
  fun openSocketTest() {
    val socket = Socket("localhost", 4444)
    val dataOutputStream = DataOutputStream(socket.getOutputStream())
    try {
      val isr = InputStreamReader(socket.getInputStream())
      isr.buffered().lines().forEach {
        println("test socket received: $it")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    fixedRateTimer(name = "socket-timer", initialDelay = 0, period = 2000) {
      dataOutputStream.writeBytes("test socket tick " + Date() + "\n")
    }
  }
}
