import { Component, Input, OnInit } from "@angular/core";
import { UntilDestroy } from "@ngneat/until-destroy";
import { WorkflowRuntimeStatistics } from "src/app/dashboard/user/type/workflow-runtime-statistics";
import * as Plotly from "plotly.js-dist-min";
import { MatTabChangeEvent } from "@angular/material/tabs";

@UntilDestroy()
@Component({
  selector: "texera-workflow-runtime-statistics",
  templateUrl: "./workflow-runtime-statistics.component.html",
  styleUrls: ["./workflow-runtime-statistics.component.scss"],
})
export class WorkflowRuntimeStatisticsComponent implements OnInit {
  @Input()
  workflowRuntimeStatistics?: WorkflowRuntimeStatistics[];

  private groupedStats?: Record<string, WorkflowRuntimeStatistics[]>;
  public metrics: string[] = ["Input Tuple Count", "Output Tuple Count"];

  constructor() {}

  ngOnInit(): void {
    if (!this.workflowRuntimeStatistics) {
      return;
    }

    this.groupedStats = this.groupStatsByOperatorId();
    this.createChart(0);
  }

  /**
   * Create a new line chart corresponding to the change of a tab
   * @param selection of a certain metric
   */
  onTabChanged(event: MatTabChangeEvent): void {
    this.createChart(event.index);
  }

  /**
   * Convert an array into a record by combining stats to the same metric and accumulate tuple counts
   */
  private groupStatsByOperatorId(): Record<string, WorkflowRuntimeStatistics[]> {
    if (!this.workflowRuntimeStatistics) {
      return {};
    }

    const beginTimestamp = this.workflowRuntimeStatistics[0].timestamp;
    return this.workflowRuntimeStatistics.reduce((acc: Record<string, WorkflowRuntimeStatistics[]>, stat) => {
      const statsArray = acc[stat.operatorId] || [];
      const lastStat = statsArray[statsArray.length - 1];

      if (lastStat) {
        stat.inputTupleCount += lastStat.inputTupleCount;
        stat.outputTupleCount += lastStat.outputTupleCount;
      }

      stat.timestamp -= beginTimestamp;

      acc[stat.operatorId] = [...statsArray, stat];
      return acc;
    }, {});
  }

  /**
   * Preprocess the dataset which will be used as an input for a line chart
   * 1. Shorten the operator ID
   * 2. Remove sink operator
   * 3. Contain only a certain metric given a metric idx
   * @param selection of a certain metric
   */
  private createDataset(metric_idx: number): any[] {
    if (!this.groupedStats) {
      return [];
    }

    return Object.keys(this.groupedStats)
      .map(operatorId => {
        const operatorName = operatorId.split("-")[0];
        const uuidLast6Digits = operatorId.slice(-6);

        if (operatorName === "sink ") {
          return null;
        }

        const yValues = metric_idx === 0 ? "inputTupleCount" : "outputTupleCount";
        if (!this.groupedStats) {
          return null;
        }
        const stats = this.groupedStats[operatorId];

        return {
          x: stats.map(stat => stat["timestamp"] / 1000),
          y: stats.map(stat => stat[yValues]),
          mode: "lines",
          name: `${operatorName}-${uuidLast6Digits}`,
        };
      })
      .filter(Boolean);
  }

  /**
   * Create a line chart using plotly
   * @param selection of a certain metric
   */
  private createChart(metric_idx: number): void {
    const dataset = this.createDataset(metric_idx);

    if (!dataset || dataset.length === 0) {
      return;
    }

    const layout = {
      title: this.metrics[metric_idx],
      xaxis: { title: "Time (s)" },
      yaxis: { title: this.metrics[metric_idx] },
    };

    Plotly.newPlot("chart", dataset, layout);
  }
}
