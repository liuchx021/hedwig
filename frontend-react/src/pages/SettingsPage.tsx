import React, { useCallback, useEffect, useState } from 'react';
import { Button, Typography, Row, Col, Skeleton, Empty, message, Card, Space, Alert } from 'antd';
import { PlusOutlined, CloudUploadOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import {
  listTargets,
  createTarget,
  updateTarget,
  deleteTarget,
  toggleTargetStatus,
  nightscoutSync,
} from '../api/nightscout';
import { getConnections } from '../api/vendors';
import type { NightscoutTarget, NightscoutTargetRequest } from '../types/nightscout';
import type { VendorConnection } from '../types';
import NightscoutTargetCard from '../components/NightscoutTargetCard';
import NightscoutTargetModal from '../components/NightscoutTargetModal';

const { Title, Paragraph } = Typography;

const SettingsPage: React.FC = () => {
  const [targets, setTargets] = useState<NightscoutTarget[]>([]);
  const [connections, setConnections] = useState<VendorConnection[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTarget, setEditingTarget] = useState<NightscoutTarget | null>(null);
  const [modalLoading, setModalLoading] = useState(false);

  // Sync state
  const [syncing, setSyncing] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [targetList, connList] = await Promise.all([
        listTargets(),
        getConnections(),
      ]);
      setTargets(targetList);
      setConnections(connList);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // 从 connections 中提取 subjects
  const subjects = connections
    .filter((c) => c.primarySubjectId != null)
    .map((c) => ({
      id: c.primarySubjectId!,
      name: c.primarySubjectName ?? `对象 ${c.primarySubjectId}`,
    }));

  // CRUD handlers
  const handleCreate = () => {
    setEditingTarget(null);
    setModalOpen(true);
  };

  const handleEdit = (target: NightscoutTarget) => {
    setEditingTarget(target);
    setModalOpen(true);
  };

  const handleModalOk = async (values: NightscoutTargetRequest) => {
    setModalLoading(true);
    try {
      if (editingTarget) {
        // 编辑：如果 apiSecret 为空则不传
        const payload: Partial<NightscoutTargetRequest> = { ...values };
        if (!payload.apiSecret) {
          delete payload.apiSecret;
        }
        await updateTarget(editingTarget.id, payload);
        message.success('更新成功');
      } else {
        await createTarget(values);
        message.success('创建成功');
      }
      setModalOpen(false);
      fetchData();
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setModalLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteTarget(id);
      message.success('已删除');
      fetchData();
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const handleToggle = async (id: number) => {
    try {
      const updated = await toggleTargetStatus(id);
      message.success(updated.status === 'ACTIVE' ? '已启用' : '已停用');
      fetchData();
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      const { count, message: msg } = await nightscoutSync();
      if (count > 0) {
        message.success(`${msg}，共上传 ${count} 条数据`);
      } else {
        message.info(msg);
      }
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setSyncing(false);
    }
  };

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }}>
      {/* 标题 + 操作栏 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 24,
          flexWrap: 'wrap',
          gap: 12,
        }}
      >
        <div>
          <Title level={3} style={{ margin: 0 }}>
            {'Nightscout配置'}
          </Title>
          <Paragraph type="secondary" style={{ margin: 0 }}>
            {'管理 Nightscout 数据推送目标与同步'}
          </Paragraph>
        </div>
        <Space>
          <Button
            icon={<CloudUploadOutlined />}
            loading={syncing}
            onClick={handleSync}
          >
            {syncing ? '同步中...' : '手动同步'}
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            {'添加目标'}
          </Button>
        </Space>
      </div>

      {/* 内容区 */}
      {loading ? (
        <Row gutter={[16, 16]}>
          {[1, 2].map((i) => (
            <Col xs={24} sm={12} lg={8} key={i}>
              <Skeleton active paragraph={{ rows: 5 }} />
            </Col>
          ))}
        </Row>
      ) : error ? (
        <Alert type="error" message={`加载失败：${error}`} showIcon />
      ) : targets.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.3 }}
          style={{ textAlign: 'center', padding: '60px 0' }}
        >
          <div style={{ fontSize: 64, marginBottom: 16 }}>{'🌙'}</div>
          <Paragraph type="secondary" style={{ fontSize: 16 }}>
            {'还没有配置 Nightscout 推送目标'}
          </Paragraph>
          <Paragraph type="secondary">
            {'添加一个目标后，血糖数据将自动推送到您的 Nightscout 服务'}
          </Paragraph>
          <Button type="primary" size="large" onClick={handleCreate}>
            {'添加第一个目标'}
          </Button>
        </motion.div>
      ) : (
        <Row gutter={[16, 16]}>
          {targets.map((target, idx) => (
            <Col xs={24} sm={12} lg={8} key={target.id}>
              <NightscoutTargetCard
                target={target}
                onEdit={handleEdit}
                onDelete={handleDelete}
                onToggle={handleToggle}
                index={idx}
              />
            </Col>
          ))}
        </Row>
      )}

      {/* 创建/编辑弹窗 */}
      <NightscoutTargetModal
        open={modalOpen}
        editingTarget={editingTarget}
        connections={connections}
        subjects={subjects}
        confirmLoading={modalLoading}
        onOk={handleModalOk}
        onCancel={() => setModalOpen(false)}
      />
    </motion.div>
  );
};

export default SettingsPage;
