package service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import model.Message
import model.MessageType
import model.State
import java.io.File
import java.util.concurrent.TimeUnit

class MainService {

  private val LOG = Logger.getInstance(MainService::class.java)

  var state = State.IDLE
    set(v) = stateSubject.onNext(v)

  var stateSubject = BehaviorSubject.createDefault(State.IDLE)

  var previousDocument: Document? = null
  var previousDocumentText = ""

  val documentUpdates: Subject<Triple<Project, Document, VirtualFile>> = PublishSubject.create()

  val messageSubject: Subject<Message> = PublishSubject.create()

  init {
    documentUpdates.debounce(200, TimeUnit.MILLISECONDS).subscribe(this@MainService::sendDocument)

    messageSubject.subscribe(::handleIncomingMessage)
  }

  private fun sendDocument(triple: Triple<Project, Document, VirtualFile>) {

    val project = triple.first
    val document = triple.second
    val virtualFile = triple.third

    val text = document.text

    if (document !== previousDocument && previousDocumentText != text) {

      val projectRelativePath = getProjectRelativePath(project, virtualFile)

      val message = Message(MessageType.UPDATE_DOC, document.text, projectRelativePath).json()

      NetworkService.getInstance().send(message)

      println("Send update for $projectRelativePath")
    }
  }

  private fun handleIncomingMessage(m: Message) {

    when (m.type) {
      MessageType.GRAB_LOCK -> {
        if (state == State.WRITER) state = (State.READER)
      }

      MessageType.UPDATE_DOC -> {

        val virtualFile = findVirtualFile(m)

        virtualFile?.let {

          ApplicationManager.getApplication().invokeLater {
            FileEditorManager
                    .getInstance(getFirstOpenProject())
                    .openFile(virtualFile, true)

            val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            document?.setText(m.payload)
          }
        }
      }

// Simplify messaging - open file on each update.
/*
      MessageType.OPEN_DOC -> {
        val virtualFile = findVirtualFile(m)

        if (virtualFile != null) {
          ApplicationManager.getApplication().invokeLater {
            FileEditorManager
                    .getInstance(getFirstOpenProject())
                    .openFile(virtualFile, true)
          }
        } else {
          println("VirtualFile not found at relative path ${m.path}")
        }
      }
*/
    }

  }

  fun findVirtualFile(m: Message): VirtualFile? {
//    val project = getFirstOpenProject()
//
//    val projectBasePath = project.basePath!!
//    val filePath = projectBasePath + m.path
//    VfsUtilCore.fixIDEAUrl()


    val ioFile = File(FileUtil.toSystemDependentName(m.path))
    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile)

    return virtualFile
  }

  companion object {
    fun getInstance(): MainService {
      return ServiceManager.getService(MainService::class.java)
    }
  }

  fun getProjectRelativePath(project: Project, virtualFile: VirtualFile): String {

    val projectBasePath = project.basePath.toString()
    val filePath = virtualFile.path

//            if (!filePath.startsWith(projectBasePath))
//                throw IllegalArgumentException("Not a permanent project file") // Scratch files and other non-project files are not supported.

    val projectRelativePath = filePath.removePrefix(projectBasePath)

    LOG.debug("")

    return projectRelativePath
  }

  fun getStatus(): String {
    return this.toString()
  }

  fun grabLock() {
    if (state == State.READER) {

      val message = Message(MessageType.GRAB_LOCK).json()

      NetworkService.getInstance().send(message)

      state = State.WRITER
    }
  }

  fun getFirstOpenProject(): Project {
    return ProjectManager.getInstance().openProjects.first()
  }
}

