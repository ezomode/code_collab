import com.google.gson.JsonParser
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.platform.ProjectSetReader
import com.intellij.projectImport.ProjectSetProcessor
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.containers.ContainerUtil
import model.Message
import model.MessageType
import model.State
import service.MainService
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

// Just another failed attempt to write a test... Again getting project disposed.
class DocumentUpdateTest : LightPlatformTestCase() {

  private val testDataPath = "testData/projectSet"

  private var testProject: Project? = null

  override fun setUp() {
    super.setUp()

    testProject = doOpenProject("/project.json", "untitled")
  }

  private fun testOpenProject() {
    try {

//      ProjectManager.getInstance().closeProject(getProject())

      println(homePath)
      println(testProject!!.locationHash)

      val editorManager = FileEditorManager.getInstance(testProject!!)
      assert(editorManager.openFiles.size == 0)

      MainService.state.onNext(State.READER)

//    assert(ProjectManager.getInstance().openProjects.size == 1)

//      val fileName = "Test.java"
//      val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(fileName)
//      val psiFile = PsiFileFactory.getInstance(getProject()).createFileFromText(fileName, fileType, "class Test {}", 0, false)
//      val virtualFile = psiFile.viewProvider.virtualFile
//      val document = FileDocumentManager.getInstance().getDocument(virtualFile)!!
//      assertNotNull(document)

//      val path = "file:///Users/ak/my/centaur/testData/projectSet/untitled/src/java/A.java"
      val path = "/src/java/A.java"
      MainService.incomingMessage.onNext(Message(MessageType.UPDATE_DOC, "untitled", path, "QWEQWE"))

//      assert(editorManager.openFiles.size == 1)
      val currentFile = editorManager.openFiles[0]
      assert(currentFile != null)
      val document = FileDocumentManager.getInstance().getDocument(currentFile!!)!!
      assert(document.text == "QWEQWE")

//      assert(editorManager.currentFile != null)
//      assert(!editorManager.currentFile!!.isWritable)

    } catch (e: Throwable) {
      throw e;
    } finally {
      (ProjectManager.getInstance() as ProjectManagerEx).closeAndDispose(testProject!!)
    }
  }

//  override fun tearDown() {
//    super.tearDown()
//
//    (ProjectManager.getInstance() as ProjectManagerEx).closeAndDispose(testProject!!)
//  }

  private fun doOpenProject(file: String, projectName: String): Project {
    val context = ProjectSetProcessor.Context()
    context.directory = VfsUtil.findFileByIoFile(File(testDataPath), true)
    readDescriptor(File(testDataPath + file), context)

    val projects = ProjectManager.getInstance().openProjects

    val project = ContainerUtil.find(projects) { project1 -> projectName == project1.name }
    assertNotNull(project)

    return project!!
    //    (ProjectManager.getInstance() as ProjectManagerEx).closeAndDispose(project!!)
  }

  @Throws(IOException::class)
  private fun readDescriptor(descriptor: File, context: ProjectSetProcessor.Context?) {
    InputStreamReader(FileInputStream(descriptor), CharsetToolkit.UTF8_CHARSET).use { input ->
      val parse = JsonParser().parse(input)
      ProjectSetReader().readDescriptor(parse.asJsonObject, context)
    }
  }
}
