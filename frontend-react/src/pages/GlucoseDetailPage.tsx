import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Typography,
  Space,
  Select,
  Button,
  Card,
  Row,
  Col,
  Statistic,
  Skeleton,
  Alert,
  Empty,
  message,
} from 'antd';
import { ArrowLeftOutlined, SyncOutlined, ReloadOutlined } from '@ant-design/icons';
import { Link, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { getHistory, syncHistory } from '../api/glucose';
import { GlucoseReading } from '../types';
import { parseTimestampMs, glucoseColor, glucoseClass } from '../utils/time';
import GlucoseChart from '../components/GlucoseChart';
import GlucoseTable from '../components/GlucoseTable';

const { Title, Paragraph, Text } = Typography;

const HOURS_OPTIONS = [
  { label: '\u6700\u8fd16\u5c0f\u65f6', value: 6 },
  { label: '\u6700\u8fd112\u5c0f\u65f6', value: 12 },
  { label: '\u6700\u8fd124\u5c0f\u65f6', value: 24 },
  { label: '\u6700\u8fd148\u5c0f\u65f6', value: 48 },
  { label: '\u6700\u8fd172\u5c0f\u65f6', value: 72 },
];

const GlucoseDetailPage: React.FC = () => {
  const { id: subjectId } = useParams<{ id: string }>();
  const [hoursBack, setHoursBack] = useState(24);
  const [readings, setReadings] = useState<GlucoseReading[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [syncing, setSyncing] = useState(false);
  const [syncMsg, setSyncMsg] = useState<string | null>(null);

  const fetchData = useCallback(
    async (hours: number) => {
      if (!subjectId) return;
      setLoading(true);
      setError(null);
      try {
        const data = await getHistory(subjectId);
        const cutoff = Date.now() - hours * 3600 * 1000;
        const filtered = data.filter((r) => {
          const ms = parseTimestampMs(r.readingTime);
          return ms !== null && ms >= cutoff;
        });
        setReadings(filtered);
      } catch (e: any) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    },
    [subjectId],
  );

  useEffect(() => {
    fetchData(hoursBack);
  }, [fetchData, hoursBack]);

  const handleSync = async () => {
    if (!subjectId) return;
    setSyncing(true);
    setSyncMsg(null);
    setError(null);
    try {
      const result = await syncHistory(subjectId);
      setSyncMsg(`\u540c\u6b65\u6210\u529f\uff1a\u5df2\u540c\u6b65 ${result.syncedCount} \u6761\u6570\u636e`);
      fetchData(hoursBack);
    } catch (e: any) {
      setError(`\u540c\u6b65\u5931\u8d25\uff1a${e.message}`);
    } finally {
      setSyncing(false);
    }
  };

  const stats = useMemo(() => {
    const count = readings.length;
    const avg =
      count > 0 ? readings.reduce((sum, r) => sum + r.glucoseMmol, 0) / count : undefined;
    const latest = readings.reduce<GlucoseReading | null>((best, r) => {
      if (!best || r.readingTime > best.readingTime) return r;
      return best;
    }, null);
    return { count, avg, latest };
  }, [readings]);

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }}>
      <Space direction="vertical" size={20} style={{ width: '100%' }}>
        <Link to="/">
          <Button type="link" icon={<ArrowLeftOutlined />} style={{ padding: 0 }}>
            {'\u8fd4\u56de\u4eea\u8868\u76d8'}
          </Button>
        </Link>

        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 12,
          }}
        >
          <div>
            <Title level={3} style={{ margin: 0 }}>
              {'\u8840\u7cd6\u8be6\u60c5 \u2014 '}{subjectId}
            </Title>
            <Paragraph type="secondary" style={{ margin: 0 }}>
              {'\u67e5\u770b\u5386\u53f2\u8840\u7cd6\u503c\u53d8\u5316\u8d8b\u52bf'}
            </Paragraph>
          </div>
          <Space>
            <Text>{'\u65f6\u95f4\u8303\u56f4\uff1a'}</Text>
            <Select
              value={hoursBack}
              options={HOURS_OPTIONS}
              onChange={setHoursBack}
              style={{ width: 140 }}
            />
            <Button icon={<ReloadOutlined />} onClick={() => fetchData(hoursBack)}>
              {'\u5237\u65b0'}
            </Button>
            <Button icon={<SyncOutlined />} loading={syncing} onClick={handleSync}>
              {syncing ? '\u540c\u6b65\u4e2d...' : '\u540c\u6b65\u6700\u8fd1\u8840\u7cd6\u6570\u636e'}
            </Button>
          </Space>
        </div>

        {syncMsg && <Alert type="success" message={syncMsg} showIcon closable onClose={() => setSyncMsg(null)} />}

        {loading ? (
          <Skeleton active paragraph={{ rows: 8 }} />
        ) : error ? (
          <Alert type="error" message={`\u52a0\u8f7d\u5931\u8d25\uff1a${error}`} showIcon />
        ) : (
          <>
            {/* Summary stats */}
            <Row gutter={16}>
              <Col xs={24} sm={8}>
                <Card>
                  <Statistic
                    title={'\u8bfb\u6570\u6761\u6570'}
                    value={stats.count}
                    valueStyle={{ color: '#c8a44e' }}
                  />
                </Card>
              </Col>
              {stats.avg !== undefined && (
                <Col xs={24} sm={8}>
                  <Card style={{ background: 'rgba(78, 205, 196, 0.06)' }}>
                    <Statistic
                      title={'\u5e73\u5747\u8840\u7cd6\u503c'}
                      value={stats.avg}
                      precision={1}
                      suffix="mmol/L"
                      valueStyle={{ color: '#4ecdc4' }}
                    />
                  </Card>
                </Col>
              )}
              {stats.latest && (
                <Col xs={24} sm={8}>
                  <Card style={{ background: `rgba(${glucoseClass(stats.latest.glucoseMmol) === 'normal' ? '78, 205, 196' : glucoseClass(stats.latest.glucoseMmol) === 'high' ? '224, 92, 92' : '224, 168, 75'}, 0.06)` }}>
                    <Statistic
                      title={'\u6700\u65b0\u8840\u7cd6\u503c'}
                      value={stats.latest.glucoseMmol}
                      precision={1}
                      suffix="mmol/L"
                      valueStyle={{ color: glucoseColor(stats.latest.glucoseMmol) }}
                    />
                  </Card>
                </Col>
              )}
            </Row>

            {/* Chart */}
            <Card title={'\u8840\u7cd6\u8d8b\u52bf\u56fe'}>
              <GlucoseChart readings={readings} />
            </Card>

            {/* Table */}
            <Card
              title={'\u6570\u636e\u660e\u7ec6'}
              extra={
                <Text type="secondary" style={{ fontSize: 13 }}>
                  {`\u5171 ${stats.count} \u6761\u8bb0\u5f55`}
                </Text>
              }
            >
              <GlucoseTable readings={readings} />
            </Card>
          </>
        )}
      </Space>
    </motion.div>
  );
};

export default GlucoseDetailPage;
