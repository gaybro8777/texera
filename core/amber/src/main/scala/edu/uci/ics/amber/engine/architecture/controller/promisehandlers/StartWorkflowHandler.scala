package edu.uci.ics.amber.engine.architecture.controller.promisehandlers

import com.twitter.util.Future
import edu.uci.ics.amber.engine.architecture.controller.ControllerAsyncRPCHandlerInitializer
import edu.uci.ics.amber.engine.architecture.controller.promisehandlers.StartWorkflowHandler.StartWorkflow
import edu.uci.ics.amber.engine.common.rpc.AsyncRPCServer.ControlCommand

object StartWorkflowHandler {
  final case class StartWorkflow() extends ControlCommand[Unit]
}

/** start the workflow by starting the source workers
  * note that this SHOULD only be called once per workflow
  *
  * possible sender: client
  */
trait StartWorkflowHandler {
  this: ControllerAsyncRPCHandlerInitializer =>

  registerHandler { (msg: StartWorkflow, sender) =>
    {
      if (cp.executionState.getState.isUninitialized) {
        cp.workflowScheduler
          .startWorkflow(cp.workflow, cp.actorRefService, cp.actorService)
          .map(_ => {
            cp.controllerTimerService.enableStatusUpdate()
            cp.controllerTimerService.enableMonitoring()
            cp.controllerTimerService.enableSkewHandling()
          })
      } else {
        Future.Unit
      }
    }
  }
}
