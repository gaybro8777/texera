package edu.uci.ics.texera.workflow.operators.source.scan.text

import edu.uci.ics.texera.workflow.common.tuple.Tuple
import edu.uci.ics.texera.workflow.common.tuple.schema.{AttributeType, Schema}
import edu.uci.ics.texera.workflow.operators.source.scan.FileAttributeType
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import scala.collection.convert.ImplicitConversions.`list asScalaBuffer`

class TextInputSourceOpDescSpec extends AnyFlatSpec with BeforeAndAfter {
  val TestTextFilePath: String = "src/test/resources/line_numbers.txt"
  val TestCRLFTextFilePath: String = "src/test/resources/line_numbers_crlf.txt"
  val TestNumbersFilePath: String = "src/test/resources/numbers.txt"
  var textInputSourceOpDesc: TextInputSourceOpDesc = _

  before {
    textInputSourceOpDesc = new TextInputSourceOpDesc()
  }

  it should "infer schema with single column representing each line of text in normal text scan mode" in {
    val inferredSchema: Schema = textInputSourceOpDesc.sourceSchema()

    assert(inferredSchema.getAttributes.length == 1)
    assert(inferredSchema.getAttribute("line").getType == AttributeType.STRING)
  }

  it should "infer schema with single column representing entire input in outputAsSingleTuple mode" in {
    textInputSourceOpDesc.attributeType = FileAttributeType.SINGLE_STRING
    val inferredSchema: Schema = textInputSourceOpDesc.sourceSchema()

    assert(inferredSchema.getAttributes.length == 1)
    assert(inferredSchema.getAttribute("line").getType == AttributeType.STRING)
  }

  it should "infer schema with user-specified output schema attribute" in {
    textInputSourceOpDesc.attributeType = FileAttributeType.STRING
    val customOutputAttributeName: String = "testing"
    textInputSourceOpDesc.attributeName = customOutputAttributeName
    val inferredSchema: Schema = textInputSourceOpDesc.sourceSchema()

    assert(inferredSchema.getAttributes.length == 1)
    assert(inferredSchema.getAttribute("testing").getType == AttributeType.STRING)
  }

  it should "infer schema with integer attribute type" in {
    textInputSourceOpDesc.attributeType = FileAttributeType.INTEGER
    val inferredSchema: Schema = textInputSourceOpDesc.sourceSchema()

    assert(inferredSchema.getAttributes.length == 1)
    assert(inferredSchema.getAttribute("line").getType == AttributeType.INTEGER)
  }

  it should "read first 5 lines of the input text into corresponding output tuples" in {
    val inputString: String = readFileIntoString(TestTextFilePath)
    textInputSourceOpDesc.textInput = inputString
    textInputSourceOpDesc.attributeType = FileAttributeType.STRING
    textInputSourceOpDesc.fileScanLimit = Option(5)
    val textScanSourceOpExec =
      new TextInputSourceOpExec(textInputSourceOpDesc)
    textScanSourceOpExec.open()
    val processedTuple: Iterator[Tuple] = textScanSourceOpExec.produceTexeraTuple()

    assert(processedTuple.next().getField("line").equals("line1"))
    assert(processedTuple.next().getField("line").equals("line2"))
    assert(processedTuple.next().getField("line").equals("line3"))
    assert(processedTuple.next().getField("line").equals("line4"))
    assert(processedTuple.next().getField("line").equals("line5"))
    assertThrows[java.util.NoSuchElementException](processedTuple.next().getField("line"))
    textScanSourceOpExec.close()
  }

  it should "read first 5 lines of the input text with CRLF separators into corresponding output tuples" in {
    val inputString: String = readFileIntoString(TestCRLFTextFilePath)
    textInputSourceOpDesc.textInput = inputString
    textInputSourceOpDesc.attributeType = FileAttributeType.STRING
    textInputSourceOpDesc.fileScanLimit = Option(5)
    val textScanSourceOpExec =
      new TextInputSourceOpExec(textInputSourceOpDesc)
    textScanSourceOpExec.open()
    val processedTuple: Iterator[Tuple] = textScanSourceOpExec.produceTexeraTuple()

    assert(processedTuple.next().getField("line").equals("line1"))
    assert(processedTuple.next().getField("line").equals("line2"))
    assert(processedTuple.next().getField("line").equals("line3"))
    assert(processedTuple.next().getField("line").equals("line4"))
    assert(processedTuple.next().getField("line").equals("line5"))
    assertThrows[java.util.NoSuchElementException](processedTuple.next().getField("line"))
    textScanSourceOpExec.close()
  }

  it should "read first 5 lines of the input text into a single output tuple" in {
    val inputString: String = readFileIntoString(TestTextFilePath)
    textInputSourceOpDesc.textInput = inputString
    textInputSourceOpDesc.attributeType = FileAttributeType.SINGLE_STRING
    val textScanSourceOpExec =
      new TextInputSourceOpExec(textInputSourceOpDesc)
    textScanSourceOpExec.open()
    val processedTuple: Iterator[Tuple] = textScanSourceOpExec.produceTexeraTuple()

    assert(
      processedTuple
        .next()
        .getField("line")
        .equals("line1\nline2\nline3\nline4\nline5\nline6\nline7\nline8\nline9\nline10")
    )
    assertThrows[java.util.NoSuchElementException](processedTuple.next().getField("line"))
    textScanSourceOpExec.close()
  }

  it should "read first 5 lines of the input text into corresponding output INTEGER tuples" in {
    val inputString: String = readFileIntoString(TestNumbersFilePath)
    textInputSourceOpDesc.textInput = inputString
    textInputSourceOpDesc.attributeType = FileAttributeType.INTEGER
    textInputSourceOpDesc.fileScanLimit = Option(5)
    val textScanSourceOpExec =
      new TextInputSourceOpExec(textInputSourceOpDesc)
    textScanSourceOpExec.open()
    val processedTuple: Iterator[Tuple] = textScanSourceOpExec.produceTexeraTuple()

    assert(processedTuple.next().getField("line").equals(1))
    assert(processedTuple.next().getField("line").equals(2))
    assert(processedTuple.next().getField("line").equals(3))
    assert(processedTuple.next().getField("line").equals(4))
    assert(processedTuple.next().getField("line").equals(5))
    assertThrows[java.util.NoSuchElementException](processedTuple.next().getField("line"))
    textScanSourceOpExec.close()
  }

  /**
    * Helper function using UTF-8 encoding to read text file
    * into String
    *
    * @param filePath path of input file
    * @return entire file represented as String
    */
  def readFileIntoString(filePath: String): String = {
    val path: Path = Paths.get(filePath)
    new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
  }
}
