import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import model.Message
import model.MessageType
import model.State
import service.MainService

class IncomingMessageHandlingTest : LightCodeInsightFixtureTestCase() {

  override fun getTestDataPath(): String {
    return "testData/projectSet"
  }

  // AssertionFailedError: Test method isn't public ???
  private fun `test document readability at different plugin states`() {

    myFixture.configureByText("x.txt", "xxxxxxxx")
    val document = myFixture.editor.document

    MainService.state.onNext(State.READER)
    assert(!document.isWritable)

    MainService.state.onNext(State.WRITER)
    assert(document.isWritable)

    MainService.state.onNext(State.READER)
    assert(!document.isWritable)

    MainService.state.onNext(State.IDLE)
    assert(document.isWritable)
  }

  // TODO: figure out - test project gets disposed.
  private fun `test editor open on UPDATE_DOC message`() {
    val editorManager = FileEditorManagerEx.getInstanceEx(project)

    myFixture.configureByText("x.txt", "xxxxxxxx")

    assert(editorManager.currentFile != null)

    MainService.state.onNext(State.READER)

//    assert(document == null)
    assert(project.isOpen)

//    assert(!editorManager.hasOpenedFile())
//    assert(editorManager.currentFile == null)

    val projectName = project.name
    MainService.incomingMessage.onNext(Message(MessageType.UPDATE_DOC, path = "temp:///src/x.txt", projectName = projectName))

    assert(editorManager.hasOpenedFile())
    assert(editorManager.currentFile != null)
    assert(!editorManager.currentFile!!.isWritable)
  }

  fun `test doc content change on DOC_UPDATE message`() {
    myFixture.configureByText("x.txt", "xxxxxxxx")
  }

  //        getProject()

//        val file = getFile("/foo.txt")
//        myManager.openFile(file, false)
//        val dockManager = DockManager.getInstance(getProject())
//        TestCase.assertEquals(1, dockManager.containers.size)


}

class Test : LightCodeInsightTestCase() {

  // Test project gets disposed
  private fun `test virtual doc update`() {
    val filename = "/projectSet/untitled/src/java/A.java"
    configureByFile(filename)
    val vfile = VirtualFileManager.getInstance().findFileByUrl("file://$testDataPath$filename")!!

    MainService.state.onNext(State.READER)

    MainService.incomingMessage.onNext(Message(MessageType.OPEN_DOC, "light_temp", vfile.url))
  }
}