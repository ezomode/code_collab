package model

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon

data class Message(@Json val type: MessageType,
                   @Json val payload: String = "",
                   @Json val path: String = "") {

  fun json(): String {
    return Klaxon().toJsonString(this)
  }
}