package controllers

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import service.webCrawler.WebCrawler

object Application extends Controller {
  val neo4jCtrl = new Neo4jController
  val ontologyCtrl = new OntologyController("OntMngApp")
  val elasticCtrl = new ElasticSearchController

  def crawl = Action {
    val webCrawler = new WebCrawler
    webCrawler.crawl

    Ok("Done!")
  }

  def index = Action {
    val ontology = neo4jCtrl.loadOntologyFromNeo4j
    ontologyCtrl.updateOntology(ontology)
    ontologyCtrl.saveOntologyToFile("test")

    Ok(views.html.main())
  }

  def search(q: String) = Action {
    val products = neo4jCtrl.findProducts(q)
    println("Search result: " + Json.toJson(products))
    Ok(Json.toJson(products))
  }

  def getComments(q: String) = Action {
    val comments = neo4jCtrl.getComments(q)
    println("Comments: " + Json.toJson(comments))
    Ok(Json.toJson(comments))
  }

  def saveGroup = Action { request =>
    val json = request.body.asJson.get
    val id = (json \ "id").as[String]
    val name = (json \ "name").as[String]
    Logger.info("Group ID: " + id)
    Logger.info("Group name: " + name)

    val groupIndividual = ontologyCtrl.createOWLIndividual(id)

    ontologyCtrl.addIndividualToOntology(ontologyCtrl.GROUP_CLASS, groupIndividual)
    ontologyCtrl.setProperty(groupIndividual, "name", name)

    neo4jCtrl.saveOntologyToNeo4j(ontologyCtrl.getOntology)

    Ok(json)
  }

  def saveProduct = Action { request =>
    val json = request.body.asJson.get
    val id = (json \ "id").as[String]
    val brand = (json \ "brand").as[String]
    val model = (json \ "model").as[String]
    val groupId = (json \ "groupId").as[String]
    Logger.info("Product ID: " + id)
    Logger.info("Product brand: " + brand)
    Logger.info("Product model: " + model)
    Logger.info("Product group ID: " + groupId)

    val productIndividual = ontologyCtrl.createOWLIndividual(id)
    val groupIndividual = ontologyCtrl.findIndividual(groupId)

    ontologyCtrl.addIndividualToOntology(ontologyCtrl.PRODUCT_CLASS, productIndividual)
    ontologyCtrl.setProperty(productIndividual, "brand", brand)
    ontologyCtrl.setProperty(productIndividual, "model", model)
    ontologyCtrl.addObjectProperty(productIndividual, "has", groupIndividual)

    neo4jCtrl.saveOntologyToNeo4j(ontologyCtrl.getOntology)

    Ok(json)
  }

  def saveProperty = Action { request =>
    val json = request.body.asJson.get
    val id = (json \ "id").as[String]
    val name = (json \ "name").as[String]
    Logger.info("Property ID: " + id)
    Logger.info("Property name: " + name)

    val propertyIndividual = ontologyCtrl.createOWLIndividual(id)

    ontologyCtrl.addIndividualToOntology(ontologyCtrl.PROPERTY_CLASS, propertyIndividual)
    ontologyCtrl.setProperty(propertyIndividual, "name", name)

    neo4jCtrl.saveOntologyToNeo4j(ontologyCtrl.getOntology)

    Ok(json)
  }

  def saveProductProperty = Action { request =>
    val json = request.body.asJson.get
    val productId = (json \ "productId").as[String]
    val propertyId = (json \ "propertyId").as[String]
    Logger.info("Product ID: " + productId)
    Logger.info("Property ID: " + propertyId)

    val productIndividual = ontologyCtrl.findIndividual(productId)
    val propertyIndividual = ontologyCtrl.findIndividual(propertyId)
    val product = neo4jCtrl.findNodeById(productId)
    val property = neo4jCtrl.findNodeById(propertyId)

    product match {
      case Some(prod) => {
        property match {
          case Some(prop) => {
            val model = prod.props.get("model").getOrElse("null").toString
            val brand = prod.props.get("brand").getOrElse("null").toString
            val propertyName = prop.props.get("name").getOrElse("null").toString.split(",")(1).trim

            println("Model, Brand, Property: " + model + ", " + brand + ", " + propertyName)

            val product = elasticCtrl.findProduct(brand + " " + model)
            val comments = elasticCtrl.findComments(product.id, propertyName)
            comments map {
              c => neo4jCtrl.addCommentNode(propertyId, (c \ "id").toString, c.toString)
            }
          }
        }
      }
    }

    ontologyCtrl.addObjectProperty(productIndividual, "has", propertyIndividual)
    neo4jCtrl.saveOntologyToNeo4j(ontologyCtrl.getOntology)

    Ok(json)
  }

  def removeGroup = Action { request =>
    val json = request.body.asJson.get
    val groupName = (json \ "name").as[String]
    Logger.info("Deleted group name: " + groupName)
    neo4jCtrl.deleteNode(groupName)

    Ok(json)
  }

  def getGroups = Action {
    val groups = neo4jCtrl.getGroupNodes
    Logger.info("Groups: " + groups)

    Ok(Json.toJson(groups))
  }

  def getProducts = Action {
    val products = neo4jCtrl.getProductNodes
    Logger.info("Products: " + products)

    Ok(Json.toJson(products))
  }

  def getProperties = Action {
    val properties = neo4jCtrl.getPropertyNodes
    Logger.info("Properties: " + properties)

    Ok(Json.toJson(properties))
  }

  def getProductProperties = Action {
    val productProperties = neo4jCtrl.getProductProperties
    Logger.info("Product Properties: " + productProperties)

    Ok(Json.toJson(productProperties))
  }
}