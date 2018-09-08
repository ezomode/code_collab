package model

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon

data class Message(@Json val type: MessageType,
                   @Json val projectName: String = "NA",
                   @Json val path: String = "NA",
                   @Json val text: String = "") {

  fun json(): String {
    return Klaxon().toJsonString(this)
  }
}