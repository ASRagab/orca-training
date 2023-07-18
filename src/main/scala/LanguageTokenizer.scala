object LanguageTokenizer extends App {
  import scala.util.chaining._

  val dict = Set("small", "red", "house", "car", "cart", "horse", "ear", "plug", "earplug", "plugin", "in")

  def tokenizeBrute(phrase: String): Set[String] = {
    phrase.indices
      .flatMap(idx => (1 to phrase.length - idx).map(len => phrase.substring(idx, idx + len)))
      .filter(dict.contains)
      .toSet
  }

  def tokenizeUsingSorted(phrase: String): Set[String] = {
    val sorted = dict.toList.sortBy(-_.length)

    def tokenizeHelper(input: String, acc: Set[String]): Set[String] = {
      if (input.isEmpty) acc
      else
        sorted
          .find(input.startsWith)
          .fold(Set.empty[String])(w => tokenizeHelper(input.drop(w.length), acc + w))
    }

    tokenizeHelper(phrase, Set.empty)
  }

  def tokenizeMultiUsingSorted(phrase: String): List[List[String]] = {
    val sorted = dict.toList.sortBy(-_.length)

    def tokenizeHelper(input: String, acc: List[List[String]]): List[List[String]] = {
      if (input.isEmpty) acc
      else
        sorted.filter(input.startsWith) match {
          case Nil   => List.empty
          case words => words.flatMap(w => tokenizeHelper(input.drop(w.length), acc.map(w :: _)))
        }

    }

    tokenizeHelper(phrase, List(List.empty))
  }

  def tokenize(phrase: String): List[String] =
    phrase.indices.reverse
      .collectFirst { case ix if dict.contains(phrase.take(ix + 1)) => phrase.take(ix + 1) } // find the longest word
      .fold(List.empty[String]) { word =>
        if (word.length == phrase.length) List(word)
        else {
          val remaining = tokenize(phrase.drop(word.length))
          if (remaining.isEmpty) Nil else word :: remaining
        }
      }

  def tokenizeMulti(phrase: String): List[List[String]] =
    if (phrase.isEmpty) List(Nil)
    else
      (1 to phrase.length)
        .collect { case ix if dict.contains(phrase.take(ix)) => phrase.take(ix) }
        .flatMap { word =>
          tokenizeMulti(phrase.drop(word.length))
            .map(remaining => word :: remaining)
        }
        .toList

  def tokenizeFn(phrase: String, index: Set[String]) = tokenizeMultiUsingSorted(phrase)

  tokenizeFn("smallredhouse", dict).tap(println)
  tokenizeFn("AAAsmallredhouse", dict).tap(println)
  tokenizeFn("smallredhouseAAA", dict).tap(println)

  tokenizeFn("carthorse", dict).tap(println)
  tokenizeFn("earplugin", dict).tap(println)
//
////assert(tokenize("smallredhouse", set) == List(List("small", "red", "house")))
////  assert(tokenizeMulti("AAAsmallredhouse", set) == List(List("small", "red", "house")))
//assert(tokenize("carthorse", set).toSet == List(List("car"), List("cart", "horse")).toSet)
//assert(tokenize("earplugin", set).toSet == List(List("ear", "plug", "in"), List("earplug", "in"), List("ear", "plugin")).toSet)
}
