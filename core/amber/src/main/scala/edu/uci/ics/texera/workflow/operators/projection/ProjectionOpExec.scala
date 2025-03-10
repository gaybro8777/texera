package edu.uci.ics.texera.workflow.operators.projection

import com.google.common.base.Preconditions
import edu.uci.ics.texera.workflow.common.operators.map.MapOpExec
import edu.uci.ics.texera.workflow.common.tuple.Tuple
import edu.uci.ics.texera.workflow.common.tuple.schema.Schema

class ProjectionOpExec(
    var attributes: List[AttributeUnit],
    val outputSchema: Schema
) extends MapOpExec {

  def project(tuple: Tuple): Tuple = {
    Preconditions.checkArgument(attributes.nonEmpty)
    val builder = Tuple.newBuilder(outputSchema)
    attributes.foreach(attrName => {
      builder.add(
        attrName.getAlias,
        tuple.getSchema.getAttribute(attrName.getOriginalAttribute).getType,
        tuple.getField(attrName.getOriginalAttribute)
      )
    })
    builder.build()
  }

  setMapFunc(project)
}
