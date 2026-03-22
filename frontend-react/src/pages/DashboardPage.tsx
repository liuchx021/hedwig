import React, { useCallback, useEffect, useState } from 'react';
import { Button, Skeleton, Empty, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { getConnections, deleteConnection } from '../api/vendors';
import { VendorConnection } from '../types';
import VendorCard from '../components/VendorCard';
import AlertBanner from '../components/AlertBanner';
import GlucoseHero from '../components/GlucoseHero';

function getGreeting(): string {
  const h = new Date().getHours();
  if (h < 6) return '夜深了';
  if (h < 12) return '早上好';
  if (h < 18) return '下午好';
  return '晚上好';
}

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [connections, setConnections] = useState<VendorConnection[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchConnections = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getConnections();
      setConnections(data);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConnections();
  }, [fetchConnections]);

  const handleDelete = async (id: number) => {
    try {
      await deleteConnection(id);
      message.success('已断开连接');
      fetchConnections();
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const heroConnections = connections.filter((c) => c.primarySubjectId != null);

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.4 }}
      className="dashboard-page"
    >
      {/* ── Page header ── */}
      <motion.div
        className="dashboard-header"
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.05 }}
      >
        <div>
          <h2 className="dashboard-greeting">{getGreeting()}</h2>
          <p className="dashboard-greeting-sub">血糖监测一览</p>
        </div>
        <Button
          type="default"
          icon={<PlusOutlined />}
          onClick={() => navigate('/connect')}
          className="dashboard-add-btn"
        >
          添加设备
        </Button>
      </motion.div>

      {/* ── Loading skeleton ── */}
      {loading && (
        <div className="dashboard-skeleton-wrap">
          <div className="dashboard-panel">
            <Skeleton active paragraph={{ rows: 2 }} title={{ width: 160 }} />
          </div>
          <div className="dashboard-panel">
            <Skeleton active paragraph={{ rows: 4 }} title={{ width: 100 }} />
          </div>
        </div>
      )}

      {/* ── Error state ── */}
      {!loading && error && (
        <div className="dashboard-panel" style={{ textAlign: 'center', padding: 48 }}>
          <Empty
            description={`加载失败：${error}`}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        </div>
      )}

      {/* ── Empty state ── */}
      {!loading && !error && connections.length === 0 && (
        <motion.div
          initial={{ opacity: 0, scale: 0.96 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4 }}
          className="dashboard-panel dashboard-empty-panel"
        >
          <div className="dashboard-empty-owl">🦉</div>
          <h3 className="dashboard-empty-title">还没有连接任何设备</h3>
          <p className="dashboard-empty-desc">连接您的 CGM 设备，开始实时监测血糖数据</p>
          <Button
            type="primary"
            size="large"
            icon={<PlusOutlined />}
            onClick={() => navigate('/connect')}
          >
            添加第一个设备
          </Button>
        </motion.div>
      )}

      {/* ── Main content ── */}
      {!loading && !error && connections.length > 0 && (
        <>
          {/* Glucose hero cards */}
          {heroConnections.length > 0 && (
            <motion.section
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.1 }}
              className="dashboard-section"
            >
              <div className="dashboard-hero-grid">
                {heroConnections.map((c) => (
                  <GlucoseHero
                    key={c.id}
                    subjectId={c.primarySubjectId!}
                    subjectName={c.primarySubjectName ?? `对象 ${c.primarySubjectId}`}
                    vendorType={c.vendorType}
                  />
                ))}
              </div>
            </motion.section>
          )}

          {/* Alert banner */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.3, delay: 0.2 }}
          >
            <AlertBanner connections={connections} />
          </motion.div>

          {/* Device connection panel */}
          <motion.section
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.25 }}
            className="dashboard-section"
          >
            <div className="dashboard-panel">
              <h4 className="dashboard-panel-title">设备连接</h4>
              <div className="dashboard-device-list">
                {connections.map((conn, idx) => (
                  <VendorCard
                    key={conn.id}
                    connection={conn}
                    onDelete={handleDelete}
                    index={idx}
                  />
                ))}
              </div>
            </div>
          </motion.section>
        </>
      )}
    </motion.div>
  );
};

export default DashboardPage;
