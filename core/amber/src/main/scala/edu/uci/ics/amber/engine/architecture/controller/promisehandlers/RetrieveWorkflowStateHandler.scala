package edu.uci.ics.amber.engine.architecture.controller.promisehandlers

import edu.uci.ics.amber.engine.architecture.controller.ControllerAsyncRPCHandlerInitializer
import edu.uci.ics.amber.engine.architecture.controller.promisehandlers.ChannelMarkerHandler.PropagateChannelMarker
import edu.uci.ics.amber.engine.architecture.controller.promisehandlers.RetrieveWorkflowStateHandler.RetrieveWorkflowState
import edu.uci.ics.amber.engine.architecture.worker.promisehandlers.RetrieveStateHandler.RetrieveState
import edu.uci.ics.amber.engine.common.ambermessage.NoAlignment
import edu.uci.ics.amber.engine.common.rpc.AsyncRPCServer.ControlCommand
import edu.uci.ics.amber.engine.common.virtualidentity.{ActorVirtualIdentity, ChannelMarkerIdentity}

import java.time.Instant
object RetrieveWorkflowStateHandler {
  final case class RetrieveWorkflowState() extends ControlCommand[Map[ActorVirtualIdentity, Unit]]
}

trait RetrieveWorkflowStateHandler {

  this: ControllerAsyncRPCHandlerInitializer =>
  registerHandler { (msg: RetrieveWorkflowState, sender) =>
    execute(
      PropagateChannelMarker(
        cp.executionState.getAllOperatorExecutions.map(_._1).toSet,
        ChannelMarkerIdentity("RetrieveWorkflowState_" + Instant.now().toString),
        NoAlignment,
        cp.workflow.physicalPlan,
        cp.workflow.physicalPlan.operators.map(_.id),
        RetrieveState()
      ),
      sender
    ).map { ret =>
      ret.map {
        case (actorId, value) =>
          (actorId, value.asInstanceOf[Unit])
      }.toMap
    }
  }
}
