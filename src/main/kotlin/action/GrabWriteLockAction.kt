package action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import model.State
import service.CollabService

// One sided lock, cannot prevent from grabbing, cannot give it forcefully
class GrabWriteLockAction : AnAction("Grab Lock") {

  override fun actionPerformed(event: AnActionEvent) {

    event.project?.let { CollabService.getInstance().grabLock(it.name) }
  }

  override fun update(e: AnActionEvent) {

    e.presentation.isVisible = CollabService.state.value == State.READER
  }
}