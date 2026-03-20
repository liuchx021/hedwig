import React from 'react';
import { Card, Button, Space, Typography, Popconfirm } from 'antd';
import { DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { VendorConnection, VendorType, TokenStatus } from '../types';
import TokenStatusBadge from './TokenStatusBadge';
import { formatShanghaiDatetime, formatRemainingTime, parseTimestampMs } from '../utils/time';

const { Text } = Typography;

interface Props {
  connection: VendorConnection;
  onDelete: (id: number) => void;
  index?: number;
}

function vendorDisplayName(vt: VendorType): string {
  switch (vt) {
    case VendorType.OTTAI:
      return '\u6b27\u6cf0\uff08Ottai\uff09';
    case VendorType.SISENSING:
      return '\u7845\u57fa\u8f7b\u4eab\uff08SiSensing\uff09';
    default:
      return String(vt);
  }
}

const VendorCard: React.FC<Props> = ({ connection, onDelete, index = 0 }) => {
  const navigate = useNavigate();
  const c = connection;

  const sensorRemaining = c.sensorExpiresAt ? formatRemainingTime(c.sensorExpiresAt) : null;

  const sensorProgressPct = (() => {
    if (!c.sensorExpiresAt) return 0;
    const expiresMs = parseTimestampMs(c.sensorExpiresAt);
    if (!expiresMs) return 0;
    const totalMs = 14 * 24 * 3600 * 1000; // 14 days
    const remainingMs = expiresMs - Date.now();
    return Math.max(0, Math.min(100, (remainingMs / totalMs) * 100));
  })();

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.08, duration: 0.3 }}
    >
      <Card
        className={
          c.tokenStatus === TokenStatus.EXPIRED ? 'vendor-card-expired' :
          c.tokenStatus === TokenStatus.EXPIRING_SOON ? 'vendor-card-expiring' :
          'vendor-card-active'
        }
        title={
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Text strong>{vendorDisplayName(c.vendorType)}</Text>
            <TokenStatusBadge status={c.tokenStatus} />
          </Space>
        }
        hoverable
        style={{ height: '100%' }}
      >
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          <div>
            <Text type="secondary">{'\u5382\u5546\u8d26\u53f7\uff1a'}</Text>
            <Text strong>{c.vendorUserId ?? '\u672a\u77e5'}</Text>
          </div>
          <div>
            <Text type="secondary">{'\u76d1\u6d4b\u5bf9\u8c61\uff1a'}</Text>
            <Text strong>{c.primarySubjectName ?? '\u672a\u53d1\u73b0\u76d1\u6d4b\u5bf9\u8c61'}</Text>
          </div>
          {sensorRemaining && c.sensorExpiresAt && (
            <div>
              <Text type="secondary">{'\u4f20\u611f\u5668\u5269\u4f59\u6709\u6548\u671f\uff1a'}</Text>
              <Text strong style={{ color: sensorRemaining === '\u5df2\u8fc7\u671f' ? '#e05c5c' : '#4ecdc4' }}>
                {sensorRemaining}
              </Text>
              <div className="sensor-progress">
                <div
                  className="sensor-progress-bar"
                  style={{
                    width: `${sensorProgressPct}%`,
                    background: sensorRemaining === '\u5df2\u8fc7\u671f' ? '#e05c5c' : '#4ecdc4',
                  }}
                />
              </div>
            </div>
          )}
          <div>
            <Text type="secondary">
              {'\u6700\u540e\u540c\u6b65\uff1a'}
              {c.lastSyncedAt ? formatShanghaiDatetime(c.lastSyncedAt) : '\u4ece\u672a\u540c\u6b65'}
            </Text>
          </div>
          <Space style={{ width: '100%', marginTop: 8 }}>
            {c.primarySubjectId ? (
              <Button
                type="primary"
                icon={<EyeOutlined />}
                block
                onClick={() => navigate(`/glucose/${c.primarySubjectId}`)}
              >
                {'\u67e5\u770b\u8be6\u60c5'}
              </Button>
            ) : (
              <Button disabled block>
                {'\u6682\u65e0\u8be6\u60c5'}
              </Button>
            )}
            <Popconfirm
              title={'\u786e\u8ba4\u65ad\u5f00\u8fde\u63a5\uff1f'}
              onConfirm={() => onDelete(c.id)}
              okText={'\u786e\u8ba4'}
              cancelText={'\u53d6\u6d88'}
            >
              <Button danger icon={<DeleteOutlined />}>
                {'\u65ad\u5f00'}
              </Button>
            </Popconfirm>
          </Space>
        </Space>
      </Card>
    </motion.div>
  );
};

export default VendorCard;
