import { mockResultPredicate, mockPoint } from './../workflow-graph/model/mock-workflow-data';
import { TestBed, inject } from '@angular/core/testing';
import * as joint from 'jointjs';

import { JointUIService, deleteButtonPath, sourceOperatorHandle, targetOperatorHandle } from './joint-ui.service';
import { OperatorMetadataService } from '../operator-metadata/operator-metadata.service';
import { StubOperatorMetadataService } from '../operator-metadata/stub-operator-metadata.service';
import { mockScanPredicate, mockSentimentPredicate } from '../workflow-graph/model/mock-workflow-data';
import { mockScanStatistic1, mockScanStatistic2 } from '../workflow-status/mock-workflow-status';
import { OperatorStates } from '../../types/execute-workflow.interface';

describe('JointUIService', () => {
  let service: JointUIService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        JointUIService,
        { provide: OperatorMetadataService, useClass: StubOperatorMetadataService },
      ],
    });
    service = TestBed.get(JointUIService);
  });

  it('should be created', inject([JointUIService], (injectedService: JointUIService) => {
    expect(injectedService).toBeTruthy();
  }));

  /**
   * Check if the getJointOperatorElement() can successfully creates a JointJS Element
   */
  it('should create an JointJS Element successfully when the function is called', () => {
    const result = service.getJointOperatorElement(
      mockScanPredicate, mockPoint);
    expect(result).toBeTruthy();
  });

  /**
   * Check if the error in getJointOperatorElement() is correctly thrown
   */
  it('should throw an error with an non existing operator', () => {
    expect(() => {
      service.getJointOperatorElement(
        {
          operatorID: 'nonexistOperator',
          operatorType: 'nonexistOperatorType',
          operatorProperties: {},
          inputPorts: [],
          outputPorts: [],
          showAdvanced: true
        },
        mockPoint
      );
    }).toThrowError(new RegExp(`doesn't exist`));
  });

  /**
   * Check if the getJointTooltipElement() can successfully creates a JointJS Element
   */
  it('should create an JointJS Element successfully when the function is called', () => {
    const result = service.getJointTooltipElement(
      mockScanPredicate, mockPoint);
    expect(result).toBeTruthy();
  });

  /**
   * Check if the error in getJointTooltipElement() is correctly thrown
   */
  it('should throw an error with an non existing operator', () => {
    expect(() => {
      service.getJointTooltipElement(
        {
          operatorID: 'nonexistOperator',
          operatorType: 'nonexistOperatorType',
          operatorProperties: {},
          inputPorts: [],
          outputPorts: [],
          showAdvanced: true
        },
        mockPoint
      );
    }).toThrowError(new RegExp(`doesn't exist`));
  });

  it('should reveal/hide tooltip and its content when showToolTip/hideTooltip is called', () => {
    const jointGraph = new joint.dia.Graph();
    const jointPaperOptions: joint.dia.Paper.Options = {model: jointGraph};
    const paper = new joint.dia.Paper(jointPaperOptions);

    jointGraph.addCell([
      service.getJointOperatorElement(
        mockScanPredicate,
        mockPoint
      ),
      service.getJointTooltipElement(
        mockScanPredicate,
        mockPoint
      )
    ]);
    const graph_tooltip1 = jointGraph.getCell('tooltip-' + mockScanPredicate.operatorID);
    expect(graph_tooltip1.attr('polygon')['display']).toEqual('none');
    expect(graph_tooltip1.attr('#operatorCount')['display']).toEqual('none');
    expect(graph_tooltip1.attr('#operatorSpeed')['display']).toEqual('none');

    service.showToolTip(paper, 'tooltip-' + mockScanPredicate.operatorID);
    expect(graph_tooltip1.attr('polygon')['display']).toBeUndefined();
    expect(graph_tooltip1.attr('#operatorCount')['display']).toBeUndefined();
    expect(graph_tooltip1.attr('#operatorSpeed')['display']).toBeUndefined();

    service.hideToolTip(paper, 'tooltip-' + mockScanPredicate.operatorID);
    expect(graph_tooltip1.attr('polygon')['display']).toEqual('none');
    expect(graph_tooltip1.attr('#operatorCount')['display']).toEqual('none');
    expect(graph_tooltip1.attr('#operatorSpeed')['display']).toEqual('none');
  });

  it('should update the content in the tooltip when changeOperatorTooltipInfo is called', () => {
    const jointGraph = new joint.dia.Graph();
    const jointPaperOptions: joint.dia.Paper.Options = {model: jointGraph};
    const paper = new joint.dia.Paper(jointPaperOptions);

    jointGraph.addCell([
      service.getJointOperatorElement(
        mockScanPredicate,
        mockPoint
      ),
      service.getJointTooltipElement(
        mockScanPredicate,
        mockPoint
      )
    ]);
    const tooltipId = 'tooltip-' + mockScanPredicate.operatorID;
    const graph_tooltip = jointGraph.getCell(tooltipId);
    expect(graph_tooltip.attr('#operatorCount')['text']).toBeUndefined();
    expect(graph_tooltip.attr('#operatorSpeed')['text']).toBeUndefined();
    service.changeOperatorTooltipInfo(paper, tooltipId, mockScanStatistic1);
    expect(graph_tooltip.attr('#operatorCount')['text']).toEqual('Output:' + mockScanStatistic1.outputCount + ' tuples');
    expect(graph_tooltip.attr('#operatorSpeed')['text']).toEqual('Speed:' + mockScanStatistic1.speed + ' tuples/ms');
    service.changeOperatorTooltipInfo(paper, tooltipId, mockScanStatistic2);
    expect(graph_tooltip.attr('#operatorCount')['text']).toEqual('Output:' + mockScanStatistic2.outputCount + ' tuples');
    expect(graph_tooltip.attr('#operatorSpeed')['text']).toEqual('Speed:' + mockScanStatistic2.speed + ' tuples/ms');
  });

  it('should change the operator state name and color when changeOperatorStates is called', () => {
    const jointGraph = new joint.dia.Graph();
    const jointPaperOptions: joint.dia.Paper.Options = {model: jointGraph};
    const paper = new joint.dia.Paper(jointPaperOptions);

    jointGraph.addCell(
      service.getJointOperatorElement(
        mockScanPredicate,
        mockPoint
    ));

    const graph_operator = jointGraph.getCell(mockScanPredicate.operatorID);
    expect(graph_operator.attr('#operatorStates')['text']).toEqual('Ready');
    expect(graph_operator.attr('#operatorStates')['fill']).toEqual('green');
    service.changeOperatorStates(paper, mockScanPredicate.operatorID, OperatorStates.Initializing);
    expect(graph_operator.attr('#operatorStates')['text']).toEqual('Initializing');
    expect(graph_operator.attr('#operatorStates')['fill']).toEqual('orange');
    service.changeOperatorStates(paper, mockScanPredicate.operatorID, OperatorStates.Running);
    expect(graph_operator.attr('#operatorStates')['text']).toEqual('Running');
    expect(graph_operator.attr('#operatorStates')['fill']).toEqual('orange');
    service.changeOperatorStates(paper, mockScanPredicate.operatorID, OperatorStates.Pausing);
    expect(graph_operator.attr('#operatorStates')['text']).toEqual('Pausing');
    expect(graph_operator.attr('#operatorStates')['fill']).toEqual('red');
    service.changeOperatorStates(paper, mockScanPredicate.operatorID, OperatorStates.Paused);
    expect(graph_operator.attr('#operatorStates')['text']).toEqual('Paused');
    expect(graph_operator.attr('#operatorStates')['fill']).toEqual('red');
    service.changeOperatorStates(paper, mockScanPredicate.operatorID, OperatorStates.Completed);
    expect(graph_operator.attr('#operatorStates')['text']).toEqual('Completed');
    expect(graph_operator.attr('#operatorStates')['fill']).toEqual('green');
  });


  /**
   * Check if the number of inPorts and outPorts created by getJointOperatorElement()
   * matches the port number specified by the operator metadata
   */
  it('should create correct number of inPorts and outPorts based on operator metadata', () => {
    const element1 = service.getJointOperatorElement(mockScanPredicate, mockPoint);
    const element2 = service.getJointOperatorElement(mockSentimentPredicate, mockPoint);
    const element3 = service.getJointOperatorElement(mockResultPredicate, mockPoint);

    const inPortCount1 = element1.getPorts().filter(port => port.group === 'in').length;
    const outPortCount1 = element1.getPorts().filter(port => port.group === 'out').length;
    const inPortCount2 = element2.getPorts().filter(port => port.group === 'in').length;
    const outPortCount2 = element2.getPorts().filter(port => port.group === 'out').length;
    const inPortCount3 = element3.getPorts().filter(port => port.group === 'in').length;
    const outPortCount3 = element3.getPorts().filter(port => port.group === 'out').length;

    expect(inPortCount1).toEqual(0);
    expect(outPortCount1).toEqual(1);
    expect(inPortCount2).toEqual(1);
    expect(outPortCount2).toEqual(1);
    expect(inPortCount3).toEqual(1);
    expect(outPortCount3).toEqual(0);

  });

  /**
   * Check if the custom attributes / svgs are correctly used by the JointJS graph
   */
  it('should apply the custom SVG styling to the JointJS element', () => {

    const graph = new joint.dia.Graph();

    graph.addCell([
      service.getJointOperatorElement(
        mockScanPredicate,
        mockPoint
      ),
      service.getJointTooltipElement(
        mockScanPredicate,
        mockPoint
      )
    ]);

    graph.addCell([
      service.getJointOperatorElement(
        mockResultPredicate,
        { x: 500, y: 100 }
      ),
      service.getJointOperatorElement(
        mockResultPredicate,
        { x: 500, y: 100 }
      )
      ]);

    const link = JointUIService.getJointLinkCell({
      linkID: 'link-1',
      source: { operatorID: 'operator1', portID: 'out0' },
      target: { operatorID: 'operator2', portID: 'in0' }
    });

    graph.addCell(link);

    const graph_operator1 = graph.getCell(mockScanPredicate.operatorID);
    const graph_operator2 = graph.getCell(mockResultPredicate.operatorID);
    const graph_link = graph.getLinks()[0];
    const graph_tooltip1 = graph.getCell('tooltip-' + mockScanPredicate.operatorID);

    // testing getCustomOperatorStyleAttrs()
    expect(graph_tooltip1.attr('polygon')).toEqual({
      fill: '#FFFFFF', 'follow-scale': true, stroke: 'purple', 'stroke-width': '2',
        rx: '5px', ry: '5px', refPoints: '0,30 150,30 150,120 85,120 75,150 65,120 0,120',
        display: 'none'
    });
    expect(graph_tooltip1.attr('#operatorCount')).toEqual({
      fill: '#595959', 'font-size': '12px', ref: 'polygon',
      'y-alignment': 'middle',
      'x-alignment': 'left',
      'ref-x': .05, 'ref-y': .2,
      display: 'none'
    });
    expect(graph_tooltip1.attr('#operatorSpeed')).toEqual({
      fill: '#595959',
      ref: 'polygon',
      'x-alignment': 'left',
      'font-size': '12px',
      'ref-x': .05, 'ref-y': .5,
      display: 'none'
    });
    expect(graph_operator1.attr('#operatorStates')).toEqual({
      text:  'Ready' , fill: 'green', 'font-size': '14px', 'visible' : false,
      'ref-x': 0.5, 'ref-y': -10, ref: 'rect', 'y-alignment': 'middle', 'x-alignment': 'middle'
    });
    expect(graph_operator1.attr('rect')).toEqual(
      { fill: '#FFFFFF', 'follow-scale': true, stroke: 'red', 'stroke-width': '2',
      rx: '5px', ry: '5px' }
    );
    expect(graph_operator2.attr('rect')).toEqual(
      { fill: '#FFFFFF', 'follow-scale': true, stroke: 'red', 'stroke-width': '2',
      rx: '5px', ry: '5px' }
    );
    expect(graph_operator1.attr('.delete-button')).toEqual(
      {
        x: 60, y: -20, cursor: 'pointer',
        fill: '#D8656A', event: 'element:delete'
      }
    );
    expect(graph_operator2.attr('.delete-button')).toEqual(
      {
        x: 60, y: -20, cursor: 'pointer',
        fill: '#D8656A', event: 'element:delete'
      }
    );

    // testing getDefaultLinkElement()
    expect(graph_link.attr('.marker-source/d')).toEqual(sourceOperatorHandle);
    expect(graph_link.attr('.marker-target/d')).toEqual(targetOperatorHandle);
    expect(graph_link.attr('.tool-remove path/d')).toEqual(deleteButtonPath);
  });
});
