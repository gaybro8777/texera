package edu.uci.ics.texera.workflow.operators.sentiment

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyDescription}
import com.google.common.base.Preconditions
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject
import edu.uci.ics.amber.engine.architecture.deploysemantics.PhysicalOp
import edu.uci.ics.amber.engine.architecture.deploysemantics.layer.OpExecInitInfo
import edu.uci.ics.amber.engine.common.virtualidentity.{ExecutionIdentity, WorkflowIdentity}
import edu.uci.ics.amber.engine.common.workflow.{InputPort, OutputPort}
import edu.uci.ics.texera.workflow.common.metadata.{OperatorGroupConstants, OperatorInfo}
import edu.uci.ics.texera.workflow.common.metadata.annotations.AutofillAttributeName
import edu.uci.ics.texera.workflow.common.operators.map.MapOpDesc
import edu.uci.ics.texera.workflow.common.tuple.schema.{AttributeType, Schema}

@JsonSchemaInject(json = """
{
  "attributeTypeRules": {
    "attribute": {
      "enum": ["string"]
    }
  }
}
""")
class SentimentAnalysisOpDesc extends MapOpDesc {
  @JsonProperty(value = "attribute", required = true)
  @JsonPropertyDescription("column to perform sentiment analysis on")
  @AutofillAttributeName
  var attribute: String = _

  @JsonProperty(
    value = "result attribute",
    required = true,
    defaultValue = "sentiment"
  )
  @JsonPropertyDescription("column name of the sentiment analysis result")
  var resultAttribute: String = _

  override def getPhysicalOp(
      workflowId: WorkflowIdentity,
      executionId: ExecutionIdentity
  ): PhysicalOp = {
    if (attribute == null)
      throw new RuntimeException("sentiment analysis: attribute is null")

    val outputSchema =
      operatorInfo.outputPorts.map(outputPort => outputPortToSchemaMapping(outputPort.id)).head
    PhysicalOp
      .oneToOnePhysicalOp(
        workflowId,
        executionId,
        operatorIdentifier,
        OpExecInitInfo((_, _, _) => new SentimentAnalysisOpExec(this, outputSchema))
      )
      .withInputPorts(operatorInfo.inputPorts, inputPortToSchemaMapping)
      .withOutputPorts(operatorInfo.outputPorts, outputPortToSchemaMapping)
  }

  override def operatorInfo =
    new OperatorInfo(
      "Sentiment Analysis",
      "analysis the sentiment of a text using machine learning",
      OperatorGroupConstants.ANALYTICS_GROUP,
      inputPorts = List(InputPort()),
      outputPorts = List(OutputPort()),
      supportReconfiguration = true
    )

  override def getOutputSchema(schemas: Array[Schema]): Schema = {
    Preconditions.checkArgument(schemas.length == 1)
    if (resultAttribute == null || resultAttribute.trim.isEmpty)
      return null
    Schema.newBuilder.add(schemas(0)).add(resultAttribute, AttributeType.INTEGER).build
  }
}
