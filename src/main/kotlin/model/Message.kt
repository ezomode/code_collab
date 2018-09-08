package model

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon

data class Message(@Json val type: MessageType,
                   @Json val projectName: String,
                   @Json val path: String = "",
                   @Json val text: String = "") {

  fun json(): String {
    return Klaxon().toJsonString(this)
  }
}