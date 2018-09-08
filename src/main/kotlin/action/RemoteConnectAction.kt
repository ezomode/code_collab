package action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import model.State
import service.MainService
import service.NetworkService

class RemoteConnectAction : AnAction("Join Session") {

  var defaultAddressPort = "localhost:4444"

  override fun actionPerformed(event: AnActionEvent) {
    val project = event.getData(PlatformDataKeys.PROJECT)
    val data = Messages.showInputDialog(project, "<ip_address>:<port>", "Connect to edit together", Messages.getInformationIcon(), defaultAddressPort, null)

    data?.let {

      val split = data.split(":").dropLastWhile(String::isEmpty)

      if (split.size == 2) {
        defaultAddressPort = data
        NetworkService.getInstance().connectRemote(split[0], split[1])
      }
    }

    //		Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.", "Information", Messages.getInformationIcon());
  }

  override fun update(e: AnActionEvent) {

    val service = MainService.getInstance()

    e.presentation.isVisible = service.state == State.IDLE
  }
}