import { describe, it, expect } from 'vitest'
import { dtoToFlow, flowToSaveRequest } from '../useWorkflowSync'
import type { WorkflowDTO } from '../../types/workflow'

const sampleWorkflow: WorkflowDTO = {
  id: 'wf-1',
  name: '範例流程',
  description: '測試用',
  status: 'DRAFT',
  version: 3,
  canvasMeta: { zoom: 1 },
  nodes: [
    {
      nodeKey: 'a1b2c3d4',
      type: 'TRIGGER',
      name: '開始',
      positionX: 100,
      positionY: 200,
      config: { schedule: 'cron', detail: { every: 5, unit: 'min' } },
    },
    {
      nodeKey: 'e5f6g7h8',
      type: 'LLM_ASSISTANT',
      name: '助手',
      positionX: 400,
      positionY: 250,
      config: {},
    },
  ],
  edges: [
    {
      sourceNodeKey: 'a1b2c3d4',
      targetNodeKey: 'e5f6g7h8',
      sourceHandle: 'out',
      targetHandle: 'in',
      label: '下一步',
    },
  ],
}

describe('dtoToFlow', () => {
  it('node.id 對應 nodeKey、position 對應 positionX/Y、data 含 name/type/config', () => {
    const { nodes } = dtoToFlow(sampleWorkflow)
    expect(nodes).toHaveLength(2)

    const trigger = nodes[0]
    expect(trigger.id).toBe('a1b2c3d4')
    expect(trigger.type).toBe('workflow')
    expect(trigger.position).toEqual({ x: 100, y: 200 })
    expect(trigger.data.name).toBe('開始')
    expect(trigger.data.type).toBe('TRIGGER')
    expect(trigger.data.config).toEqual({ schedule: 'cron', detail: { every: 5, unit: 'min' } })
  })

  it('edge.source/target 對應 sourceNodeKey/targetNodeKey，保留 handle 與 label', () => {
    const { edges } = dtoToFlow(sampleWorkflow)
    expect(edges).toHaveLength(1)

    const edge = edges[0]
    expect(edge.source).toBe('a1b2c3d4')
    expect(edge.target).toBe('e5f6g7h8')
    expect(edge.sourceHandle).toBe('out')
    expect(edge.targetHandle).toBe('in')
    expect(edge.label).toBe('下一步')
    expect(edge.id).toBeTruthy()
  })
})

describe('edge id 帶 handle（避免多分支撞 id）', () => {
  it('同一對節點的不同 handle 分支產生不同 edge id', () => {
    const branching: WorkflowDTO = {
      name: '分支',
      nodes: [
        { nodeKey: 'c1', type: 'CONDITION', positionX: 0, positionY: 0, config: {} },
        { nodeKey: 't1', type: 'TOOL', positionX: 0, positionY: 0, config: {} },
      ],
      edges: [
        { sourceNodeKey: 'c1', targetNodeKey: 't1', sourceHandle: 'out:true', targetHandle: 'in:main' },
        { sourceNodeKey: 'c1', targetNodeKey: 't1', sourceHandle: 'out:false', targetHandle: 'in:main' },
      ],
    }
    const { edges } = dtoToFlow(branching)
    expect(edges[0].id).not.toBe(edges[1].id)
    expect(edges[0].id).toContain('out:true')
    expect(edges[1].id).toContain('out:false')
  })

  it('handle 為空時 edge id 以 main 佔位', () => {
    const wf: WorkflowDTO = {
      name: 'x',
      nodes: [
        { nodeKey: 'a', type: 'TRIGGER', positionX: 0, positionY: 0, config: {} },
        { nodeKey: 'b', type: 'TOOL', positionX: 0, positionY: 0, config: {} },
      ],
      edges: [{ sourceNodeKey: 'a', targetNodeKey: 'b' }],
    }
    const { edges } = dtoToFlow(wf)
    expect(edges[0].id).toBe('e-a:main-b:main')
  })
})

describe('flowToSaveRequest', () => {
  it('反向對應 node/edge 並帶入 meta', () => {
    const { nodes, edges } = dtoToFlow(sampleWorkflow)
    const req = flowToSaveRequest(nodes, edges, {
      id: 'wf-1',
      version: 3,
      name: '範例流程',
      description: '測試用',
      canvasMeta: { zoom: 1 },
    })

    expect(req.id).toBe('wf-1')
    expect(req.version).toBe(3)
    expect(req.name).toBe('範例流程')
    expect(req.nodes[0].nodeKey).toBe('a1b2c3d4')
    expect(req.nodes[0].positionX).toBe(100)
    expect(req.nodes[0].positionY).toBe(200)
    expect(req.edges[0].sourceNodeKey).toBe('a1b2c3d4')
    expect(req.edges[0].targetNodeKey).toBe('e5f6g7h8')
  })

  it('config 物件 round-trip 不失真（含巢狀）', () => {
    const { nodes, edges } = dtoToFlow(sampleWorkflow)
    const req = flowToSaveRequest(nodes, edges, { name: '範例流程' })
    expect(req.nodes[0].config).toEqual({
      schedule: 'cron',
      detail: { every: 5, unit: 'min' },
    })
  })
})

describe('round-trip', () => {
  it('flowToSaveRequest(dtoToFlow(wf)) 的 nodes/edges 與原 wf 等價', () => {
    const { nodes, edges } = dtoToFlow(sampleWorkflow)
    const req = flowToSaveRequest(nodes, edges, {
      id: sampleWorkflow.id,
      version: sampleWorkflow.version,
      name: sampleWorkflow.name,
      description: sampleWorkflow.description,
      canvasMeta: sampleWorkflow.canvasMeta,
    })

    expect(req.nodes).toEqual(sampleWorkflow.nodes)
    expect(req.edges).toEqual(sampleWorkflow.edges)
  })
})
