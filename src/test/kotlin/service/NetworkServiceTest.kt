package service

import com.beust.klaxon.Klaxon
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import junit.framework.TestCase
import model.Message
import model.MessageType
import model.State
import org.junit.Ignore
import org.junit.Test
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timer


class NetworkServiceTest : LightPlatformCodeInsightFixtureTestCase() {

  fun `test state for remote socket and GRAB_LOCK message`() {

    val stateCounter = AtomicInteger(0)

    MainService.getInstance().state.subscribe {
      when (stateCounter.get()) {
        0 -> TestCase.assertEquals(State.IDLE, it)
        1 -> TestCase.assertEquals(State.WRITER, it)
        2 -> TestCase.assertEquals(State.READER, it)
        3 -> TestCase.assertEquals(State.IDLE, it)
      }
      stateCounter.getAndAdd(1)
    }

    val networkService = NetworkService.getInstance()
    networkService.initServerSocket()

    val socket = Socket("localhost", 4444)

    val dataOutputStream = DataOutputStream(socket.getOutputStream())
    dataOutputStream.writeBytes(Message(MessageType.GRAB_LOCK).json())
    dataOutputStream.flush()
    dataOutputStream.close()

    Thread.sleep(600)

    networkService.disconnect()
  }


  fun `test communication through remote socket`() {

    val serverSocket = ServerSocket(4444)

    val ns = NetworkService.getInstance()
    ns.connectRemote("localhost", "4444")

    val timer = fixedRateTimer(name = "socket-timer", initialDelay = 0, period = 200) {
      ns.send("tick " + Date() + "\n")
    }

    val counter = AtomicInteger(0)

    val socket = serverSocket.accept()

    try {

      val isr = InputStreamReader(socket.getInputStream())

      isr.buffered().lines().forEach {
        counter.addAndGet(1)

        println(it + "!")
        assert(it.startsWith("tick"))

        if (counter.get() == 3) {
          assertEquals(State.READER, MainService.getInstance().state.value)

          timer.cancel()
          isr.close()
          ns.disconnect()
          socket.close()
          serverSocket.close()
          return@forEach
        }
      }
    } catch (e: Exception) { // No graceful shutdown, ignore exception. TODO
//      e.printStackTrace()
//      TestCase.fail()
    }

    assertEquals(3, counter.get())

    assertEquals(State.IDLE, MainService.getInstance().state.value)
  }

  // Still could not figure out the WriteCommandAction vs project being disposed...
  private fun `test doc updates over socket from WRITER`() {

    val file = myFixture.configureByText("x.txt", "xxxxxxxx").virtualFile

    val networkService = NetworkService.getInstance()
    networkService.initServerSocket()

    timer(initialDelay = 2000, period = 1000) { updateFile(file) }

    val socket = Socket("localhost", 4444)
    val isr = InputStreamReader(socket.getInputStream())
    isr.buffered().lines().forEach {

      val message = Klaxon().parse<Message>(it)!!

      assertEquals(MessageType.UPDATE_DOC, message.type)
      assert(message.text.startsWith("Doc update"))

      println(it)
    }
  }

  fun updateFile(file: VirtualFile) {
    WriteCommandAction.runWriteCommandAction(project) {
      VfsUtil.saveText(file, "Doc update " + Date())
    }
  }

  @Ignore
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
