package controllers

import java.io.File
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model._
import org.semanticweb.owlapi.reasoner.{Node}
import scala.collection.JavaConversions._
import play.api.Logger

/** Creates OWLOntology instance
  *
  *  @param    ontologyIRI Ontology IRI
  */
class OntologyController(ontologyIRI: String) {
  private val iri = IRI.create(ontologyIRI)
  private val ontologyManager = OWLManager.createOWLOntologyManager
  private val ontologyDataFactory = OWLManager.getOWLDataFactory

  val GROUP_CLASS = createOWLClass("Group")
  val MODIFIER_CLASS = createOWLClass("Modifier")
  val PROPERTY_CLASS = createOWLClass("Property")
  val PRODUCT_CLASS = createOWLClass("Product")

  private var ontology = createOntology(iri)
  initializeOntology

  /** Creates OWL ontology instance
    *
    *  @param    iri Ontology IRI
    *  @return   OWL ontology instance
    */
  private def createOntology(iri: IRI) = {
    val ontology = ontologyManager.createOntology(iri)
    Logger.info("Created ontology: " + ontology.toString)
    ontology
  }

  /** Adds default classes to ontology
    *
    */
  private def initializeOntology() = {
    val classes = List(GROUP_CLASS, MODIFIER_CLASS, PROPERTY_CLASS, PRODUCT_CLASS)
    classes foreach(addClassToOntology(_))
  }

  /** Returns current ontology instance
    *
    *  @return   OWL ontology instance
    */
  def getOntology = ontology

  def updateOntology(ontology: OWLOntology) = this.ontology = ontology

  /** Creates OWL class from given class name
    *
    *  @param    className Name of the class
    *  @return   OWL class instance
    */
  def createOWLClass(className: String) = {
    ontologyDataFactory.getOWLClass(IRI.create(iri + "#" + className))
  }

  /** Creates OWL individual from given individual name
    *
    *  @param    individualName Name of the individual
    *  @return   OWL individual instance
    */
  def createOWLIndividual(individualName: String) = {
    ontologyDataFactory.getOWLNamedIndividual(IRI.create(iri + "#" + individualName))
  }

  def findIndividual(individualName: String) = {
    val reasonerFactory = PelletReasonerFactory.getInstance
    val reasoner = reasonerFactory.createReasoner(ontology)

    def isEqual(n: Node[OWLNamedIndividual]) = {
      val instanceName = OntologyUtils.getName(n.getRepresentativeElement)
      instanceName.equals(individualName)
    }

    val result = for {
      c <- ontology.getClassesInSignature(true)
      i <- reasoner.getInstances(c, true) if isEqual(i)
    } yield (i.getRepresentativeElement)

    result.head
  }

  /** Adds given class to ontology
    *
    *  @param    newClass Class that will be added to ontology
    *  @return   List of the changes in the ontology
    */
  def addClassToOntology(newClass: OWLClass) = {
    val axiom = ontologyDataFactory.getOWLDeclarationAxiom(newClass)
    val addAxiom = new AddAxiom(ontology, axiom)
    ontologyManager.applyChange(addAxiom)
  }

  /** Adds given subclass to ontology
    *
    *  @param    subclass   Subclass that will be added
    *  @param    superclass Subclass will be added to this class
    *  @return   List of the changes in the ontology
    */
  def addSubclassToOntology(subclass: OWLClass, superclass: OWLClass) = {
    val axiom = ontologyDataFactory.getOWLSubClassOfAxiom(subclass, superclass)
    val addAxiom = new AddAxiom(ontology, axiom)
    ontologyManager.applyChange(addAxiom)
  }

  /** Adds individual to given ontology class
    *
    *  @param    ontologyClass   Individual will be added to this class
    *  @param    individual      Individual that will be added
    *  @return   List of the changes in the ontology
    */
  def addIndividualToOntology(ontologyClass: OWLClass, individual: OWLNamedIndividual) = {
    val axiom = ontologyDataFactory.getOWLClassAssertionAxiom(ontologyClass, individual)
    val addAxiom = new AddAxiom(ontology, axiom)
    ontologyManager.applyChange(addAxiom)
  }

  def setProperty(individual: OWLNamedIndividual, propertyName: String, propertyValue: String) = {
    val property = ontologyDataFactory.getOWLDataProperty(IRI.create(iri + "#" + propertyName))
    val axiom = ontologyDataFactory.getOWLDataPropertyAssertionAxiom(property, individual, propertyValue)
    val addAxiom = new AddAxiom(ontology, axiom)
    ontologyManager.applyChange(addAxiom)
  }

  def addObjectProperty(mSubject: OWLNamedIndividual, mPredicate: String, mObject: OWLNamedIndividual) = {
    val property = ontologyDataFactory.getOWLObjectProperty(IRI.create(iri + "#" + mPredicate))
    val axiom = ontologyDataFactory.getOWLObjectPropertyAssertionAxiom(property, mSubject, mObject)
    val addAxiom = new AddAxiom(ontology, axiom)
    ontologyManager.applyChange(addAxiom)
  }

  /** Saves OWL ontology to file with OWL format
    *
    *  @param    fileName File name
    */
  def saveOntologyToFile(fileName: String) = {
    val file = new File(fileName + ".owl")
    ontologyManager.saveOntology(ontology, IRI.create(file.toURI))
  }
}

object OntologyUtils {
  /** Extracts entity name from OWLEntity
    *
    *  @param    e OWL entity
    *  @return   Entity's name
    */
  def getName(e: OWLEntity) = {
    val name = e.toString
    if (name.contains("#")) {
      name.substring(name.indexOf("#") + 1, name.lastIndexOf(">"))
    } else {
      name
    }
  }

  /** Extracts entity name from given string
    *
    *  @param    s String
    *  @return   Entity's name
    */
  def getName(s: String) = {
    if (s.contains("#")) {
      s.substring(s.indexOf("#") + 1, s.lastIndexOf(">"))
    } else {
      s
    }
  }
}
