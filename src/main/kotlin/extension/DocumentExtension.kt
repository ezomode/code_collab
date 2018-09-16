package extension

import com.intellij.openapi.editor.Document
import model.State
import service.MainService

fun Document.addReadabilityHook() {

  MainService.state.subscribe { state ->
    when (state) {
      State.READER -> this.setReadOnly(true)
      else -> this.setReadOnly(false)
    }

  }

}