import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    'intro',
    {
      type: 'category',
      label: '架構說明',
      items: [
        'architecture/overview',
        'architecture/modules',
        'architecture/tech-stack',
        'architecture/i18n-messages',
      ],
    },
    {
      type: 'category',
      label: '快速開始',
      items: [
        'getting-started/prerequisites',
        'getting-started/installation',
        'getting-started/first-run',
      ],
    },
    {
      type: 'category',
      label: '功能說明',
      items: [
        'features/ai-models',
        'features/rag',
        'features/tools',
        'features/mcp-servers',
        'features/workflow',
      ],
    },
    {
      type: 'category',
      label: 'API 文件',
      items: [
        'api/authentication',
        'api/llm-setting',
        'api/assistant',
        'api/knowledge-base',
        'api/skills',
        'api/workflow',
        'api/swagger',
      ],
    },
  ],
};

export default sidebars;
