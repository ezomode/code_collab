import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import model.State
import service.MainService

class IncomingMessageTest : LightPlatformCodeInsightFixtureTestCase() {

  fun `test editor open on OPEN_DOC message`() {
    val editorManager = FileEditorManagerEx.getInstanceEx(project)

    myFixture.configureByText("x.txt", "xxxxxxxx")

    assert(editorManager.currentFile != null)

    editorManager.closeAllFiles()

    val editor = myFixture.editor
    val document = editor.document

//    myFixture.performEditorAction()

    val mainService = MainService.getInstance()
    mainService.state = State.READER

//    assert(document == null)
    assert(project.isOpen)

    assert(!editorManager.hasOpenedFile())
    assert(editorManager.currentFile == null)

//    LightPlatformCodeInsightTestCase.executeAction(IdeActions., editor, project)

//    mainService.messageSubject.onNext(Message(MessageType.OPEN_DOC, path = "/src/x.txt"))
    assert(editorManager.currentFile != null)
    assert(!editorManager.currentFile!!.isWritable)
  }

  fun `test doc content change on DOC_UPDATE message`() {
    myFixture.configureByText("x.txt", "xxxxxxxx")
  }

  fun `test document readability at different plugin states`() {
//        getProject()

//        val file = getFile("/foo.txt")
//        myManager.openFile(file, false)
//        val dockManager = DockManager.getInstance(getProject())
//        TestCase.assertEquals(1, dockManager.containers.size)

    myFixture.configureByText("x.txt", "xxxxxxxx")
    val document = myFixture.editor.document

    MainService.getInstance().state = State.READER
    assert(!document.isWritable)
    MainService.getInstance().state = State.WRITER
    assert(document.isWritable)
    MainService.getInstance().state = State.READER
    assert(!document.isWritable)
    MainService.getInstance().state = State.IDLE
    assert(document.isWritable)
  }
}