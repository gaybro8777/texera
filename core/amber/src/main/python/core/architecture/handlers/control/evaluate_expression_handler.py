from proto.edu.uci.ics.amber.engine.architecture.worker import (
    EvaluateExpressionV2,
    EvaluatedValue,
)
from core.architecture.handlers.control.control_handler_base import ControlHandler
from core.architecture.managers.context import Context
from core.util.expression_evaluator import ExpressionEvaluator


class EvaluateExpressionHandler(ControlHandler):
    cmd = EvaluateExpressionV2

    def __call__(self, context: Context, command: cmd, *args, **kwargs):
        runtime_context = {
            r"self": context.operator_manager.operator,
            r"tuple_": context.tuple_processing_manager.current_input_tuple,
            r"input_": context.tuple_processing_manager.current_input_link,
        }

        evaluated_value: EvaluatedValue = ExpressionEvaluator.evaluate(
            command.expression, runtime_context
        )

        return evaluated_value
