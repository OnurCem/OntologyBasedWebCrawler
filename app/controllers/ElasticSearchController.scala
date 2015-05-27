package controllers

import com.sksamuel.elastic4s.{ElasticClient}
import com.sksamuel.elastic4s.ElasticDsl._
import models.{HBComment, HBProduct, HBObject}
import play.api.libs.json.JsArray
import play.api.libs.json.Json


/**
 * Created by Onur Cem on 12/11/2014.
 */
class ElasticSearchController {
  val client = ElasticClient.local

  def createIndex(indexName: String) = {
    client.execute { create index indexName }
  }

  def save(hb: HBObject) = {
    val product = HBProduct(hb.url, hb.name, hb.id, hb.category, hb.subCategory)
    saveProduct(product)

    val comments = hb.comments
    comments.map {
      c => saveComment(HBComment(c.id, hb.id, c.writer, c.title, c.date, c.rating, c.content))
    }
  }

  def saveProduct(p: HBProduct) = {
    /*implicit object HBObjectIndexable extends Indexable[HBObject] {
      override def json(hb: HBObject) = Json.toJson(hb).toString
    }*/
    client.execute { index into "hepsiburada/product" id p.id doc p }
  }

  def saveComment(c: HBComment) = {
    client.execute { index into "hepsiburada/comment" doc c }
  }

  def findProduct(name: String) = {
    val result = client.execute { search in "hepsiburada" -> "product" query { matchQuery("name", name) } limit 1 }.
      await.toString
    val hits = Json.parse(result) \ "hits" \ "hits"
    val product = hits(0) \ "_source"

    HBProduct((product \ "url").as[String], (product \ "name").as[String], (product \ "id").as[String],
      (product \ "category").as[String], (product \ "subCategory").as[String])
  }

  def findComments(productId: String, keyword: String) = {
    val result = client.execute {
      search in "hepsiburada" -> "comment" query { matchQuery("productId", productId) } postFilter {
        queryFilter(queryStringQuery("*" + keyword + "*"))
      }
    }.await.toString

    val hits = (Json.parse(result) \ "hits" \ "hits").as[JsArray]
    val comments = (hits \\ "_source").toList
    println("Comments for " + keyword + ": " + comments)
    comments
  }
}
