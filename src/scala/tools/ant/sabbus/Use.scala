/*                     __                                               *\
**     ________ ___   / /  ___     Scala Ant Tasks                      **
**    / __/ __// _ | / /  / _ |    (c) 2005-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.tools.ant.sabbus

import java.io.File

import org.apache.tools.ant.taskdefs.MatchingTask
import org.apache.tools.ant.types.{Path, Reference}
import org.apache.tools.ant.util.{GlobPatternMapper, SourceFileScanner}

class Use extends MatchingTask {
  
  def setId(input: String): Unit = {
    id = Some(input)
  }
  
  def setSrcdir(input: File) {
    sourceDir = Some(input)
  }
  
  def setDestdir(input: File): Unit = {
    destinationDir = Some(input)
  }
  
  def setFailOnError(input: Boolean): Unit = {
    failOnError = input
  }
  
  private var id: Option[String] = None
  private var sourceDir: Option[File] = None
  private var destinationDir: Option[File] = None
  private var failOnError: Boolean = true
  
  override def execute(): Unit = {
    if (id.isEmpty) error("Mandatory attribute 'id' is not set.")
    if (sourceDir.isEmpty) error("Mandatory attribute 'srcdir' is not set.")
    val compiler = Compilers(id.get)
    if (!destinationDir.isEmpty) compiler.settings.d = destinationDir.get
    val mapper = new GlobPatternMapper()
    mapper.setTo("*.class")
    mapper.setFrom("*.scala")
    val includedFiles: Array[File] =
      new SourceFileScanner(this).restrict(
        getDirectoryScanner(sourceDir.get).getIncludedFiles,
        sourceDir.get,
        compiler.settings.d,
        mapper
      ) map (new File(sourceDir.get, _))
    if (includedFiles.size > 0)
      try {
        log("Compiling " + includedFiles.size + " file" + (if (includedFiles.size > 1) "s" else "") + " to " + compiler.settings.d.getAbsolutePath)
        val (errors, warnings) = compiler.compile(includedFiles)
        if (errors > 0)
          error("Compilation failed with " + errors + " error" + (if (errors > 1) "s" else "") + ".")
        else if (warnings > 0)
          log("Compilation suceeded with " + warnings + " warning" + (if (warnings > 1) "s" else "") + ".")
      }
      catch {
        case CompilationFailure(msg, ex) =>
          ex.printStackTrace
          val errorMsg =
            "Compilation failed because of an internal compiler error (" + msg + "); see the error output for details."
          if (failOnError) error(errorMsg) else log(errorMsg)
      }
  }
  
}
