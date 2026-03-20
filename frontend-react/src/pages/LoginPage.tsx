import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Typography, Alert } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { login } from '../api/auth';
import { useAuthStore } from '../store/authStore';

const { Title, Text, Paragraph } = Typography;

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const authLogin = useAuthStore((s) => s.login);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const onFinish = async (values: { username: string; password: string }) => {
    if (!values.username || !values.password) {
      setError('请输入用户名和密码');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      const resp = await login(values.username, values.password);
      authLogin(resp.token, resp.username);
      navigate('/');
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const staggerChildren = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: { staggerChildren: 0.1 },
    },
  };

  const fadeUp = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.4 } },
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
      style={{
        display: 'flex',
        flexDirection: isMobile ? 'column' : 'row',
        minHeight: '100vh',
      }}
    >
      {/* Left brand panel */}
      <div
        className="auth-brand-panel"
        style={{
          width: isMobile ? '100%' : '50%',
          height: isMobile ? 200 : '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          position: 'relative',
          flexShrink: 0,
        }}
      >
        <div className="auth-dots">
          <span /><span /><span /><span /><span />
        </div>
        <div style={{ position: 'relative', zIndex: 1, textAlign: 'center' }}>
          <div style={{ fontSize: isMobile ? 48 : 80, lineHeight: 1 }}>🦉</div>
          <Title
            level={isMobile ? 4 : 2}
            style={{ margin: '12px 0 4px', color: '#c8a44e' }}
          >
            Hedwig
          </Title>
          <Text type="secondary" style={{ fontSize: isMobile ? 13 : 15 }}>
            CGM · 血糖数据网关
          </Text>
        </div>
      </div>

      {/* Right form panel */}
      <div
        style={{
          width: isMobile ? '100%' : '50%',
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: isMobile ? '24px 16px' : 40,
        }}
      >
        <motion.div
          className="glass"
          style={{
            width: '100%',
            maxWidth: 420,
            padding: isMobile ? 24 : 40,
            borderRadius: 16,
          }}
          variants={staggerChildren}
          initial="hidden"
          animate="visible"
        >
          <motion.div variants={fadeUp} style={{ textAlign: 'center', marginBottom: 24 }}>
            <Title level={3} style={{ marginBottom: 4 }}>欢迎回来</Title>
            <Paragraph type="secondary" style={{ marginBottom: 0 }}>
              登录 Hedwig 管理系统
            </Paragraph>
          </motion.div>

          {error && (
            <Alert
              type="error"
              message={error}
              showIcon
              style={{ marginBottom: 16 }}
              closable
              onClose={() => setError(null)}
            />
          )}

          <Form layout="vertical" onFinish={onFinish} autoComplete="on">
            <motion.div variants={fadeUp}>
              <Form.Item
                label="用户名"
                name="username"
                rules={[{ required: true, message: '请输入用户名' }]}
              >
                <Input
                  prefix={<UserOutlined />}
                  placeholder="请输入用户名"
                  autoComplete="username"
                  size="large"
                />
              </Form.Item>
            </motion.div>

            <motion.div variants={fadeUp}>
              <Form.Item
                label="密码"
                name="password"
                rules={[{ required: true, message: '请输入密码' }]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="请输入密码"
                  autoComplete="current-password"
                  size="large"
                />
              </Form.Item>
            </motion.div>

            <motion.div variants={fadeUp}>
              <Form.Item>
                <Button type="primary" htmlType="submit" loading={loading} block size="large">
                  {loading ? '登录中...' : '登 录'}
                </Button>
              </Form.Item>
            </motion.div>
          </Form>

          <motion.div variants={fadeUp} style={{ textAlign: 'center' }}>
            <Text type="secondary">没有账号？</Text>{' '}
            <Link to="/register">立即注册</Link>
          </motion.div>
        </motion.div>
      </div>
    </motion.div>
  );
};

export default LoginPage;
