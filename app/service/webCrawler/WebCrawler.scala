package service.webCrawler

import java.io.{PrintWriter, File}
import controllers.ElasticSearchController
import models.{HB, HBComment}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future}
import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Created by Onur Cem on 1/8/2015.
 */
class WebCrawler {
  val logger = new PrintWriter(new File("crawler_log.txt"))
  val elastic = new ElasticSearchController

  //create Website instance
  val hb = new HB

  def crawl = {
    val departmentLinks = getDepartmentLinks(hb.hostURL)
    departmentLinks foreach { i => println(i) }

    val subDepartmentLinks = departmentLinks.map(link =>
      getSubDepartmentLinks(link)
    ).flatten
    subDepartmentLinks foreach { i => println(i) }

    subDepartmentLinks.par.map(link =>
      saveProducts(link)
    )

    logger.close
  }

  def getDepartmentLinks(url: String) = {
    val document = connect(url)
    var result = new ListBuffer[String]()
    val links = document.select("ul[class=catA emenu-root mb12] a")

    links.map { e =>
      if (e.attr("href").contains("department")) {
        result += e.attr("href")
      }
    }

    result.toList.take(result.size - 1)
  }

  def getSubDepartmentLinks(url: String) = {
    val document = connect(url)
    var result = new ListBuffer[String]()
    val links = document.select("div[class=hierrarchyMenu w192] a")

    links.map { e =>
      if (e.attr("href").contains("department")) {
        result += hb.hostURL + e.attr("href") + "&pn="
      }
    }

    result.toList
  }

  def saveProducts(url: String) = {
    try {
      println("Crawling department link: " + url)
      logger.append("Crawling department link: " + url + System.lineSeparator)
      var document = connect(url + "1")
      var products = document.select(hb.productQuery)
      val pageCount = hb.getSearchPageCount(document)
      val category = hb.getCategory(document)
      val subCategory = hb.getSubCategory(document)

      if (products != null) {
        products.par.map { p =>
          val product = hb.getProduct(p)
          product match {
            case Some(p) => {
              if (!p.id.isEmpty) {
                p.comments = getProductComments(p.id)
                if (p.comments != Nil) {
                  println("Saving product: " + p.name)
                  logger.append("Saving product: " + p.name + System.lineSeparator)
                  p.category = category
                  p.subCategory = subCategory
                  elastic.save(p)
                } else {
                  println("ERROR: comments is null > " + p.name)
                  logger.append("ERROR: comments is null > " + p.name + System.lineSeparator)
                }
              } else {
                println("ERROR: product id is empty > " + p.name)
                logger.append("ERROR: product id is empty > " + p.name + System.lineSeparator)
              }
            }
            case None => println("ERROR: product not found > " + p.text); logger.append("ERROR: product not found > " + p.text + System.lineSeparator)
          }
        }
      } else {
        println("ERROR: products is null > " + url + "1"); logger.append("ERROR: products is null > " + url + "1" + System.lineSeparator)
      }

      for (i <- 2 to pageCount) {
        document = connect(url + i)
        products = document.select(hb.productQuery)

        if (products != null) {
          products.par.map { p =>
            val product = hb.getProduct(p)
            product match {
              case Some(p) => {
                if (!p.id.isEmpty) {
                  p.comments = getProductComments(p.id)
                  if (p.comments != Nil) {
                    println("Saving product: " + p.name)
                    logger.append("Saving product: " + p.name + System.lineSeparator)
                    p.category = category
                    p.subCategory = subCategory
                    elastic.save(p)
                  } else {
                    println("ERROR: comments is null > " + p.name)
                    logger.append("ERROR: comments is null > " + p.name + System.lineSeparator)
                  }
                } else {
                  println("ERROR: product id is empty > " + p.name)
                  logger.append("ERROR: product id is empty > " + p.name + System.lineSeparator)
                }
              }
              case None => println("ERROR: product not found > " + p.text); logger.append("ERROR: product not found > " + p.text + System.lineSeparator)
            }
          }
        } else {
          println("ERROR: products is null > " + url + i); logger.append("ERROR: products is null > " + url + i + System.lineSeparator)
        }
      }
    } catch {
      case e: Exception => e.printStackTrace
    }
  }

  def getProductComments(id: String) = {
    try {
      val commentsURL = hb.getCommentURL(id)
      val document = connect(commentsURL + 0)
      val pageCount = hb.getCommentPageCount(document)

      @tailrec
      def loop(n: Int, xs: List[List[HBComment]]): List[List[HBComment]] = {
        if (n == -1)
          xs
        else {
          val x = hb.getComments(id, connect(commentsURL + n)).getOrElse(
            List(HBComment("", "", "", "", "", 0, "")))

          loop(n - 1, x :: xs)
        }
      }

      loop(pageCount, Nil).flatten
    } catch {
      case e: Exception => e.printStackTrace; Nil
    }
  }

  def connect(url: String) = {
    val page = Source.fromURL(url, "UTF-8").getLines.mkString
    Jsoup.parse(page)
  }
}
