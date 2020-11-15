package mainargs
object MainUtils {
  def construct[T](cep: ClassMains[T],
                   args: Seq[String],
                   allowPositional: Boolean) = {
    Grouping.groupArgs(args, cep.main.argSigs, allowPositional)
      .flatMap(invoke(cep.companion(), cep.main, _))
      .map(_.value.asInstanceOf[T])
  }
  def invoke[B](target: B,
                main: MainData[B],
                grouping: Grouping[B]): Result[Computed[Any]] = {
    try main.invoke0(
      target,
      grouping.grouped.map{case (k, b) => (k.name, b)}, grouping.remaining
    ) catch{case e: Throwable => Result.Error.Exception(e)}
  }
  def runMains[T](mains: Mains[T],
                  args: Seq[String],
                  allowPositional: Boolean): Either[Result.Error.Early, (MainData[T], Result[Computed[Any]])] = {

    mains.value match{
      case Seq() => Left(Result.Error.Early.NoMainMethodsDetected())
      case Seq(main) =>
        Right(
          main,
          Grouping.groupArgs(args.toList, main.argSigs, allowPositional)
            .flatMap(MainUtils.invoke(mains.base(), main, _))
        )

      case multiple =>
        args.toList match{
          case List() => Left(Result.Error.Early.SubcommandNotSpecified(multiple.map(_.name)))
          case head :: tail =>
            if (head.startsWith("-")) {
              Left(Result.Error.Early.SubcommandSelectionDashes(head))
            } else{
              multiple.find(_.name == head) match{
                case None => Left(Result.Error.Early.UnableToFindSubcommand(head))
                case Some(main) =>
                  Right(
                    main,
                    Grouping
                      .groupArgs(tail, main.argSigs, allowPositional)
                      .flatMap(MainUtils.invoke(mains.base(), main, _))
                  )

              }
            }
        }
    }
  }
}