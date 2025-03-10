package edu.uci.ics.texera.web

import java.time.{LocalDateTime, Duration => JDuration}
import akka.actor.Cancellable
import com.typesafe.scalalogging.LazyLogging
import edu.uci.ics.texera.web.storage.ExecutionStateStore
import edu.uci.ics.texera.web.workflowruntimestate.{ExecutionMetadataStore, WorkflowAggregatedState}
import edu.uci.ics.texera.web.workflowruntimestate.WorkflowAggregatedState.RUNNING

import scala.concurrent.duration.DurationInt

class WorkflowLifecycleManager(id: String, cleanUpTimeout: Int, cleanUpCallback: () => Unit)
    extends LazyLogging {
  private var userCount = 0
  private var cleanUpExecution: Cancellable = Cancellable.alreadyCancelled

  private[this] def setCleanUpDeadline(status: WorkflowAggregatedState): Unit = {
    synchronized {
      if (userCount > 0 || status == RUNNING) {
        cleanUpExecution.cancel()
        logger.info(
          s"[$id] workflow state clean up postponed. current user count = $userCount, workflow status = $status"
        )
      } else {
        refreshDeadline()
      }
    }
  }

  private[this] def refreshDeadline(): Unit = {
    if (cleanUpExecution.isCancelled || cleanUpExecution.cancel()) {
      logger.info(
        s"[$id] workflow state clean up will start at ${LocalDateTime.now().plus(JDuration.ofSeconds(cleanUpTimeout))}"
      )
      cleanUpExecution =
        TexeraWebApplication.scheduleCallThroughActorSystem(cleanUpTimeout.seconds) {
          cleanUp()
        }
    }
  }

  private[this] def cleanUp(): Unit = {
    synchronized {
      if (userCount > 0) {
        // do nothing
        logger.info(s"[$id] workflow state clean up failed. current user count = $userCount")
      } else {
        cleanUpExecution.cancel()
        cleanUpCallback()
        logger.info(s"[$id] workflow state clean up completed.")
      }
    }
  }

  def increaseUserCount(): Unit = {
    synchronized {
      userCount += 1
      cleanUpExecution.cancel()
      logger.info(s"[$id] workflow state clean up postponed. current user count = $userCount")
    }
  }

  def decreaseUserCount(currentWorkflowState: Option[WorkflowAggregatedState]): Unit = {
    synchronized {
      userCount -= 1
      if (userCount == 0 && (currentWorkflowState.isEmpty || currentWorkflowState.get != RUNNING)) {
        refreshDeadline()
      } else {
        logger.info(s"[$id] workflow state clean up postponed. current user count = $userCount")
      }
    }
  }

  def registerCleanUpOnStateChange(stateStore: ExecutionStateStore): Unit = {
    cleanUpExecution.cancel()
    stateStore.metadataStore.getStateObservable.subscribe { newState: ExecutionMetadataStore =>
      setCleanUpDeadline(newState.state)
    }
  }

}
