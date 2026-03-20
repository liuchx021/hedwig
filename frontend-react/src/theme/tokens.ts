import { theme } from 'antd';
import type { ThemeConfig } from 'antd';

const primaryColor = '#c8a44e';
const accentColor = '#4ecdc4';
const fontFamily = "'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif";

const sharedComponents: ThemeConfig['components'] = {
  Card: {
    boxShadowTertiary: '0 1px 4px rgba(0,0,0,0.08)',
  },
  Button: {
    borderRadius: 6,
    fontWeight: 500,
  },
  Input: {
    borderRadius: 6,
  },
  Menu: {
    itemBorderRadius: 6,
  },
};

export const darkTheme: ThemeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    colorPrimary: primaryColor,
    colorBgContainer: '#141a23',
    colorBgElevated: '#1a2230',
    colorBgLayout: '#0c1117',
    colorText: '#e8e6e1',
    colorTextSecondary: '#7a8599',
    borderRadius: 8,
    colorLink: accentColor,
    colorSuccess: accentColor,
    colorError: '#e05c5c',
    colorWarning: '#e0a84b',
    fontFamily,
  },
  components: {
    ...sharedComponents,
    Table: {
      colorBgContainer: '#141a23',
      headerBg: '#1a2230',
      rowHoverBg: '#222d3d',
    },
    Card: {
      ...sharedComponents.Card,
      colorBgContainer: '#141a23',
    },
  },
};

export const lightTheme: ThemeConfig = {
  algorithm: theme.defaultAlgorithm,
  token: {
    colorPrimary: primaryColor,
    borderRadius: 8,
    colorLink: accentColor,
    colorSuccess: accentColor,
    colorError: '#e05c5c',
    colorWarning: '#e0a84b',
    fontFamily,
  },
  components: {
    ...sharedComponents,
    Table: {
      headerBg: '#fafafa',
      rowHoverBg: '#f5f5f5',
    },
  },
};
