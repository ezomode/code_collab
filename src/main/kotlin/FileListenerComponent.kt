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
import extension.addReadabilityHook
import model.Message
import model.MessageType
import org.jetbrains.annotations.NotNull
import service.CollabService

class FileListenerComponent(project: Project) : AbstractProjectComponent(project) {

  private val LOG = Logger.getInstance(FileListenerComponent::class.java)

  override fun projectOpened() {
    myProject.messageBus
            .connect()
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, FileEditorManagerListenerImpl(myProject))
  }
}

private class FileEditorManagerListenerImpl(val myProject: Project) : FileEditorManagerListener {

  private val LOG = Logger.getInstance(FileEditorManagerListenerImpl::class.java)

  override fun fileOpened(@NotNull manager: FileEditorManager, @NotNull virtualFile: VirtualFile) {
    LOG.debug("fileOpened: ${virtualFile.path}")

    val document = FileDocumentManager.getInstance().getDocument(virtualFile)

    document?.let {
      document.addReadabilityHook()

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

      val project = event.manager.project

      val projectRelativePath = CollabService.getProjectRelativePath(project, file)

      val message = Message(type = MessageType.OPEN_DOC, projectName = project.name, path = projectRelativePath)
      val messageJson = message.json()

      CollabService.getInstance().send(messageJson)
    }
  }
}

private class DocumentUpdateListener(private val project: Project,
                                     private val document: Document,
                                     private val virtualFile: VirtualFile) : DocumentListener {

  override fun documentChanged(event: DocumentEvent) {
    CollabService.documentUpdate.onNext(Triple(project, document, virtualFile))
  }
}