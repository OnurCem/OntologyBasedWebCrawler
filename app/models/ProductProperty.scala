package models

import play.api.libs.json.{Json, Writes}

/**
 * Created by Onur Cem on 2/17/2015.
 */
case class ProductProperty(productId: String, propertyId: String)

object ProductProperty {
  implicit val ProductPropertyObjectWrites = new Writes[ProductProperty] {
    def writes(productPropertyObject: ProductProperty) = Json.obj(
      "productId"  -> productPropertyObject.productId,
      "propertyId" -> productPropertyObject.propertyId)
  }
}