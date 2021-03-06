package skascheme {
  object Core {

    import scala.collection._

    /** 
     * Core scheme functions
     */
    Interpreter.default.env += (SSymbol("define") -> ((args: Seq[SExpr]) => {
      if(args.size == 0)
        err("define: Bad syntax.")
      else if (args.size < 2)
        err("define: Bad syntax (no expressions for procedure body)")
      else {
        // defined function args
        val fArgs = args(0)
        // TODO should accept e1 e2 e3 ... as body construction
        val body = args(1)

        // presuppose that result is ok
        var res = ok
        var fName = SSymbol("undefined")

        fArgs match {
          case (l: SList) if !l.value.isEmpty => 
            // Check if arguments are symbols
            if(l.value.foldLeft(true) {
              case (true, s: SSymbol) => true
              case _ => false
              }) {
                // Get defined function name
                fName = l.value(0) match {
                  case s: SSymbol => s
                  case _ => throw new Exception("Should never happen!")
                }

                // Get local function vars
                var fLocalVars = l.value.tail.map {
                  case s: SSymbol => s
                  case _ => throw new Exception("Should never happen!")
                }

                // Add new function to the environment
                Interpreter.default.env += (fName -> ((rargs: Seq[SExpr]) =>
                    if(fLocalVars.size != rargs.size)
                      err(s"$fName: Expected ${fLocalVars.size} arguments, found ${rargs.size}.")
                    else {
                      val localEnv = (fLocalVars zip rargs).toMap

                      // subst and eval
                      Interpreter.default.reduce(subst(localEnv, body))
                    }
                    ))
              } else {
                // Not all arguments are symbols
                res = err("define: Invalid local variables definition.")
              }
                  case _ => res = err("define: Expected name and params.")
        }

        res
      }
    }))

    Interpreter.default.env += (SSymbol("quote") -> ((args: Seq[SExpr]) => {
      if(args.size > 1)
        err("quote: Invalid arguments.")
      else
        args(0) match {
          // empty SList evals to null
                  case e: SList if e.value.size == 0 => SNil()

                  // Anything else is the exact thing
                  // TODO might need to cover some more special cases
                  case e: SExpr => e
        }
    }))


    // (let ((x 1) (y 2)) expr)
    Interpreter.default.env += (SSymbol("let") -> ((args: Seq[SExpr]) => {
      if(args.size == 0)
        err("let: Invalid arguments.")
      else if(args.size < 2)
        err("let: Missing body.")
      else {
        val varValPairs = args(0) match {
          case s: SList => s.value
          case _ => throw new Exception("Should never happen")
        }

        val body = args(1)
        var localEnv = mutable.Map[SSymbol, SExpr]()

        varValPairs.foreach {
          x => x match {
            case l: SList => l.value match {
              case (s: SSymbol) :: (e: SExpr) :: Nil =>
                localEnv += (
                  s -> Interpreter.default.reduce(e)
                )
              case _ => throw new Exception("Might want to handle this better")
            }

              case _ => throw new Exception("Might want to handle this better")
          }
        }

        Interpreter.default.reduce(subst(localEnv, body))
      }
    }))


    Interpreter.default.env += (SSymbol("let*") -> ((args: Seq[SExpr]) => {
      if(args.size == 0)
        err("let*: Invalid arguments.")
      else if(args.size < 2)
        err("let*: Missing body.")
      else {
        val varValPairs = args(0) match {
          case s: SList => s.value
          case _ => throw new Exception("Should never happen")
        }

        val body = args(1)
        var localEnv = mutable.Map[SSymbol, SExpr]()

        varValPairs.foreach {
          x => x match {
            case l: SList => l.value match {
              case (s: SSymbol) :: (e: SExpr) :: Nil =>
                localEnv += (
                  s -> Interpreter.default.reduce(subst(localEnv, e))
                )
              case _ => throw new Exception("Might want to handle this better")
            }

              case _ => throw new Exception("Might want to handle this better")
          }
        }

        Interpreter.default.reduce(subst(localEnv, body))
      }
    }))

    Interpreter.default.env += (SSymbol("letrec") -> ((args: Seq[SExpr]) => {
      if(args.size == 0)
        err("letrec: Invalid arguments.")
      else if(args.size < 2)
        err("letrec: Missing body.")
      else {
        var varValPairs = args(0) match {
          case s: SList => s.value
          case _ => throw new Exception("Should never happen")
        }

        val body = args(1)
        var localEnv = mutable.Map[SSymbol, SExpr]()

        // Add all functions to the env, in order
        // to allow recursive calls
        varValPairs.foreach {
          x => x match {
            case l: SList => l.value match {
              case (s: SSymbol) :: (e: SExpr) :: Nil =>
                e match {
                  // If first element is a symbol,
                  // then we have a function call
                  // and this may contain a recursion
                  // so we expand the env
              case l: SList => {
                // if this is not a function call,
                // it will error later anyways
                localEnv += (s -> e)
              }

              case _ => Unit
                }
              case _ => throw new Exception("Might want to handle this better")
            }
              case _ => throw new Exception("Might want to handle this better")
          }
        }

        // Now we eval
        varValPairs.foreach {
          x => x match {
            case l: SList => l.value match {
              case (s: SSymbol) :: (e: SExpr) :: Nil =>
                Interpreter.default.reduce(subst(localEnv, e))
              case _ => throw new Exception("Might want to handle this better")
            }
              case _ => throw new Exception("Might want to handle this better")
          }
        }

        Interpreter.default.reduce(subst(localEnv, body))
      }
    }))

    /** 
     * Functions for working with numbers
     */
    Interpreter.default.env += (SSymbol("+") -> ((args: Seq[SExpr]) => {
      var res: SExpr = SNumber(0)
      args.foreach {
        e => e match {
          case n: SNumber => res match {
            case rn: SNumber => res = SNumber(rn.value + n.value)
            case _ => res = res // do not change
          }

            case exp: SExpr => exp match {
              case s: SSymbol => res = err(s"${s.toStr}: undefined.")
              case _ => res = err(s"+: Invalid argument.")
            }

        }
      }

      res
    }))
  }
}
