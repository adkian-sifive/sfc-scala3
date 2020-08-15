// See LICENSE for license details.
// Author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package firrtl.backends.experimental.smt

import java.io.Writer

import firrtl._
import firrtl.annotations.{Annotation, NoTargetAnnotation}
import firrtl.options.Viewer.view
import firrtl.options.{CustomFileEmission, Dependency}
import firrtl.stage.FirrtlOptions

private[firrtl] abstract class SMTEmitter private[firrtl] ()
    extends Transform
    with Emitter
    with DependencyAPIMigration {
  override def prerequisites: Seq[Dependency[Transform]] = Seq(Dependency(FirrtlToTransitionSystem))
  override def invalidates(a: Transform): Boolean = false

  override def emit(state: CircuitState, writer: Writer): Unit = error("Deprecated since firrtl 1.0!")

  protected def serialize(sys: TransitionSystem): Annotation

  private val BleedingEdgeWarning =
    """WARNING: The SMT and BTOR2 emitters are experimental preview features.
      |- they might be removed in future versions without deprecation warning
      |- their behavior and interfaces might change without notice
      |- they are unsupported, we won't be able to fix any issues you find in them
      |- however, we do accept pull requests: https://github.com/freechipsproject/firrtl/pulls
      |""".stripMargin

  override protected def execute(state: CircuitState): CircuitState = {
    val emitCircuit = state.annotations.exists {
      case EmitCircuitAnnotation(a) if this.getClass == a    => true
      case EmitAllModulesAnnotation(a) if this.getClass == a => error("EmitAllModulesAnnotation not supported!")
      case _                                                 => false
    }

    if (!emitCircuit) { return state }

    logger.warn(BleedingEdgeWarning)

    val sys = state.annotations.collectFirst { case TransitionSystemAnnotation(sys) => sys }.getOrElse {
      error("Could not find the transition system!")
    }
    state.copy(annotations = state.annotations :+ serialize(sys))
  }

  protected def generatedHeader(format: String, name: String): String =
    s"; $format description generated by firrtl ${BuildInfo.version} for module $name.\n"

  protected def error(msg: String): Nothing = throw new RuntimeException(msg)
}

case class EmittedSMTModelAnnotation(name: String, src: String, outputSuffix: String)
    extends NoTargetAnnotation
    with CustomFileEmission {
  override protected def baseFileName(annotations: AnnotationSeq): String =
    view[FirrtlOptions](annotations).outputFileName.getOrElse(name)
  override protected def suffix: Option[String] = Some(outputSuffix)
  override def getBytes:         Iterable[Byte] = src.getBytes
}

private[firrtl] class Btor2Emitter extends SMTEmitter {
  override def outputSuffix: String = ".btor2"
  override protected def serialize(sys: TransitionSystem): Annotation = {
    val btor = generatedHeader("BTOR", sys.name) + Btor2Serializer.serialize(sys).mkString("\n") + "\n"
    EmittedSMTModelAnnotation(sys.name, btor, outputSuffix)
  }
}

private[firrtl] class SMTLibEmitter extends SMTEmitter {
  override def outputSuffix: String = ".smt2"
  override protected def serialize(sys: TransitionSystem): Annotation = {
    val hasMemory = sys.states.exists(_.sym.isInstanceOf[ArrayExpr])
    val logic = SMTLibSerializer.setLogic(hasMemory) + "\n"
    val header = if (hasMemory) {
      "; We have to disable the logic for z3 to accept the non-standard \"as const\"\n" +
        "; see https://github.com/Z3Prover/z3/issues/1803\n" +
        "; for CVC4 you probably want to include the logic\n" +
        ";" + logic
    } else { logic }
    val smt = generatedHeader("SMT-LIBv2", sys.name) + header +
      SMTTransitionSystemEncoder.encode(sys).map(SMTLibSerializer.serialize).mkString("\n") + "\n"
    EmittedSMTModelAnnotation(sys.name, smt, outputSuffix)
  }
}
