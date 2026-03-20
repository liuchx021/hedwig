import React from 'react';
import { Card, Button, Space, Typography, Tag, Popconfirm, Tooltip } from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  StarFilled,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import type { NightscoutTarget } from '../types/nightscout';
import { formatShanghaiDatetime } from '../utils/time';

const { Text } = Typography;

interface Props {
  target: NightscoutTarget;
  onEdit: (target: NightscoutTarget) => void;
  onDelete: (id: number) => void;
  onToggle: (id: number) => void;
  index?: number;
}

const NightscoutTargetCard: React.FC<Props> = ({ target, onEdit, onDelete, onToggle, index = 0 }) => {
  const isActive = target.status === 'ACTIVE';

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.08, duration: 0.3 }}
    >
      <Card
        className={isActive ? 'vendor-card-active' : 'vendor-card-expired'}
        title={
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Space>
              {target.isDefault && (
                <Tooltip title="默认推送目标">
                  <StarFilled style={{ color: '#c8a44e' }} />
                </Tooltip>
              )}
              <Text strong>{target.name}</Text>
            </Space>
            <Tag color={isActive ? 'success' : 'default'}>
              {isActive ? '已启用' : '已停用'}
            </Tag>
          </Space>
        }
        hoverable
        style={{ height: '100%' }}
      >
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          <div>
            <Text type="secondary">{'地址：'}</Text>
            <Text strong style={{ wordBreak: 'break-all' }}>{target.baseUrl}</Text>
          </div>
          <div>
            <Text type="secondary">{'API 密钥：'}</Text>
            <Text code>{target.apiSecretHint || '****'}</Text>
          </div>
          {target.monitoredSubjectName && (
            <div>
              <Text type="secondary">{'监测对象：'}</Text>
              <Text strong>{target.monitoredSubjectName}</Text>
            </div>
          )}
          {!target.monitoredSubjectId && (
            <div>
              <Text type="secondary">{'监测对象：'}</Text>
              <Text>全部</Text>
            </div>
          )}

          {/* 推送状态 */}
          <div>
            <Text type="secondary">{'最近推送：'}</Text>
            {target.lastPushAt ? (
              <Space size={4}>
                <Text>{formatShanghaiDatetime(target.lastPushAt)}</Text>
                {target.lastSuccessAt && target.lastSuccessAt === target.lastPushAt ? (
                  <CheckCircleOutlined style={{ color: '#4ecdc4' }} />
                ) : target.lastErrorMessage ? (
                  <Tooltip title={target.lastErrorMessage}>
                    <CloseCircleOutlined style={{ color: '#e05c5c' }} />
                  </Tooltip>
                ) : null}
              </Space>
            ) : (
              <Text type="secondary">{'从未推送'}</Text>
            )}
          </div>

          {target.lastErrorMessage && (
            <div>
              <Text type="danger" style={{ fontSize: 12 }}>
                {'错误：'}{target.lastErrorMessage.length > 80
                  ? target.lastErrorMessage.substring(0, 80) + '...'
                  : target.lastErrorMessage}
              </Text>
            </div>
          )}

          <Space style={{ width: '100%', marginTop: 8 }} wrap>
            <Button
              icon={isActive ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
              onClick={() => onToggle(target.id)}
            >
              {isActive ? '停用' : '启用'}
            </Button>
            <Button icon={<EditOutlined />} onClick={() => onEdit(target)}>
              {'编辑'}
            </Button>
            <Popconfirm
              title={'确认删除此 Nightscout 目标？'}
              description={'删除后相关推送记录将不可恢复'}
              onConfirm={() => onDelete(target.id)}
              okText={'确认'}
              cancelText={'取消'}
            >
              <Button danger icon={<DeleteOutlined />}>
                {'删除'}
              </Button>
            </Popconfirm>
          </Space>
        </Space>
      </Card>
    </motion.div>
  );
};

export default NightscoutTargetCard;
