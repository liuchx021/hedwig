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

const GlucoseHero: React.FC<GlucoseHeroProps> = ({ subjectId, subjectName, vendorType }) => {
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
    if (diffMin < 1) return '\u521a\u521a';
    if (diffMin < 60) return `${diffMin} \u5206\u949f\u524d`;
    const hours = Math.floor(diffMin / 60);
    return `${hours} \u5c0f\u65f6\u524d`;
  };

  const cls = reading ? glucoseClass(reading.glucoseMmol) : 'normal';

  if (loading) {
    return (
      <div className="glucose-hero glucose-hero--normal">
        <Skeleton active paragraph={{ rows: 2 }} />
      </div>
    );
  }

  if (!reading) {
    return (
      <div
        className="glucose-hero glucose-hero--normal"
        style={{ cursor: 'pointer' }}
        onClick={() => navigate(`/glucose/${subjectId}`)}
      >
        <Text type="secondary">{subjectName} - \u6682\u65e0\u6570\u636e</Text>
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.4 }}
      className={`glucose-hero glucose-hero--${cls}`}
      style={{ cursor: 'pointer' }}
      onClick={() => navigate(`/glucose/${subjectId}`)}
    >
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
        <span
          className="glucose-value"
          style={{
            fontFamily: 'var(--font-mono)',
            color: glucoseColor(reading.glucoseMmol),
          }}
        >
          {reading.glucoseMmol.toFixed(1)}
        </span>
        <span className="glucose-unit">mmol/L</span>
        <span
          className="glucose-trend"
          style={{ color: glucoseColor(reading.glucoseMmol) }}
        >
          {trendArrow(reading.trendDirection)}
        </span>
      </div>
      <div style={{ marginTop: 4 }}>
        <Text type="secondary">{subjectName}</Text>
        <Text type="secondary" style={{ marginLeft: 12 }}>
          {'\u6700\u540e\u66f4\u65b0 '}{relativeTime()}
        </Text>
      </div>
    </motion.div>
  );
};

export default GlucoseHero;
