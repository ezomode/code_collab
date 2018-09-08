import com.beust.klaxon.Klaxon
import model.Message
import model.MessageType
import org.junit.Test
import kotlin.test.assertEquals

class MessageJsonCycleTest {

  @Test
  fun testJsonCycle() {
    val message = Message(MessageType.UPDATE_DOC, projectName = "", text = "123\n456")
    val json = message.json()

    val parsedMessage = Klaxon().parse<Message>(json)

    assertEquals(message, parsedMessage)
  }

}
