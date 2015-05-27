package models

import play.api.libs.json.{Json, Writes}

/**
 * Created by Onur Cem on 2/17/2015.
 */
case class Property(id: String, name: String)

object Property {
  implicit val PropertyObjectWrites = new Writes[Property] {
    def writes(propertyObject: Property) = Json.obj(
      "id"   -> propertyObject.id,
      "name" -> propertyObject.name)
  }
}