import React, { useCallback, useEffect, useState } from 'react';
import { Button, Typography, Row, Col, Skeleton, Empty, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { getConnections, deleteConnection } from '../api/vendors';
import { VendorConnection } from '../types';
import VendorCard from '../components/VendorCard';
import AlertBanner from '../components/AlertBanner';
import GlucoseHero from '../components/GlucoseHero';

const { Title, Paragraph } = Typography;

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
      message.success('\u5df2\u65ad\u5f00\u8fde\u63a5');
      fetchConnections();
    } catch (e: any) {
      message.error(e.message);
    }
  };

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }}>
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
            {'\u4eea\u8868\u76d8'}
          </Title>
          <Paragraph type="secondary" style={{ margin: 0 }}>
            {'\u7ba1\u7406\u60a8\u7684\u8840\u7cd6\u76d1\u6d4b\u8bbe\u5907\u8fde\u63a5'}
          </Paragraph>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/connect')}>
          {'\u6dfb\u52a0\u8bbe\u5907'}
        </Button>
      </div>

      {/* Glucose Hero Section */}
      {!loading && connections.filter(c => c.primarySubjectId).length > 0 && (
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          {connections
            .filter((c) => c.primarySubjectId != null)
            .map((c) => (
              <Col xs={24} sm={12} key={c.id}>
                <GlucoseHero
                  subjectId={c.primarySubjectId!}
                  subjectName={c.primarySubjectName ?? `对象 ${c.primarySubjectId}`}
                  vendorType={c.vendorType}
                />
              </Col>
            ))}
        </Row>
      )}

      {!loading && connections.length > 0 && <AlertBanner connections={connections} />}

      {loading ? (
        <Row gutter={[16, 16]}>
          {[1, 2].map((i) => (
            <Col xs={24} sm={12} lg={8} key={i}>
              <Skeleton active paragraph={{ rows: 5 }} />
            </Col>
          ))}
        </Row>
      ) : error ? (
        <Empty
          description={`\u52a0\u8f7d\u5931\u8d25\uff1a${error}`}
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      ) : connections.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.3 }}
          style={{ textAlign: 'center', padding: '60px 0' }}
        >
          <div style={{ fontSize: 64, marginBottom: 16 }}>{'\uD83E\uDD89'}</div>
          <Paragraph type="secondary" style={{ fontSize: 16 }}>
            {'\u8fd8\u6ca1\u6709\u8fde\u63a5\u4efb\u4f55\u8bbe\u5907'}
          </Paragraph>
          <Button type="primary" size="large" onClick={() => navigate('/connect')}>
            {'\u6dfb\u52a0\u7b2c\u4e00\u4e2a\u8bbe\u5907'}
          </Button>
        </motion.div>
      ) : (
        <Row gutter={[16, 16]}>
          {connections.map((conn, idx) => (
            <Col xs={24} sm={12} lg={8} key={conn.id}>
              <VendorCard connection={conn} onDelete={handleDelete} index={idx} />
            </Col>
          ))}
        </Row>
      )}
    </motion.div>
  );
};

export default DashboardPage;
