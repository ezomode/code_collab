package action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import service.MainService

class StatusAction : AnAction() {

  override fun actionPerformed(event: AnActionEvent) {
  }

  override fun update(e: AnActionEvent) {

    e.presentation.text = "State: " + MainService.state.value
  }
}