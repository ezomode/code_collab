import com.beust.klaxon.Klaxon
import org.junit.Test
import model.Message
import model.MessageType
import kotlin.test.assertEquals

class MessageTest {

    @Test
    fun testJsonCycle() {
        val message = Message(MessageType.UPDATE_DOC, "123\n456")
        val json = message.json()

        val parsedMessage = Klaxon().parse<Message>(json)

        assertEquals(message, parsedMessage)
    }

}
