package models

import play.api.libs.json.{Json, Writes}

/**
 * Created by Onur Cem on 2/17/2015.
 */
case class Product(id: String, brand: String, model: String, groupId: String, properties: List[String])

object Product {
  implicit val ProductObjectWrites = new Writes[Product] {
    def writes(productObject: Product) = Json.obj(
      "id"         -> productObject.id,
      "brand"      -> productObject.brand,
      "model"      -> productObject.model,
      "groupId"    -> productObject.groupId,
      "properties" -> productObject.properties)
  }
}