package models

import play.api.libs.json._

/**
 * Created by Onur Cem on 12/14/2014.
 */
case class Group(id: String, name: String)

object Group {
  implicit val GroupObjectWrites = new Writes[Group] {
    def writes(groupObject: Group) = Json.obj(
      "id"   -> groupObject.id,
      "name" -> groupObject.name)
  }
}
