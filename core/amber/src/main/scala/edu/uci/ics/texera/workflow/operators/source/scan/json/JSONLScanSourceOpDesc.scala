package edu.uci.ics.texera.workflow.operators.source.scan.json

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyDescription}
import com.fasterxml.jackson.databind.JsonNode
import edu.uci.ics.amber.engine.architecture.deploysemantics.PhysicalOp
import edu.uci.ics.amber.engine.architecture.deploysemantics.layer.OpExecInitInfo
import edu.uci.ics.amber.engine.common.virtualidentity.{ExecutionIdentity, WorkflowIdentity}
import edu.uci.ics.texera.Utils.objectMapper
import edu.uci.ics.texera.workflow.common.tuple.schema.AttributeTypeUtils.inferSchemaFromRows
import edu.uci.ics.texera.workflow.common.tuple.schema.{Attribute, Schema}
import edu.uci.ics.texera.workflow.operators.source.scan.ScanSourceOpDesc
import edu.uci.ics.texera.workflow.operators.source.scan.json.JSONUtil.JSONToMap

import java.io.{BufferedReader, FileInputStream, IOException, InputStreamReader}
import scala.collection.JavaConverters._
import scala.collection.convert.ImplicitConversions.`iterator asScala`
import scala.collection.mutable.ArrayBuffer

class JSONLScanSourceOpDesc extends ScanSourceOpDesc {

  @JsonProperty(required = true)
  @JsonPropertyDescription("flatten nested objects and arrays")
  var flatten: Boolean = false

  fileTypeName = Option("JSONL")

  @throws[IOException]
  override def getPhysicalOp(
      workflowId: WorkflowIdentity,
      executionId: ExecutionIdentity
  ): PhysicalOp = {
    filePath match {
      case Some(path) =>
        // count lines and partition the task to each worker
        val reader = new BufferedReader(
          new InputStreamReader(new FileInputStream(path), fileEncoding.getCharset)
        )
        val offsetValue = offset.getOrElse(0)
        var lines = reader.lines().iterator().drop(offsetValue)
        if (limit.isDefined) lines = lines.take(limit.get)
        val count: Int = lines.map(_ => 1).sum
        reader.close()

        PhysicalOp
          .sourcePhysicalOp(
            workflowId,
            executionId,
            operatorIdentifier,
            OpExecInitInfo((idx, physicalOp, operatorConfig) => {
              val workerCount = operatorConfig.workerConfigs.length
              val startOffset: Int = offsetValue + count / workerCount * idx
              val endOffset: Int =
                offsetValue + (if (idx != workerCount - 1) count / workerCount * (idx + 1)
                               else count)
              new JSONLScanSourceOpExec(this, startOffset, endOffset)
            })
          )
          .withInputPorts(operatorInfo.inputPorts, inputPortToSchemaMapping)
          .withOutputPorts(operatorInfo.outputPorts, outputPortToSchemaMapping)
          .withParallelizable(true)

      case None =>
        throw new RuntimeException("File path is not provided.")
    }

  }

  /**
    * Infer Texera.Schema based on the top few lines of data.
    *
    * @return Texera.Schema build for this operator
    */
  @Override
  def inferSchema(): Schema = {
    val reader = new BufferedReader(
      new InputStreamReader(new FileInputStream(filePath.get), fileEncoding.getCharset)
    )
    var fieldNames = Set[String]()

    val allFields: ArrayBuffer[Map[String, String]] = ArrayBuffer()

    val startOffset = offset.getOrElse(0)
    val endOffset =
      startOffset + limit.getOrElse(INFER_READ_LIMIT).min(INFER_READ_LIMIT)
    reader
      .lines()
      .iterator()
      .asScala
      .slice(startOffset, endOffset)
      .foreach(line => {
        val root: JsonNode = objectMapper.readTree(line)
        if (root.isObject) {
          val fields: Map[String, String] = JSONToMap(root, flatten = flatten)
          fieldNames = fieldNames.++(fields.keySet)
          allFields += fields
        }
      })

    val sortedFieldNames = fieldNames.toList.sorted
    reader.close()

    val attributeTypes = inferSchemaFromRows(allFields.iterator.map(fields => {
      val result = ArrayBuffer[Object]()
      for (fieldName <- sortedFieldNames) {
        if (fields.contains(fieldName)) {
          result += fields(fieldName)
        } else {
          result += null
        }
      }
      result.toArray
    }))

    Schema.newBuilder
      .add(
        sortedFieldNames.indices
          .map(i => new Attribute(sortedFieldNames(i), attributeTypes(i)))
          .asJava
      )
      .build
  }

}
