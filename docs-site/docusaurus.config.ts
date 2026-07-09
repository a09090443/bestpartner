import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
  title: 'BestPartner',
  tagline: 'AI 應用大平台 — 動態建立 AI Agent，支援多種 AI 模型',
  favicon: 'img/favicon.ico',

  future: {
    v4: true,
  },

  url: 'https://a09090443.github.io',
  baseUrl: '/bestpartner/',

  organizationName: 'a09090443',
  projectName: 'bestpartner',

  onBrokenLinks: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'throw',
      onBrokenMarkdownImages: 'warn',
    },
  },

  i18n: {
    defaultLocale: 'zh-Hant',
    locales: ['zh-Hant'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          routeBasePath: '/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  plugins: [
    [
      'docusaurus-plugin-llms',
      {
        // 產生 AI/LLM 友善的彙整檔與各頁純 Markdown
        docsDir: 'docs',
        generateLLMsTxt: true, // /llms.txt：站台索引與頁面清單
        generateLLMsFullTxt: true, // /llms-full.txt：全文彙整，供 LLM 一次讀取
        generateMarkdownFiles: true, // 每頁輸出對應的純 .md，供 AI agent 直接抓取
        title: 'BestPartner 文件',
        description:
          'BestPartner 是一個 AI 應用大平台，可動態建立 AI Agent 並支援多種 AI 模型，內建 RAG 知識庫、工具調用、MCP Server 與視覺化 Workflow。',
        includeOrder: [
          'intro.md',
          'getting-started/**',
          'architecture/**',
          'features/**',
          'api/**',
        ],
      },
    ],
  ],

  themeConfig: {
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'BestPartner',
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: '文件',
        },
        {
          href: 'https://github.com/a09090443/bestpartner',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: '文件',
          items: [
            { label: '快速開始', to: '/getting-started/prerequisites' },
            { label: '架構說明', to: '/architecture/overview' },
            { label: 'API 文件', to: '/api/authentication' },
          ],
        },
        {
          title: '資源',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/a09090443/bestpartner',
            },
            {
              label: 'Langchain4j 官網',
              href: 'https://docs.langchain4j.dev/',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} BestPartner. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['kotlin', 'java', 'bash', 'yaml', 'sql'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
