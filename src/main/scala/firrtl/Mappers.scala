// SPDX-License-Identifier: Apache-2.0

package firrtl

import firrtl.ir._

// TODO: Implement remaining mappers and recursive mappers
object Mappers {
  // ********** Port Mappers **********
  trait PortMagnet {
    def map(p: Port): Port
  }
  object PortMagnet {
    implicit def forType(f: Type => Type): PortMagnet = new PortMagnet {
      override def map(port: Port): Port = port.mapType(f)
    }
    implicit def forString(f: String => String): PortMagnet = new PortMagnet {
      override def map(port: Port): Port = port.mapString(f)
    }
  }
  implicit class PortMap(val _port: Port) extends AnyVal {
    def map[T](f: T => T)(implicit magnet: (T => T) => PortMagnet): Port = magnet(f).map(_port)
  }

  // ********** Stmt Mappers **********
  trait StmtMagnet {
    def map(stmt: Statement): Statement
  }
  object StmtMagnet {
    implicit def forStmt(f: Statement => Statement): StmtMagnet = new StmtMagnet {
      override def map(stmt: Statement): Statement = stmt.mapStmt(f)
    }
    implicit def forExp(f: Expression => Expression): StmtMagnet = new StmtMagnet {
      override def map(stmt: Statement): Statement = stmt.mapExpr(f)
    }
    implicit def forType(f: Type => Type): StmtMagnet = new StmtMagnet {
      override def map(stmt: Statement): Statement = stmt.mapType(f)
    }
    implicit def forString(f: String => String): StmtMagnet = new StmtMagnet {
      override def map(stmt: Statement): Statement = stmt.mapString(f)
    }
    implicit def forInfo(f: Info => Info): StmtMagnet = new StmtMagnet {
      override def map(stmt: Statement): Statement = stmt.mapInfo(f)
    }
  }
  implicit class StmtMap(val _stmt: Statement) extends AnyVal {
    // Using implicit types to allow overloading of function type to map, see StmtMagnet above
    def map[T](f: T => T)(implicit magnet: (T => T) => StmtMagnet): Statement = magnet(f).map(_stmt)
  }

  // ********** Expression Mappers **********
  trait ExprMagnet {
    def map(expr: Expression): Expression
  }
  object ExprMagnet {
    implicit def forExpr(f: Expression => Expression): ExprMagnet = new ExprMagnet {
      override def map(expr: Expression): Expression = expr.mapExpr(f)
    }
    implicit def forType(f: Type => Type): ExprMagnet = new ExprMagnet {
      override def map(expr: Expression): Expression = expr.mapType(f)
    }
    implicit def forWidth(f: Width => Width): ExprMagnet = new ExprMagnet {
      override def map(expr: Expression): Expression = expr.mapWidth(f)
    }
  }
  implicit class ExprMap(val _expr: Expression) extends AnyVal {
    def map[T](f: T => T)(implicit magnet: (T => T) => ExprMagnet): Expression = magnet(f).map(_expr)
  }

  // ********** Type Mappers **********
  trait TypeMagnet {
    def map(tpe: Type): Type
  }
  object TypeMagnet {
    implicit def forType(f: Type => Type): TypeMagnet = new TypeMagnet {
      override def map(tpe: Type): Type = tpe.mapType(f)
    }
    implicit def forWidth(f: Width => Width): TypeMagnet = new TypeMagnet {
      override def map(tpe: Type): Type = tpe.mapWidth(f)
    }
  }
  implicit class TypeMap(val _tpe: Type) extends AnyVal {
    def map[T](f: T => T)(implicit magnet: (T => T) => TypeMagnet): Type = magnet(f).map(_tpe)
  }

  // ********** Width Mappers **********
  trait WidthMagnet {
    def map(width: Width): Width
  }
  object WidthMagnet {
    implicit def forWidth(f: Width => Width): WidthMagnet = new WidthMagnet {
      override def map(width: Width): Width = width match {
        case mapable: HasMapWidth => mapable.mapWidth(f) // WIR
        case other => other // Standard IR nodes
      }
    }
  }
  implicit class WidthMap(val _width: Width) extends AnyVal {
    def map[T](f: T => T)(implicit magnet: (T => T) => WidthMagnet): Width = magnet(f).map(_width)
  }

  // ********** Module Mappers **********
  trait ModuleMagnet {
    def map(module: DefModule): DefModule
  }
  object ModuleMagnet {
    implicit def forStmt(f: Statement => Statement): ModuleMagnet = new ModuleMagnet {
      override def map(module: DefModule): DefModule = module.mapStmt(f)
    }
    implicit def forPorts(f: Port => Port): ModuleMagnet = new ModuleMagnet {
      override def map(module: DefModule): DefModule = module.mapPort(f)
    }
    implicit def forString(f: String => String): ModuleMagnet = new ModuleMagnet {
      override def map(module: DefModule): DefModule = module.mapString(f)
    }
    implicit def forInfo(f: Info => Info): ModuleMagnet = new ModuleMagnet {
      override def map(module: DefModule): DefModule = module.mapInfo(f)
    }
  }
  implicit class ModuleMap(val _module: DefModule) extends AnyVal {
    def map[T](f: T => T)(implicit magnet: (T => T) => ModuleMagnet): DefModule = magnet(f).map(_module)
  }

  // ********** Circuit Mappers **********
  trait CircuitMagnet {
    def map(module: Circuit): Circuit
  }
  object CircuitMagnet {
    implicit def forModules(f: DefModule => DefModule): CircuitMagnet = new CircuitMagnet {
      override def map(circuit: Circuit): Circuit = circuit.mapModule(f)
    }
    implicit def forString(f: String => String): CircuitMagnet = new CircuitMagnet {
      override def map(circuit: Circuit): Circuit = circuit.mapString(f)
    }
    implicit def forInfo(f: Info => Info): CircuitMagnet = new CircuitMagnet {
      override def map(circuit: Circuit): Circuit = circuit.mapInfo(f)
    }
  }
  implicit class CircuitMap(val _circuit: Circuit) extends AnyVal {
    def map[T](f: T => T)(implicit magnet: (T => T) => CircuitMagnet): Circuit = magnet(f).map(_circuit)
  }
}
