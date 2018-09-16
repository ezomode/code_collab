package service

import com.beust.klaxon.Klaxon
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
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
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.containers.ContainerUtil
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import model.Message
import model.MessageType
import model.State
import java.io.File
import java.util.concurrent.TimeUnit


class CollabService {

  private val LOG = Logger.getInstance(CollabService::class.java)

  init {
    documentUpdate.debounce(200, TimeUnit.MILLISECONDS).subscribe(this@CollabService::sendDocument)

    incomingMessage.subscribe(::handleIncomingMessage)

    state.subscribe { LOG.debug(it.name) }
    documentUpdate.subscribe { LOG.debug(it.toString()) }
    incomingMessage.subscribe { LOG.debug(it.json()) }
  }

  private fun sendDocument(triple: Triple<Project, Document, VirtualFile>) {

    val project = triple.first
    val document = triple.second
    val virtualFile = triple.third

    val path = getProjectRelativePath(project, virtualFile)

    val message = Message(
            type = MessageType.UPDATE_DOC,
            projectName = project.name,
            path = path,
            text = document.text
    ).json()

    send(message)

    LOG.debug("Send message: $message")
  }

  fun send(text: String) {
    if (CollabService.state.value == State.WRITER) {
      NetworkService.toSocket.onNext(text)
    }
  }

  private fun handleIncomingMessage(m: Message) {

    when (m.type) {
      MessageType.GRAB_LOCK -> {
        if (state.value == State.WRITER) {
          state.onNext(State.READER)
        }
      }

      // TODO: see if possible to simplify - almost duplicate code as for OPEN_DOC.
      MessageType.UPDATE_DOC -> {

        val project = findProject(m.projectName)
        val virtualFile = project?.let { findVirtualFile(project, m) }

        if (project != null) {
          if (virtualFile != null) {

            WriteCommandAction.runWriteCommandAction(project) {
              val document = FileDocumentManager.getInstance().getDocument(virtualFile)
              document?.setReadOnly(false)
              document?.setText(m.text)
              document?.setReadOnly(true)
            }
//            VfsUtil.saveText(virtualFile, m.text)

            openVirtualFile(project, virtualFile)

          } else {
            LOG.error("VirtualFile not found at relative path ${m.path}")
          }
        } else {
          LOG.error("Project with name=${m.projectName} not found!")
        }
      }

      MessageType.OPEN_DOC -> {
        val project = findProject(m.projectName)
        val virtualFile = project?.let { findVirtualFile(project, m) }

        if (project != null) {
          if (virtualFile != null) {

            openVirtualFile(project, virtualFile)

          } else {
            LOG.error("VirtualFile not found at relative path ${m.path}")
          }
        } else {
          LOG.error("Project with name=${m.projectName} not found!")
        }
      }
    }

  }

  fun openVirtualFile(project: Project, virtualFile: VirtualFile) {
    ApplicationManager.getApplication().invokeLater {
      FileEditorManager
              .getInstance(project)
              .openFile(virtualFile, true)
    }
  }

  fun findVirtualFile(project: Project, m: Message): VirtualFile? {

    val projectBasePath = project.basePath!!
    val filePath = projectBasePath + m.path

//    VfsUtilCore.fixIDEAUrl()

    val ioFile = File(FileUtil.toSystemDependentName(filePath))

    if (ioFile.exists())
      return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile)


    return VirtualFileManager.getInstance().refreshAndFindFileByUrl(m.path) // fall back for temp files from tests
  }

  companion object {
    fun getInstance(): CollabService {
      return ServiceManager.getService(CollabService::class.java)
    }

    var state = BehaviorSubject.createDefault(State.IDLE)!! // interesting warning on nullability check, could not infer non-nullable without !!

    val documentUpdate: Subject<Triple<Project, Document, VirtualFile>> = PublishSubject.create()

    val incomingMessage: Subject<Message> = PublishSubject.create()

    fun getProjectRelativePath(project: Project, virtualFile: VirtualFile): String {

      val projectBasePath = project.basePath.toString()
      val filePath = virtualFile.path

//            if (!filePath.startsWith(projectBasePath))
//                throw IllegalArgumentException("Not a permanent project file") // Scratch files and other non-project files are not supported.

      return filePath.removePrefix(projectBasePath)
    }
  }

  fun getStatus(): String {
    return Klaxon().toJsonString(this)
  }

  fun grabLock(projectName: String) {
    if (state.value == State.READER) {

      val message = Message(MessageType.GRAB_LOCK, projectName).json()

      NetworkService.toSocket.onNext(message)

      state.onNext(State.WRITER)
    }
  }

  private fun findProject(projectName: String): Project? {

    val projects = ProjectManager.getInstance().openProjects

    return ContainerUtil.find(projects) { project1 -> projectName == project1.name }
  }
}