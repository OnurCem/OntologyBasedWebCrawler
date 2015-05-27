package controllers

import java.util.Date
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory
import models._
import org.anormcypher._
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary
import play.api.libs.json.{Json, JsValue}
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class Neo4jController {
  implicit val connection = Neo4jREST()

  def addCommentNode(propertyId: String, commentId: String, comment: String) = {
    Cypher("MERGE (a:Comment { id: '" + commentId + "', content: '" +
      comment + "', type:'comment' }) ").execute()
    Cypher("MATCH (a:Comment),(b:Property)" +
      "WHERE a.id = '" + commentId + "' AND b.id = '" + propertyId + "'" +
      "CREATE UNIQUE (b)-[r:has]->(a)" +
      "SET r.type = 'has'").execute()
  }

  def getAllRelationshipsExceptComments = {
    Cypher("MATCH (n)-[r]->(m) WHERE NOT (n.type = 'comment' AND m.type = 'comment') RETURN r").apply().map {
      row  => row[Option[NeoRelationship]]("r")
    }.toList
  }

  def findNodeById(id: Long) = {
    Cypher("MATCH (n)" +
      "WHERE id(n) = " + id + " " +
      "RETURN n.id as id").apply().map {
      row  => row[Option[String]]("id")
    }.get(0)
  }

  def findNodeById(id: String) = {
    Cypher("MATCH (n)" +
      "WHERE n.id = '" + id + "' " +
      "RETURN n as node").apply().map {
      row  => row[Option[NeoNode]]("node")
    }.get(0)
  }

  def findProducts(keywords: String) = {
    val products = Cypher("START n=node:node_auto_index('model:(" + keywords + ") OR brand:(" + keywords + ")')" +
      "MATCH (n:Product)-[r1]-(p:Property), (n:Product)-[r2]-(g:Group)" +
      "RETURN n.id as id, n.brand as brand, n.model as model, g.name as group, collect(p.name) as properties")
      .apply().map {
        row  => SearchResult(row[String]("id"), row[String]("brand"), row[String]("model"),
          row[String]("group"), row[Seq[String]]("properties"))
      }.toList

    products
  }

  def getComments(productId: String) = {
    val products = Cypher("MATCH (n:Product)-[r1]-(p:Property)-[r2]-(c:Comment) " +
      "WHERE n.id = '" + productId + "' " +
      "RETURN p.name as property, collect(c.content) as comments")
      .apply().map {
      row  => (row[String]("property"), row[Seq[String]]("comments"))
    }.toList

    products.map(i => ProductComment(i._1.split(",")(1).trim, i._2.toList.map(Json.parse(_))))
  }

  def getGroupNodes = {
    Cypher("MATCH (i:Group) RETURN i.id as id, i.name as name").apply().map(
      row => Group(row[String]("id"), row[String]("name"))
    ).toList
  }

  def getProductNodes = {
    val products: List[(String, String, String, String, Option[String])] = Cypher("MATCH (i:Product)--(g:Group) " +
           "OPTIONAL MATCH (i:Product)--(p:Property) " +
           "RETURN i.id as id, i.brand as brand, i.model as model, g.id as groupId, p.id as property").apply().map(
      row => (row[String]("id"), row[String]("brand"), row[String]("model"),
        row[String]("groupId"), row[Option[String]]("property"))
    ).toList

    products.foldLeft(List[(String, String, String, String, ListBuffer[String])]()) { (a, b) =>
      if (!a.isEmpty && b._1.equals(a.head._1)) {
        b._5 match {
          case Some(x) => a.head._5 += x
        }
        a
      } else {
        b._5 match {
          case Some(x) => (b._1, b._2, b._3, b._4, ListBuffer(x)) :: a
          case None    => (b._1, b._2, b._3, b._4, ListBuffer[String]()) :: a
        }
      }
    }.map(i => Product(i._1, i._2, i._3, i._4, i._5.toList))
  }

  def getPropertyNodes = {
    Cypher("MATCH (i:Property) RETURN i.id as id, i.name as name").apply().map(
      row => Property(row[String]("id"), row[String]("name"))
    ).toList
  }

  def getProductProperties = {
    Cypher("MATCH (i:Product)--(p:Property) " +
           "RETURN i.id as productId, p.id as propertyId").apply().map(
        row => ProductProperty(row[String]("productId"), row[String]("propertyId"))
    ).toList
  }

  def deleteNode(nodeName: String) = {
    Cypher("MATCH (n {name: '" + nodeName + "'})-[r]-() DELETE n, r").execute()
  }

  def updateNodeName(oldName: String, newName: String) = {
    Cypher("MATCH (n {name: '" + oldName + "'}) SET n.name = '" + newName + "'").execute()
  }

  /** Saves OWL ontology to Neo4j database
    *
    */
  def saveOntologyToNeo4j(ontology: OWLOntology) = {
    val reasonerFactory = PelletReasonerFactory.getInstance
    val reasoner = reasonerFactory.createReasoner(ontology)
    val classes = ontology.getClassesInSignature(true)
    val OWLThing = OWLRDFVocabulary.OWL_THING.getShortName

    for (c <- classes) {
      val className = OntologyUtils.getName(c)

      Cypher("MERGE (:Class {name:'" + className + "', type:'class'})").execute()

      val superClasses = reasoner.getSuperClasses(c, true)

      if (superClasses.isEmpty) {
        Cypher("MATCH (a:Class),(b:Class)" +
          "WHERE a.name = '" + className + "' AND b.name = '" + OWLThing + "'" +
          "CREATE UNIQUE (a)-[r:isA]->(b)" +
          "SET r.type = 'isA'").execute()
      } else {
        for (parentOWLNode <- superClasses) {
          val parent = parentOWLNode.getRepresentativeElement
          val parentName = OntologyUtils.getName(parent)

          Cypher("MERGE (:Class {name:'" + parentName + "', type:'class'})").execute()
          Cypher("MATCH (a:Class),(b:Class)" +
            "WHERE a.name = '" + className + "' AND b.name = '" + parentName + "'" +
            "CREATE UNIQUE (a)-[r:isA]->(b)" +
            "SET r.type = 'isA'").execute()
        }
      }

      for (i <- reasoner.getInstances(c, true)) {
        val instance = i.getRepresentativeElement
        val instanceName = OntologyUtils.getName(instance)

        val individualType =
          if (instanceName.startsWith("group"))
            "Group"
          else if (instanceName.startsWith("product"))
            "Product"
          else if (instanceName.startsWith("property"))
            "Property"
          else
            "Individual"

        Cypher("MERGE (:" + individualType + "{id:'" + instanceName + "'})").execute()
        Cypher("MATCH (a:" + individualType + "),(b:Class)" +
          "WHERE a.id = '" + instanceName + "' AND b.name = '" + className + "'" +
          "CREATE UNIQUE (a)-[r:isA]->(b)" +
          "SET r.type = 'isA'").execute()

        for (objectProperty <- ontology.getObjectPropertiesInSignature()) {
          for (propertyValue <- reasoner.getObjectPropertyValues(instance, objectProperty)) {
            var relationType = objectProperty.toString
            relationType = OntologyUtils.getName(relationType)
            var propertyName = propertyValue.getRepresentativeElement.toString
            propertyName = OntologyUtils.getName(propertyName)

            //Cypher("MERGE (:ObjectProperty {id:'" + propertyName + "})").execute()
            Cypher("MATCH (a:" + individualType + "),(b)" +
              "WHERE a.id = '" + instanceName + "' AND b.id = '" + propertyName + "'" +
              "CREATE UNIQUE (a)-[r:" + relationType + "]->(b)" +
              "SET r.type = '" + relationType + "'").execute()
          }
        }

        for (dataProperty <- ontology.getDataPropertiesInSignature) {
          for (propertyValue <- reasoner.getDataPropertyValues(instance, dataProperty.asOWLDataProperty)) {
            var relationType = dataProperty.asOWLDataProperty.toString
            relationType = OntologyUtils.getName(relationType)
            val propertyName = propertyValue.getLiteral

            Cypher("MATCH (a:" + individualType + ")" +
              "WHERE a.id = '" + instanceName + "'" +
              "SET a." + relationType + " = '" + propertyName + "'").execute()
          }
        }

        for (dataProperty <- ontology.getAnnotationPropertiesInSignature) {
          for (propertyValue <- reasoner.getAnnotationPropertyValues(instance, dataProperty)) {
            var relationType = dataProperty.toString
            relationType = OntologyUtils.getName(relationType)
            val propertyName = propertyValue.toString

            Cypher("MATCH (a:" + individualType + ")" +
              "WHERE a.id = '" + instanceName +
              "SET a." + relationType + " = '" + propertyName + "'").execute()
          }
        }
      }
    }
  }

  def loadOntologyFromNeo4j = {
    val ontologyCtrl = new OntologyController("OntMngApp")
    val ontology = ontologyCtrl.getOntology

    getProductNodes map { p =>
      val individual = ontologyCtrl.createOWLIndividual(p.id)
      ontologyCtrl.addIndividualToOntology(ontologyCtrl.PRODUCT_CLASS, individual)
      ontologyCtrl.setProperty(individual, "brand", p.brand)
      ontologyCtrl.setProperty(individual, "model", p.model)
    }

    getPropertyNodes map { p =>
      val individual = ontologyCtrl.createOWLIndividual(p.id)
      ontologyCtrl.addIndividualToOntology(ontologyCtrl.PROPERTY_CLASS, individual)
      ontologyCtrl.setProperty(individual, "name", p.name)
    }

    getGroupNodes map { g =>
      val individual = ontologyCtrl.createOWLIndividual(g.id)
      ontologyCtrl.addIndividualToOntology(ontologyCtrl.GROUP_CLASS, individual)
      ontologyCtrl.setProperty(individual, "name", g.name)
    }

    getAllRelationshipsExceptComments.map {
      r => r match {
        case Some(x) => {
          val mSubject = findNodeById(x.start).getOrElse("")
          val mObject = findNodeById(x.end).getOrElse("")
          val mPredicate = x.props.get("type").get.toString

          if (!mSubject.isEmpty && !mObject.isEmpty) {
            ontologyCtrl.addObjectProperty(
              ontologyCtrl.findIndividual(mSubject),
              mPredicate,
              ontologyCtrl.findIndividual(mObject))
          }
        }
      }
    }

    ontology
  }
}
