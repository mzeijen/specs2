package org.specs2
package reporter

import specification._

class SequenceSpec extends Specification with ScalaCheck with ArbitraryFragments { def is =
                                                                                                                       """
 Before executing and reporting a specification, the fragments must be arranged for execution:

  * steps must be executed before examples as specified
  * the 'sequential' argument forces the each fragment to be sequenced
  * if the 'isolated' argument is present then Examples/Steps/Actions bodies must be copied
    so that they'll be executed in a separate Specification instance
                                                                                                                        """^
                                                                                                                        p^
  "if a specification contains steps they must be grouped before the examples"                                          ^
    "2 consecutive steps must not be in the same list"                                                                  ! steps().e1^
    "2 consecutive examples must be in the same list"                                                                   ! steps().e2^
    "an example followed by a step must not be in the same list"                                                        ! steps().e3^
    "a step followed by an example must not be in the same list"                                                        ! steps().e4^
    "in any specification steps and examples are always separated"                                                      ! steps().e5^
                                                                                                                        p^
  "if a specification contains the 'sequential' argument"                                                               ^
    "all examples must be executed in a sequence"                                                                       ! seq().e1^
    "with a Reporter"                                                                                                   ! seq().e2^
                                                                                                                        p^
  "if a specification contains the 'isolated' argument"                                                                 ^
    "examples bodies must be copied"                                                                                    ! isolate().e1^
      "along with all the previous steps"                                                                               ! isolate().e2^bt^
    "steps bodies must not be copied"                                                                                   ! isolate().e3^
    "actions bodies must be copied"                                                                                     ! isolate().e4^
    "if the examples, steps or actions are marked as global, they are never copied"                                     ! isolate().e5^
      "with a global step before an example"                                                                            ! isolate().e6^
                                                                                                                        end

  case class steps() extends ScalaCheck with WithSelection {
    implicit val params = set(maxSize -> 3)

    def e1 = check { (fs: Fragments) =>
      val selected = selectSequence(fs ^ step("1"))
      val selected2 = selectSequence(fs ^ step("1") ^ step("2"))
      selected2 must not have size(selected.size)
    }
    def e2 = check { (fs: Fragments) =>
      val selected = selectSequence(fs ^ ex1)
      val selected2 = selectSequence(fs ^ ex1 ^ ex2)
      selected2 must have size(selected.size)
    }
    def e3 = check { (fs: Fragments) =>
      val selected = selectSequence(fs ^ ex1)
      val selected2 = selectSequence(fs ^ ex1 ^ step("1"))
      selected2 must have size(selected.size + 1)
    }
    def e4 = check { (fs: Fragments) =>
      val selected = selectSequence(fs ^ step("1"))
      val selected2 = selectSequence(fs ^ step("1") ^ ex2)
      selected2 must have size(selected.size + 1)
    }
    def e5 = {
      val fragments: Fragments = "intro" ^ step("1") ^ ex1 ^ ex2 ^ step("2") ^ step("3") ^ ex1 ^ ex2
      selectSequence(fragments).map((s: FragmentSeq) => s.fragments.toString) must contain(
        "List(SpecStart(), Text(intro), Step)",
        "List(Example(ex1), Example(ex2))",
        "List(Step)",
        "List(Step)",
        "List(Example(ex1), Example(ex2), SpecEnd())").inOrder
    }
  }

  case class seq() extends WithSelection {
    def e1 = {
      val fragments: Fragments = sequential ^ example("e1") ^ step("s1") ^ example("e2")
      select(fragments).toString must_== "List(SpecStart(Object), Example(e1), Step, Example(e2), SpecEnd(Object))"
    }
    def e2 = {
      val spec = new Specification { def is = sequential ^ example("e1") ^ step("s1") ^ example("e2") }
      reporter.report(spec)(main.Arguments())
      reporter.messages must contain("e1", "s1", "e2").inOrder
    }
  }

  case class isolate() extends WithSelection {
    implicit val arguments = main.Arguments()

    def isIsolated(spec: SpecificationWithLocalVariable, expectedLocalValue: Int) = {
      reporter.report(spec)
      spec.i === expectedLocalValue
    }

    def e1 = isIsolated(new SpecificationWithLocalVariable { def is = isolated ^ "e1" ! { i = 1; ok } }, expectedLocalValue = 0)
    def e2 = isIsolated(new SpecificationWithLocalVariable { def is = isolated ^ Step(i = 1) ^ "e1" ! ok ^ Step(i = 2) }, expectedLocalValue = 2)
    def e3 = isIsolated(new SpecificationWithLocalVariable { def is = isolated ^ Step(i = 1) }, expectedLocalValue = 1)
    def e4 = isIsolated(new SpecificationWithLocalVariable { def is = isolated ^ Action(i = 1) }, expectedLocalValue = 0)

    def e5 = isIsolated(new SpecificationWithLocalVariable { def is = isolated ^ ("e1" ! { i = 1; ok }).global }, expectedLocalValue = 1)
    def e6 = isIsolated(new SpecificationWithLocalVariable { def is = isolated ^ Step(i = 1).global ^ "e1" ! { i = 2; ok } }, expectedLocalValue = 1)
  }

  trait SpecificationWithLocalVariable extends Specification {
    var i = 0
  }
  val ex1 = "ex1" ! success
  val ex2 = "ex2" ! success

}