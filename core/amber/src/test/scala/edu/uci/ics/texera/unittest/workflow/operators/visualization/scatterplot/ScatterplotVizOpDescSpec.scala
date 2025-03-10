package edu.uci.ics.texera.unittest.workflow.operators.visualization.scatterplot

import edu.uci.ics.amber.engine.common.workflow.PortIdentity
import edu.uci.ics.texera.workflow.common.WorkflowContext.{
  DEFAULT_EXECUTION_ID,
  DEFAULT_WORKFLOW_ID
}
import edu.uci.ics.texera.workflow.common.tuple.schema.{Attribute, AttributeType, Schema}
import edu.uci.ics.texera.workflow.operators.visualization.scatterplot.ScatterplotOpDesc
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

class ScatterplotVizOpDescSpec extends AnyFlatSpec with BeforeAndAfter {
  val correctSchema = new Schema(
    new Attribute("field1", AttributeType.DOUBLE),
    new Attribute("field2", AttributeType.INTEGER)
  )
  val wrongTypesSchema = new Schema(
    new Attribute("field1", AttributeType.DOUBLE),
    new Attribute("field2", AttributeType.STRING)
  )
  val lessNumberofArgumentsSchema = new Schema(
    new Attribute("field1", AttributeType.DOUBLE)
  )
  val moreNumberofArgumentsSchema = new Schema(
    new Attribute("field1", AttributeType.DOUBLE),
    new Attribute("field2", AttributeType.INTEGER),
    new Attribute("field3", AttributeType.INTEGER)
  )
  val hardcodedFieldNames = new Schema(
    new Attribute("xColumn", AttributeType.DOUBLE),
    new Attribute("yColumn", AttributeType.DOUBLE)
  )
  var scatterplotOpDesc: ScatterplotOpDesc = _

  before {
    scatterplotOpDesc = new ScatterplotOpDesc()
    scatterplotOpDesc.xColumn = "field1"
    scatterplotOpDesc.yColumn = "field2"
    scatterplotOpDesc.inputPortToSchemaMapping(PortIdentity()) = correctSchema
  }

  it should "take in both fields correctly without caring about the order" in {
    assert(scatterplotOpDesc.getOutputSchema(Array(correctSchema)).getAttributes.size() == 2)
  }

  it should "take in both fields correctly without caring about the extra fields" in {
    assert(
      scatterplotOpDesc
        .getOutputSchema(Array(moreNumberofArgumentsSchema))
        .getAttributes
        .size() == 2
    )
  }

  it should "raise IllegalArgumentException if less fields are passed (X, Y)" in {
    assertThrows[RuntimeException] {
      scatterplotOpDesc.getOutputSchema(Array(lessNumberofArgumentsSchema))
    }
  }

  it should "raise IllegalArgumentException if the field type is not a number" in {
    scatterplotOpDesc.inputPortToSchemaMapping(PortIdentity()) = wrongTypesSchema
    assertThrows[IllegalArgumentException] {
      scatterplotOpDesc.getPhysicalOp(
        DEFAULT_WORKFLOW_ID,
        DEFAULT_EXECUTION_ID
      )
    }
  }

  it should "build specific schema(for frontend to understand) if geometric" in {
    scatterplotOpDesc.isGeometric = true
    val outputSchema = scatterplotOpDesc.getOutputSchema(Array(correctSchema))
    assert(outputSchema.getAttributes.get(0).getName.equals("xColumn"))
    assert(outputSchema.getAttributes.get(1).getName.equals("yColumn"))
  }

  it should "build the schema without a problem if the field names (xColumn, yColumn) are the same as the hardcoded geometric but the type is not geometric" in {
    scatterplotOpDesc.xColumn = "xColumn"
    scatterplotOpDesc.yColumn = "yColumn"
    val outputSchema = scatterplotOpDesc.getOutputSchema(Array(hardcodedFieldNames))
    assert(!scatterplotOpDesc.isGeometric)
    assert(outputSchema.getIndex("xColumn") == 0)
    assert(outputSchema.getIndex("yColumn") == 1)
  }
}
