package edu.uci.ics.texera.workflow.operators.symmetricDifference

import com.google.common.base.Preconditions
import edu.uci.ics.amber.engine.architecture.deploysemantics.PhysicalOp
import edu.uci.ics.amber.engine.architecture.deploysemantics.layer.OpExecInitInfo
import edu.uci.ics.amber.engine.common.virtualidentity.{ExecutionIdentity, WorkflowIdentity}
import edu.uci.ics.texera.workflow.common.metadata.{OperatorGroupConstants, OperatorInfo}
import edu.uci.ics.texera.workflow.common.operators.LogicalOp
import edu.uci.ics.texera.workflow.common.tuple.schema.Schema
import edu.uci.ics.amber.engine.common.workflow.{InputPort, OutputPort, PortIdentity}

class SymmetricDifferenceOpDesc extends LogicalOp {

  override def getPhysicalOp(
      workflowId: WorkflowIdentity,
      executionId: ExecutionIdentity
  ): PhysicalOp = {
    PhysicalOp.hashPhysicalOp(
      workflowId,
      executionId,
      operatorIdentifier,
      OpExecInitInfo((_, _, _) => new SymmetricDifferenceOpExec()),
      operatorInfo.inputPorts
        .map(inputPort => inputPortToSchemaMapping(inputPort.id))
        .head
        .getAttributes
        .toArray
        .indices
        .toList
    )
  }

  override def operatorInfo: OperatorInfo =
    OperatorInfo(
      "SymmetricDifference",
      "find the symmetric difference (the set of elements which are in either of the sets, but not in their intersection) of two inputs",
      OperatorGroupConstants.UTILITY_GROUP,
      inputPorts = List(InputPort(PortIdentity(0)), InputPort(PortIdentity(1))),
      outputPorts = List(OutputPort())
    )

  override def getOutputSchema(schemas: Array[Schema]): Schema = {
    Preconditions.checkArgument(schemas.forall(_ == schemas(0)))
    schemas(0)
  }

}
