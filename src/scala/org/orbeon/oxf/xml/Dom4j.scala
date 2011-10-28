/**
 *  Copyright (C) 2007 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xml

import dom4j.Dom4jUtils._
import org.dom4j._
import java.util.{List ⇒ JList, Map ⇒ JMap}
import org.apache.commons.lang.StringUtils._
import collection.JavaConverters._
import org.orbeon.saxon.value.Whitespace

object Dom4j {

    /**
     * Compare two dom4j documents.
     *
     * This comparison:
     *
     * - doesn't modify the input documents
     * - normalizes adjacent text/CDATA nodes into single text nodes
     * - ignores resulting blank text nodes
     * - trims non-blank text nodes before comparing them
     * - ignores namespace nodes (not because they don't matter, but they are hard to handle)
     * - ignores entity nodes (we shouldn't have any and we don't know how to compare them with dom4j anyway)
     * - compares qualified names by checking namespace URIs and local names, but ignores prefixes
     * - ignores the relative order of an element's attributes
     *
     * In-scope namespaces are hard to compare when dealing with documents that were embedded. The embedded document
     * might declare many namespaces on its root element because of embedding. However the other document might have a
     * slightly different namespace layout. It is not clear, for the purpose of unit tests, whether we can realistically
     * compare namespace nodes in a much better way, without some kind of schema information.
     */
    def compareDocumentsIgnoreNamespacesInScope(left: Document, right: Document) = {
        val normalizeText = trimToEmpty _
        compareTwoNodes(normalizeTextNodes(createCopy(left.getRootElement)), normalizeTextNodes(createCopy(right.getRootElement)))(normalizeText)
    }

    /**
     * Same as compareDocumentsIgnoreNamespacesInScope but collapse white space when comparing text.
     */
    def compareDocumentsIgnoreNamespacesInScopeCollapse(left: Document, right: Document) =
        compareElementsIgnoreNamespacesInScopeCollapse(left.getRootElement, right.getRootElement)

    def compareElementsIgnoreNamespacesInScopeCollapse(left: Element, right: Element) = {
        val normalizeText = (c: String) ⇒ Whitespace.collapseWhitespace(c).toString
        compareTwoNodes(normalizeTextNodes(createCopy(left)), normalizeTextNodes(createCopy(right)))(normalizeText)
    }

    // Only keep the nodes we care about
    private def filterOut(l: Seq[Node]) = l collect {
        case n @ (_: Document | _: Element | _: Attribute | _: Comment | _: ProcessingInstruction) ⇒ n
        case t: CharacterData if isNotBlank(t.getText) ⇒ t
    }

    private def compareTwoNodeSeqs(left: Seq[Node], right: Seq[Node])(implicit normalizeText: String ⇒ String) =
        left.size == right.size && (left.zip(right) forall
            { case (n1, n2) ⇒ compareTwoNodes(n1, n2) })

    private implicit def dom4jListToNodeSeq(l: JList[_]): Seq[Node] = l.asInstanceOf[JList[Node]].asScala

    // An ordering for attributes, which takes into account the namespace URI and the local name
    private implicit object AttributeOrdering extends Ordering[Attribute] {
        def compare(x: Attribute, y: Attribute) =
            XMLUtils.buildExplodedQName(x.getQName) compare XMLUtils.buildExplodedQName(y.getQName)
    }

    private def compareTwoNodes(left: Node, right: Node)(implicit normalizeText: String ⇒ String): Boolean =
        (left, right) match {
            case (d1: Document, d2: Document) ⇒
                compareTwoNodeSeqs(filterOut(d1.content), filterOut(d2.content))
            case (e1: Element, e2: Element) ⇒
                e1.getQName == e2.getQName &&
                    compareTwoNodeSeqs(e1.attributes.asInstanceOf[JList[Attribute]].asScala.sorted, e2.attributes.asInstanceOf[JList[Attribute]].asScala.sorted) && // sort attributes
                    compareTwoNodeSeqs(filterOut(e1.content), filterOut(e2.content))
            case (a1: Attribute, a2: Attribute) ⇒
                a1.getQName == a2.getQName &&
                    a1.getValue == a2.getValue
            case (c1: Comment, c2: Comment) ⇒
                c1.getText == c2.getText
            case (t1: CharacterData, t2: CharacterData) ⇒ // Text, CDATA, or Comment (Comment is checked above - dom4j class hierarchy is bad)
                normalizeText(t1.getText) == normalizeText(t2.getText)
            case (p1: ProcessingInstruction, p2: ProcessingInstruction) ⇒
                p1.getTarget == p2.getTarget &&
                    p1.getValues.asInstanceOf[JMap[String, String]].asScala == p2.getValues.asInstanceOf[JMap[String, String]].asScala
            case _ ⇒
                false
        }
}