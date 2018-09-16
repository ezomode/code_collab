package action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import model.State
import service.MainService
import service.NetworkService

class OpenSocketAction : AnAction("Create Session") {

  override fun actionPerformed(event: AnActionEvent) {

    NetworkService.getInstance().initServerSocket()
  }

  override fun update(e: AnActionEvent) {

    e.presentation.isVisible = MainService.state.value == State.IDLE
  }
}