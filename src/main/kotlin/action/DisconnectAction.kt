package action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import model.State
import service.CollabService
import service.NetworkService

class DisconnectAction : AnAction("Disconnect") {

  override fun actionPerformed(event: AnActionEvent) {

    NetworkService.getInstance().disconnect()
  }

  override fun update(e: AnActionEvent) {

    e.presentation.isVisible = CollabService.state.value != State.IDLE
  }
}