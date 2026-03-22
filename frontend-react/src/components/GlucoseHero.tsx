import React, { useState, useEffect, useCallback } from 'react';
import { Skeleton, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { getLatest } from '../api/glucose';
import { glucoseColor, glucoseClass, trendArrow, parseTimestampMs } from '../utils/time';
import type { GlucoseReading } from '../types';

const { Text } = Typography;

interface GlucoseHeroProps {
  subjectId: number;
  subjectName: string;
  vendorType: string;
}

const statusBg: Record<string, { bg: string; border: string; glow: string }> = {
  normal: {
    bg: 'rgba(78, 205, 196, 0.06)',
    border: 'rgba(78, 205, 196, 0.18)',
    glow: 'rgba(78, 205, 196, 0.08)',
  },
  high: {
    bg: 'rgba(224, 92, 92, 0.06)',
    border: 'rgba(224, 92, 92, 0.18)',
    glow: 'rgba(224, 92, 92, 0.08)',
  },
  low: {
    bg: 'rgba(224, 168, 75, 0.06)',
    border: 'rgba(224, 168, 75, 0.18)',
    glow: 'rgba(224, 168, 75, 0.08)',
  },
};

const GlucoseHero: React.FC<GlucoseHeroProps> = ({ subjectId, subjectName }) => {
  const navigate = useNavigate();
  const [reading, setReading] = useState<GlucoseReading | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchLatest = useCallback(async () => {
    const data = await getLatest(subjectId);
    setReading(data);
    setLoading(false);
  }, [subjectId]);

  useEffect(() => {
    fetchLatest();
    const interval = setInterval(fetchLatest, 60_000);
    return () => clearInterval(interval);
  }, [fetchLatest]);

  const relativeTime = (): string => {
    if (!reading) return '';
    const ms = parseTimestampMs(reading.readingTime);
    if (ms === null) return '';
    const diffMin = Math.floor((Date.now() - ms) / 60_000);
    if (diffMin < 1) return '刚刚';
    if (diffMin < 60) return `${diffMin} 分钟前`;
    const hours = Math.floor(diffMin / 60);
    return `${hours} 小时前`;
  };

  const cls = reading ? glucoseClass(reading.glucoseMmol) : 'normal';
  const theme = statusBg[cls];

  if (loading) {
    return (
      <div
        className="dashboard-hero-card"
        style={{
          background: statusBg.normal.bg,
          border: `1px solid ${statusBg.normal.border}`,
        }}
      >
        <Skeleton active paragraph={{ rows: 1 }} title={{ width: 120 }} />
      </div>
    );
  }

  if (!reading) {
    return (
      <div
        className="dashboard-hero-card"
        style={{
          background: statusBg.normal.bg,
          border: `1px solid ${statusBg.normal.border}`,
          cursor: 'pointer',
        }}
        onClick={() => navigate(`/glucose/${subjectId}`)}
      >
        <Text type="secondary" style={{ fontSize: 15 }}>{subjectName} - 暂无数据</Text>
      </div>
    );
  }

  const color = glucoseColor(reading.glucoseMmol);

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: 'easeOut' }}
      className="dashboard-hero-card"
      style={{
        background: theme.bg,
        border: `1px solid ${theme.border}`,
        borderLeft: `4px solid ${color}`,
        boxShadow: `0 0 40px ${theme.glow}, 0 2px 8px rgba(0,0,0,0.1)`,
        cursor: 'pointer',
      }}
      onClick={() => navigate(`/glucose/${subjectId}`)}
    >
      {/* Main value row */}
      <div className="dashboard-hero-value-row">
        <span className="dashboard-hero-number" style={{ color }}>
          {reading.glucoseMmol.toFixed(1)}
        </span>
        <span className="dashboard-hero-trend" style={{ color }}>
          {trendArrow(reading.trendDirection)}
        </span>
        <span className="dashboard-hero-unit">mmol/L</span>
      </div>

      {/* Subject info row */}
      <div className="dashboard-hero-meta">
        <span className="dashboard-hero-dot" style={{ background: color }} />
        {subjectName}
        <span className="dashboard-hero-sep">·</span>
        {relativeTime()}
      </div>
    </motion.div>
  );
};

export default GlucoseHero;
