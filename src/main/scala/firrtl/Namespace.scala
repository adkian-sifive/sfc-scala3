// SPDX-License-Identifier: Apache-2.0

package firrtl

import scala.collection.mutable
import scala.annotation.tailrec
import firrtl.ir._

class Namespace private {
  private val tempNamePrefix: String = "_GEN"
  // Begin with a tempNamePrefix in namespace so we always have a number suffix
  private val namespace = mutable.HashSet[String](tempNamePrefix)
  // Memoize where we were on a given prefix
  private val indices = mutable.HashMap[String, Int]()

  def tryName(value: String): Boolean = {
    val unused = !contains(value)
    if (unused) namespace += value
    unused
  }

  def contains(value: String): Boolean = namespace.contains(value)

  def newName(value: String): String = {
    // First try, just check
    if (tryName(value)) value
    else {
      var idx = indices.getOrElse(value, 0)
      var str = value
      while {
        !(tryName(str))
      } do {
        str = s"${value}_$idx"
        idx += 1
      }
      indices(value) = idx
      str
    }
  }

  def newTemp: String = newName(tempNamePrefix)

  /** Create a copy of the HashSet backing this [[Namespace]]
    * @return a copy of the underlying HashSet
    */
  def cloneUnderlying: mutable.HashSet[String] = namespace.clone
}

/* TODO(azidar): Make Namespace return unique names that will not conflict with expanded
 * names after LowerTypes expands names (like the Uniquify pass).
 */
object Namespace {
  // Initializes a namespace from a Module
  def apply(m: DefModule): Namespace = {
    val namespace = new Namespace

    def buildNamespaceStmt(s: Statement): Seq[String] = s match {
      // Empty names are allowed for backwards compatibility reasons and
      // indicate that the entity has essentially no name.
      case s: IsDeclaration if s.name.nonEmpty => Seq(s.name)
      case s: Conditionally                    => buildNamespaceStmt(s.conseq) ++ buildNamespaceStmt(s.alt)
      case s: Block                            => s.stmts.flatMap(buildNamespaceStmt)
      case _ => Nil
    }
    namespace.namespace ++= m.ports.map(_.name)
    m match {
      case in: Module =>
        namespace.namespace ++= buildNamespaceStmt(in.body)
      case _ => // Do nothing
    }

    namespace
  }

  /** Initializes a [[Namespace]] for [[ir.Module]] names in a [[ir.Circuit]] */
  def apply(c: Circuit): Namespace = {
    val namespace = new Namespace
    namespace.namespace ++= c.modules.map(_.name)
    namespace
  }

  /** Initializes a [[Namespace]] from arbitrary strings * */
  def apply(names: Seq[String] = Nil): Namespace = {
    val namespace = new Namespace
    namespace.namespace ++= names
    namespace
  }

  /** Appends delim to prefix until no collisions of prefix + elts in names We don't add an _ in the collision check
    * because elts could be Seq("") In this case, we're just really checking if prefix itself collides
    */
  def findValidPrefix(
    prefix:    String,
    elts:      Iterable[String],
    namespace: String => Boolean
  ): String = {
    @tailrec
    def rec(p: String): String = {
      val found = elts.exists(elt => namespace(p + elt))
      if (found) rec(p + "_") else p
    }
    rec(prefix)
  }
}
