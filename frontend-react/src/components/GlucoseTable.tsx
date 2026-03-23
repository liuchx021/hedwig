import React, { useMemo } from 'react';
import { Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { GlucoseReading } from '../types';
import { formatShanghaiDatetime, trendArrow, glucoseColor, glucoseClass } from '../utils/time';

interface Props {
  readings: GlucoseReading[];
}

const GlucoseTable: React.FC<Props> = ({ readings }) => {
  const sorted = useMemo(() => {
    return [...readings].sort((a, b) => b.readingTime.localeCompare(a.readingTime));
  }, [readings]);

  const columns: ColumnsType<GlucoseReading> = [
    {
      title: '\u65f6\u95f4',
      dataIndex: 'readingTime',
      key: 'readingTime',
      render: (val: string) => formatShanghaiDatetime(val),
      width: 180,
    },
    {
      title: '\u8840\u7cd6\u503c\uff08mmol/L\uff09',
      dataIndex: 'glucoseMmol',
      key: 'glucoseMmol',
      render: (val: number) => (
        <span style={{ color: glucoseColor(val), fontWeight: 600 }}>{val.toFixed(1)}</span>
      ),
      width: 160,
    },
    {
      title: '\u53d8\u5316\u8d8b\u52bf',
      dataIndex: 'trendDirection',
      key: 'trendDirection',
      render: (val: string | undefined) => {
        const arrow = trendArrow(val);
        const isUp = val === 'DOUBLE_UP' || val === 'SINGLE_UP' || val === 'FORTY_FIVE_UP';
        const isDown = val === 'DOUBLE_DOWN' || val === 'SINGLE_DOWN' || val === 'FORTY_FIVE_DOWN';
        const color = isUp ? '#e05c5c' : isDown ? '#e0a84b' : undefined;
        return <span style={{ fontSize: 16, color }}>{arrow}</span>;
      },
      width: 100,
    },
    {
      title: '\u5df2\u63a8\u9001',
      dataIndex: 'pushedToNightscout',
      key: 'pushedToNightscout',
      render: (val: boolean | undefined) =>
        val ? (
          <Tag color="success">{'\u5df2\u63a8\u9001'}</Tag>
        ) : (
          <Tag>{'\u672a\u63a8\u9001'}</Tag>
        ),
      width: 100,
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={sorted}
      rowKey={(r) => `${r.readingTime}-${r.glucoseMmol}`}
      size="small"
      pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (total) => `\u5171 ${total} \u6761\u8bb0\u5f55` }}
      rowClassName={(record) => {
        const cls = glucoseClass(record.glucoseMmol);
        if (cls === 'low') return 'glucose-row-low';
        if (cls === 'high') return 'glucose-row-high';
        return '';
      }}
    />
  );
};

export default GlucoseTable;
