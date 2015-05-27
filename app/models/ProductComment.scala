package models

import play.api.libs.json.{Json, Writes, JsValue}

/**
 * Created by Onur Cem on 5/19/2015.
 */
case class ProductComment(property: String, comments: List[JsValue])

object ProductComment {
  implicit val ProductCommentWrites = new Writes[ProductComment] {
    def writes(productCommentObject: ProductComment) = Json.obj(
      "property"  -> productCommentObject.property,
      "comments"  -> productCommentObject.comments)
  }
}