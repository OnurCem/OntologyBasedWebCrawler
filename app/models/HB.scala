package models

import com.sksamuel.elastic4s.source.{Indexable, DocumentMap}
import org.jsoup.nodes.{Element, Document}
import play.api.libs.json._
import scala.collection.JavaConversions._

case class HBObject(url: String, name: String, id: String, var category: String = "",
                    var subCategory: String = "", var comments: List[HBComment] = Nil) extends DocumentMap {
  def map = Map("url" -> url,
                "name" -> name,
                "category" -> category,
                "subCategory" -> subCategory,
                "id" -> id,
                "comments" -> comments)
}

case class HBProduct(url: String, name: String, id: String, var category: String = "",
                    var subCategory: String = "") extends DocumentMap {
  def map = Map("url" -> url,
                "name" -> name,
                "category" -> category,
                "subCategory" -> subCategory,
                "id" -> id)
}

case class HBComment(id: String, productId: String, writer: String, title: String, date: String, rating: Int, content: String) extends DocumentMap {
  def map = Map("id" -> id,
                "productId" -> productId,
                "writer" -> writer,
                "title" -> title,
                "date" -> date,
                "rating" -> rating,
                "content" -> content)
}

class HB {
  val name = "hepsiburada.com"
  val hostURL = "http://www.hepsiburada.com"
  val searchURL = "http://www.hepsiburada.com/liste/search.aspx?sText="
  val commentURL = "http://www.hepsiburada.com/liste/ReviewContent.aspx?productId="
  val searchPageCountQuery = "span[class=fleft ml5] a"
  val commentPageCountQuery = "span[class=fleft ml5] a"
  val productQuery = "div[class=productDetails]"
  val productURLQuery = "a[title]"
  val productRatingQuery = "img[id*=imgStar]"
  val productCategoryQuery = "ul[class=m0] li"
  val commentQuery = "div[class=comment_rpt]"
  val commentWriterQuery = "span[id*=FullTitle]"
  val commentTitleQuery = "span[id*=lblHeader]"
  val commentDateQuery = "span[id*=date]"
  val commentRatingQuery = "img[id*=imgTopGood]"
  val commentContentQuery = "div[class=comment_txt mt10]"

  def getSearchURL(keyword: String) = searchURL + keyword + "&pn="

  def getCommentURL(id: String) = commentURL + id + "&sortBy=lastAllTime&c=&IsLoad=true&pn="

  def getSearchPageCount(document: Document) = {
    try {
      document.select(searchPageCountQuery).last() match {
        case null => 0
        case x    => x.attr("pagerindex").toInt + 1
      }
    } catch {
      case e: Exception => e.printStackTrace; 0
    }
  }

  def getCommentPageCount(document: Document) = {
    try {
      document.select(commentPageCountQuery).last() match {
        case null => 0
        case x    => x.attr("pagerindex").toInt
      }
    } catch {
      case e: Exception => e.printStackTrace; 0
    }
  }

  def getProductID(url: String) = {
    val regex = """.*productId=([\w\d\W\D]+)&.*""".r
    url match {
      case regex(id) => Some(id)
      case _         => None
    }
  }

  def getProduct(e: Element) = {
    try {
      val url = e.select(productURLQuery).first
      val urlString = hostURL + url.attr("href")
      val id = getProductID(urlString).getOrElse("")
      val name = url.attr("title")

      Some(HBObject(urlString, name, id))
    } catch {
      case e: Exception => e.printStackTrace; None
    }
  }

  def getCategory(document: Document) = {
      try {
        document.select(productCategoryQuery).get(1).text
      } catch {
        case e: Exception => e.printStackTrace; ""
      }
  }

  def getSubCategory(document: Document) = {
    try {
      document.select(productCategoryQuery).get(3).text
    } catch {
      case e: Exception => e.printStackTrace; ""
    }
  }

  def getComments(productId: String, document: Document) = {
    try {
      document.select(commentQuery) match {
        case null => None
        case x    => {
          val comments = x.map { e =>
            val id = e.attr("id")
            val writer = e.select(commentWriterQuery).first
            val writerString =
              if (writer.hasAttr("class"))
                writer.text
              else ""
            val title = e.select(commentTitleQuery).first.text
            val date = e.select(commentDateQuery).first.text
            val rating = getRating(e, commentRatingQuery)
            val content = e.select(commentContentQuery).first.text

            HBComment(id, productId, writerString, title, date, rating, content)
          }.toList

          Some(comments)
        }
      }
    } catch {
      case e: Exception => e.printStackTrace; None
    }
  }

  def getRating(element: Element, query: String) = {
    val oneStar = """.*(10).gif""".r
    val twoStar = """.*(20).gif""".r
    val threeStar = """.*(30).gif""".r
    val fourStar = """.*(40).gif""".r
    val fiveStar = """.*(50).gif""".r

    element.select(query).attr("src") match {
      case oneStar(one) => one.toInt / 10
      case twoStar(two) => two.toInt / 10
      case threeStar(three) => three.toInt / 10
      case fourStar(four) => four.toInt / 10
      case fiveStar(five) => five.toInt / 10
      case _ => 0
    }
  }
}

object HBObject {
  implicit val HBCommentWrites = new Writes[HBComment] {
    def writes(hbComment: HBComment) = Json.obj(
      "writer" -> hbComment.writer,
      "title" -> hbComment.title,
      "date" -> hbComment.date,
      "rating" -> hbComment.rating,
      "content" -> hbComment.content)
  }

  implicit val HBWrites = new Writes[HBObject] {
    def writes(hb: HBObject) = Json.obj(
      "url" -> hb.url,
      "name" -> hb.name,
      "category" -> hb.category,
      "subCategory" -> hb.subCategory,
      "id" -> hb.id,
      "comments"  -> hb.comments)
  }

  implicit val HBProductWrites = new Writes[HBProduct] {
    def writes(hb: HBProduct) = Json.obj(
      "url" -> hb.url,
      "name" -> hb.name,
      "category" -> hb.category,
      "subCategory" -> hb.subCategory,
      "id" -> hb.id)
  }
}
