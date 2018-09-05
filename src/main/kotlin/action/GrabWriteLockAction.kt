package action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import model.State
import service.MainService

// One sided lock, cannot prevent from grabbing, cannot give it forcefully
class GrabWriteLockAction : AnAction("Grab Lock") {

  override fun actionPerformed(event: AnActionEvent) {

    MainService.getInstance().grabLock()
  }

  override fun update(e: AnActionEvent) {

    val mainService = MainService.getInstance()

    e.presentation.isVisible = mainService.state == State.READER
  }
}