package models

import play.api.libs.json.{Json, Writes, JsValue}

/**
 * Created by Onur Cem on 5/17/2015.
 */
case class SearchResult(id: String, brand: String, model: String, group: String, properties: Seq[String])

object SearchResult {
  implicit val SearchResultWrites = new Writes[SearchResult] {
    def writes(searchResultObject: SearchResult) = Json.obj(
      "id"         -> searchResultObject.id,
      "brand"      -> searchResultObject.brand,
      "model"      -> searchResultObject.model,
      "group"      -> searchResultObject.group,
      "properties" -> searchResultObject.properties)
  }
}