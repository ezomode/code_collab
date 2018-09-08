import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import model.Message
import model.MessageType
import model.State
import org.jetbrains.annotations.NotNull
import service.MainService
import service.NetworkService

class MyProjectComponent(project: Project) : AbstractProjectComponent(project) {

  private val LOG = Logger.getInstance(MyProjectComponent::class.java)

  override fun projectOpened() {
    myProject.messageBus
            .connect()
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, FileEditorManagerListenerImpl(myProject))

/*
    myProject.messageBus
            .connectRemote()
            .subscribe(AppTopics.FILE_DOCUMENT_SYNC, object : FileDocumentManagerListener {
              override fun fileContentLoaded(file: VirtualFile, document: Document) {
                super.fileContentLoaded(file, document)

                LOG.debug("fileContentLoaded: ${file.path}")
              }
            })
*/
  }
}

private class FileEditorManagerListenerImpl(val myProject: Project) : FileEditorManagerListener {

  private val LOG = Logger.getInstance(FileEditorManagerListenerImpl::class.java)

  override fun fileOpened(@NotNull manager: FileEditorManager, @NotNull virtualFile: VirtualFile) {
    LOG.debug("fileOpened: ${virtualFile.path}")

    val document = FileDocumentManager.getInstance().getDocument(virtualFile)

    document?.let {

      MainService.getInstance().stateSubject.subscribe { state ->
        when (state) {
          State.READER -> document.setReadOnly(true)
          else -> document.setReadOnly(false)
        }

      }

      document.addDocumentListener(DocumentUpdateListener(myProject, document, virtualFile))
    }

  }

  override fun fileClosed(@NotNull source: FileEditorManager, @NotNull file: VirtualFile) {
    LOG.debug("fileClosed: ${file.path}")
  }

  override fun selectionChanged(@NotNull event: FileEditorManagerEvent) {
    super.selectionChanged(event)

    LOG.debug("selectionChanged old: ${event.oldEditor?.file?.path}")
    LOG.debug("selectionChanged new: ${event.newEditor?.file?.path}")

    event.newEditor?.file?.let { file ->

      MainService.getInstance()

      val project = event.manager.project

      val projectRelativePath = MainService.getInstance().getProjectRelativePath(project, file)

      val message = Message(type = MessageType.OPEN_DOC, projectName = project.name, path = projectRelativePath)
      val messageJson = message.json()

      NetworkService.getInstance().send(messageJson)
    }
  }
}

private class DocumentUpdateListener(private val project: Project,
                             private val document: Document,
                             private val virtualFile: VirtualFile) : DocumentListener {

  override fun documentChanged(event: DocumentEvent) {
    MainService.getInstance().documentUpdates.onNext(Triple(project, document, virtualFile))
  }
}