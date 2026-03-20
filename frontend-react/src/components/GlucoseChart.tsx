import React, { useMemo } from 'react';
import { Line } from '@ant-design/charts';
import { Empty } from 'antd';
import { GlucoseReading } from '../types';
import { parseTimestampMs } from '../utils/time';
import { useThemeStore } from '../store/themeStore';

interface Props {
  readings: GlucoseReading[];
}

const GlucoseChart: React.FC<Props> = ({ readings }) => {
  const { isDark } = useThemeStore();

  const data = useMemo(() => {
    return readings
      .map((r) => {
        const ms = parseTimestampMs(r.readingTime);
        return ms !== null ? { time: ms, value: r.glucoseMmol } : null;
      })
      .filter(Boolean)
      .sort((a, b) => a!.time - b!.time) as {
      time: number;
      value: number;
    }[];
  }, [readings]);

  if (data.length === 0) {
    return <Empty description={'\u6682\u65e0\u6570\u636e'} />;
  }

  const config = {
    data,
    xField: 'time',
    yField: 'value',
    shapeField: 'smooth',
    height: 320,
    autoFit: true,
    scale: {
      x: { type: 'time' as const },
      y: { domainMin: 2, domainMax: 20 },
    },
    axis: {
      x: {
        labelFormatter: (v: string | number | Date) => {
          const d = new Date(v);
          return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
        },
      },
      y: {},
    },
    style: {
      stroke: '#c8a44e',
      lineWidth: 2,
    },
    tooltip: {
      title: (d: { time: number }) => new Date(d.time).toLocaleString('zh-CN'),
      items: [
        {
          channel: 'y',
          name: '\u8840\u7cd6',
          valueFormatter: (v: number) => `${v.toFixed(1)} mmol/L`,
        },
      ],
    },
    annotations: [
      {
        type: 'lineY',
        yField: 3.9,
        style: { stroke: '#e0a84b', strokeOpacity: 0.5, lineDash: [4, 4] },
      },
      {
        type: 'lineY',
        yField: 10.0,
        style: { stroke: '#e05c5c', strokeOpacity: 0.5, lineDash: [4, 4] },
      },
    ],
    theme: isDark ? 'classicDark' : 'classic',
  };

  return <Line {...(config as any)} />;
};

export default GlucoseChart;
