package action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import service.MainService

// For debug purposes
class StatusAction : AnAction() {

  override fun actionPerformed(event: AnActionEvent) {
    val service = MainService.getInstance()

    println(service.getStatus())

    Messages.showMessageDialog(event.project, service.getStatus(), "Plugin Status Info", Messages.getInformationIcon())
  }

  override fun update(e: AnActionEvent) {

    e.presentation.text = "State: " + MainService.getInstance().state.value
  }
}